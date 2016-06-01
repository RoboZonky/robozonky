/*
 * Copyright 2016 Lukáš Petrovický
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.triceo.robozonky;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import com.github.triceo.robozonky.exceptions.LoginFailedException;
import com.github.triceo.robozonky.exceptions.LogoutFailedException;
import com.github.triceo.robozonky.remote.Investment;
import com.github.triceo.robozonky.remote.InvestmentStatus;
import com.github.triceo.robozonky.remote.InvestmentStatuses;
import com.github.triceo.robozonky.remote.Loan;
import com.github.triceo.robozonky.remote.Rating;
import com.github.triceo.robozonky.remote.Ratings;
import com.github.triceo.robozonky.remote.RiskPortfolio;
import com.github.triceo.robozonky.remote.Statistics;
import com.github.triceo.robozonky.remote.Token;
import com.github.triceo.robozonky.remote.ZonkyAPI;
import com.github.triceo.robozonky.strategy.InvestmentStrategy;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.plugins.providers.RegisterBuiltin;
import org.jboss.resteasy.plugins.providers.jackson.ResteasyJackson2Provider;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Operations {

    public static final int MINIMAL_INVESTMENT_ALLOWED = 200;
    protected static final String ZONKY_VERSION_UNDETECTED = "UNDETECTED";
    protected static final String ZONKY_VERSION_UNKNOWN = "UNKNOWN";

    private static final int CONNECTION_POOL_SIZE = 2;
    static final String ZONKY_URL = "https://api.zonky.cz";

    private static final Logger LOGGER = LoggerFactory.getLogger(Operations.class);

    /**
     * Get the share of 'payments due for each rating' on the overall portfolio.
     * @param stats
     * @param investments Loans which have already been invested in by the current user.
     * @return Map where each rating is the key and value is the share of that rating among overall due payments.
     */
    static Map<Rating, BigDecimal> calculateSharesPerRating(final Statistics stats,
                                                            final Collection<Investment> investments) {
        final Map<Rating, BigDecimal> amounts = stats.getRiskPortfolio().stream().collect(
                Collectors.toMap(RiskPortfolio::getRating, risk -> BigDecimal.valueOf(risk.getUnpaid()))
        );
        // make sure ratings are present even when there's 0 invested in them
        Arrays.stream(Rating.values()).filter(r -> !amounts.containsKey(r)).forEach(r -> amounts.put(r, BigDecimal.ZERO));
        // make sure the share reflects investments made by ZonkyBot which have not yet been reflected in the API
        investments.forEach(previousInvestment -> {
            final Rating r = previousInvestment.getRating();
            final BigDecimal investment = BigDecimal.valueOf(previousInvestment.getAmount());
            amounts.put(r, amounts.get(r).add(investment));
        });
        final BigDecimal total = Util.sum(amounts.values());
        if (total.compareTo(BigDecimal.ZERO) == 0) { // no ratings have any investments
            return Collections.unmodifiableMap(amounts);
        }
        final Map<Rating, BigDecimal> result = new EnumMap<>(Rating.class);
        amounts.forEach((rating, amount) -> result.put(rating, amount.divide(total, 4, RoundingMode.HALF_EVEN)));
        return Collections.unmodifiableMap(result);
    }

    /**
     * Put money into an already selected loan.
     * @param oc Context for the current session.
     * @param l Loan to invest into.
     * @param investmentsInSession Previous loans invested in this session.
     * @param balance Latest known Zonky account balance.
     * @return Present only if Zonky API confirmed money was invested or if dry run.
     */
    static Optional<Investment> actuallyInvest(final OperationsContext oc, final Loan l,
                                               final List<Investment> investmentsInSession, final BigDecimal balance) {
        if (Util.isLoanPresent(l, investmentsInSession)) {
            Operations.LOGGER.info("RoboZonky already invested in loan '{}', skipping. May only happen in dry runs.", l);
            return Optional.empty();
        } else if (!oc.getStrategy().isAcceptable(l)) {
            Operations.LOGGER.info("According to the investment strategy, loan '{}' is not acceptable.", l);
            return Optional.empty();
        }
        // figure out how much to invest
        final int resultingInvestment = oc.getStrategy().recommendInvestmentAmount(l, balance);
        Operations.LOGGER.debug("Strategy recommended to invest {} CZK on balance of {} CZK.", resultingInvestment,
                balance.intValue());
        if (resultingInvestment < Operations.MINIMAL_INVESTMENT_ALLOWED) {
            Operations.LOGGER.info("Not investing into loan '{}', since investment ({} CZK) less than bare minimum.",
                    l, resultingInvestment);
            return Optional.empty();
        } else if (resultingInvestment > balance.intValue()) {
            Operations.LOGGER.info("Not investing into loan '{}', since investment ({} CZK) more than {} CZK balance.",
                    l, resultingInvestment, balance);
            return Optional.empty();
        }
        return Operations.invest(oc, l, resultingInvestment);
    }

    /**
     * Choose from available loans of a given rating one loan to invest money into.
     * @param oc Context for the current session.
     * @param r Rating in question.
     * @param loansFuture Loans carrying that rating.
     * @param investmentsInSession Previous investments made in this session.
     * @param balance Latest known Zonky account balance.
     * @return Present only if Zonky API confirmed money was invested or if dry run.
     */
    static Optional<Investment> identifyLoanToInvest(final OperationsContext oc, final Rating r,
                                                     final Future<Collection<Loan>> loansFuture,
                                                     final List<Investment> investmentsInSession,
                                                     final BigDecimal balance) {
        final Collection<Loan> loans;
        try {
            loans = loansFuture.get();
        } catch (final Exception e) {
            Operations.LOGGER.warn("Could not list loans with rating '{}'. Can not invest in that rating.", r, e);
            return Optional.empty();
        }
        if (loans.size() == 0) {
            Operations.LOGGER.info("There are no loans of rating '{}' matching the investment strategy.", r);
            return Optional.empty();
        }
        // sort loans by their term and start investing
        final boolean prefersLongTerm = oc.getStrategy().prefersLongerTerms(r);
        Operations.LOGGER.info("Investment strategy for rating '{}' prefers {} term loans.", r,
                prefersLongTerm ? "longer" : "shorter");
        for (final Loan l : Util.sortLoansByTerm(loans, prefersLongTerm)) {
            final Optional<Investment> investment = Operations.actuallyInvest(oc, l, investmentsInSession, balance);
            if (investment.isPresent()) {
                return investment;
            }
        }
        return Optional.empty();
    }

    /**
     *
     * @param oc Context for the current session.
     * @param currentShare Current share of investments in a given rating.
     * @return Ratings in the order of decreasing demand. Over-invested ratings not present.
     */
    static List<Rating> rankRatingsByDemand(final OperationsContext oc, final Map<Rating, BigDecimal> currentShare) {
        final MultiValuedMap<BigDecimal, Rating> mostWantedRatings = new HashSetValuedHashMap<>();
        // put the ratings into buckets based on how much we're missing them
        currentShare.forEach((r, currentRatingShare) -> {
            final BigDecimal maximumAllowedShare = oc.getStrategy().getTargetShare(r);
            if (currentRatingShare.compareTo(maximumAllowedShare) >= 0) { // we bought too many of this rating; ignore
                return;
            }
            mostWantedRatings.put(maximumAllowedShare.subtract(currentRatingShare), r);
        });
        // and now output ratings in an order, bigger first
        final List<Rating> result = new ArrayList<>(currentShare.size());
        while (!mostWantedRatings.isEmpty()) {
            BigDecimal biggestRanking = BigDecimal.ZERO;
            for (final BigDecimal tested: mostWantedRatings.keySet()) {
                if (tested.compareTo(biggestRanking) > 0) {
                    biggestRanking = tested;
                }
            }
            result.addAll(mostWantedRatings.remove(biggestRanking));
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Choose from available loans the most important loan to invest money into.
     * @param oc Context for the current session.
     * @param investmentsInSession Previous investments made in this session.
     * @param balance Latest known Zonky account balance.
     * @return Present only if Zonky API confirmed money was invested or if dry run.
     */
    static Optional<Investment> identifyLoanToInvest(final OperationsContext oc,
                                                     final List<Investment> investmentsInSession,
                                                     final BigDecimal balance) {
        final ZonkyAPI api = oc.getAPI();
        final Collection<Investment> investments = Util.mergeInvestments(
                api.getInvestments(InvestmentStatuses.of(InvestmentStatus.SIGNED)), investmentsInSession);
        final Map<Rating, BigDecimal> shareOfRatings = Operations.calculateSharesPerRating(api.getStatistics(),
                investments);
        final List<Rating> mostWantedRatings = Operations.rankRatingsByDemand(oc, shareOfRatings);
        Operations.LOGGER.debug("Current share of unpaid loans with a given rating is currently: {}.", shareOfRatings);
        Operations.LOGGER.info("According to the investment strategy, the portfolio is low on following ratings: {}.",
                mostWantedRatings);
        final Map<Rating, Future<Collection<Loan>>> availableLoans = new EnumMap<>(Rating.class);
        mostWantedRatings.forEach(r -> { // submit HTTP requests ahead of time
            final Callable<Collection<Loan>> future
                    = () -> api.getLoans(Ratings.of(r), Operations.MINIMAL_INVESTMENT_ALLOWED);
            availableLoans.put(r, oc.getBackgroundExecutor().submit(future));
        });
        for (final Rating r : mostWantedRatings) { // try to invest in a given rating
            final Optional<Investment> investment =
                    Operations.identifyLoanToInvest(oc, r, availableLoans.get(r), investmentsInSession, balance);
            if (investment.isPresent()) {
                return investment;
            }
        }
        return Optional.empty();
    }

    static BigDecimal getAvailableBalance(final OperationsContext oc,
                                                    final Collection<Investment> investmentsInSession) {
        final boolean isDryRun = oc.isDryRun();
        final int dryRunInitialBalance = oc.getDryRunInitialBalance();
        BigDecimal balance = (isDryRun && dryRunInitialBalance >= 0) ?
                BigDecimal.valueOf(dryRunInitialBalance) : oc.getAPI().getWallet().getAvailableBalance();
        if (isDryRun) {
            for (final Investment i : investmentsInSession) {
                balance = balance.subtract(BigDecimal.valueOf(i.getAmount()));
            }
        }
        return balance;
    }

    public static Collection<Investment> invest(final OperationsContext oc) {
        final int minimumInvestmentAmount = Operations.MINIMAL_INVESTMENT_ALLOWED;
        final List<Investment> investmentsMade = new ArrayList<>();
        BigDecimal availableBalance = Operations.getAvailableBalance(oc, investmentsMade);
        Operations.LOGGER.info("RoboZonky starting account balance is {} CZK.", availableBalance);
        while (availableBalance.compareTo(BigDecimal.valueOf(minimumInvestmentAmount)) >= 0) {
            final Optional<Investment> investment = Operations.identifyLoanToInvest(oc, investmentsMade, availableBalance);
            if (investment.isPresent()) {
                investmentsMade.add(investment.get());
                availableBalance = Operations.getAvailableBalance(oc, investmentsMade);
                if (oc.isDryRun()) {
                    Operations.LOGGER.info("Simulated new account balance is {} CZK.", availableBalance);
                } else {
                    Operations.LOGGER.info("New account balance is {} CZK.", availableBalance);
                }
            } else {
                break;
            }
        }
        return Collections.unmodifiableList(investmentsMade);
    }

    private static Optional<Investment> invest(final OperationsContext oc, final Investment i) {
        if (oc.isDryRun()) {
            Operations.LOGGER.info("This is a dry run. Not investing {} CZK into loan {}.", i.getAmount(),
                    i.getLoanId());
            return Optional.of(i);
        } else {
            Operations.LOGGER.info("Attempting to invest {} CZK into loan {}.", i.getAmount(), i.getLoanId());
            try {
                oc.getAPI().invest(i);
                Operations.LOGGER.warn("Investment operation succeeded.");
                return Optional.of(i);
            } catch (final Exception ex) {
                Operations.LOGGER.warn("Investment operation failed.", ex);
                return Optional.empty();
            }
        }
    }

    private static Optional<Investment> invest(final OperationsContext oc, final Loan loan, final int loanAmount) {
        final Investment investment = new Investment(loan, loanAmount);
        return Operations.invest(oc, investment);
    }

    public static Optional<Investment> invest(final OperationsContext oc, final int loanId, final int loanAmount) {
        final Investment investment = new Investment(loanId, loanAmount);
        return Operations.invest(oc, investment);
    }

    // FIXME what if login fails?
    public static OperationsContext login(final Authentication authentication, final boolean dryRun,
                                          final int dryRunInitialBalance, final InvestmentStrategy strategy)
            throws LoginFailedException {
        try {
            // register Jackson
            final ResteasyProviderFactory instance = ResteasyProviderFactory.getInstance();
            RegisterBuiltin.register(instance);
            instance.registerProvider(ResteasyJackson2Provider.class);
            // authenticate
            final ResteasyClientBuilder clientBuilder = new ResteasyClientBuilder();
            clientBuilder.providerFactory(instance);
            final Token authorization = authentication.authenticate(clientBuilder);
            // provide authenticated clients
            clientBuilder.connectionPoolSize(Operations.CONNECTION_POOL_SIZE);
            final ZonkyAPI api = clientBuilder.build().register(new AuthenticatedFilter(authorization))
                    .target(Operations.ZONKY_URL).proxy(ZonkyAPI.class);
            return new OperationsContext(api, strategy, dryRun, dryRunInitialBalance, Operations.CONNECTION_POOL_SIZE);
        } catch (final RuntimeException ex) {
            throw new LoginFailedException("Error while instantiating Zonky API proxy.", ex);
        }
    }

    public static OperationsContext login(final Authentication authentication, final boolean dryRun,
                                          final int dryRunInitialBalance) throws LoginFailedException {
        return Operations.login(authentication, dryRun, dryRunInitialBalance, null);
    }

    public static void logout(final OperationsContext oc) throws LogoutFailedException {
        try {
            oc.getAPI().logout();
        } catch (final RuntimeException ex) {
            throw new LogoutFailedException("Error while logging out Zonky.", ex);
        }
        oc.dispose();
        Operations.LOGGER.info("Logged out of Zonky.");
    }

}
