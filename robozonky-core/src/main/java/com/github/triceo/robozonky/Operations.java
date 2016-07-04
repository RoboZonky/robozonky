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
import java.util.stream.Collectors;

import com.github.triceo.robozonky.authentication.Authentication;
import com.github.triceo.robozonky.authentication.Authenticator;
import com.github.triceo.robozonky.remote.BlockedAmount;
import com.github.triceo.robozonky.remote.Investment;
import com.github.triceo.robozonky.remote.InvestmentStatus;
import com.github.triceo.robozonky.remote.InvestmentStatuses;
import com.github.triceo.robozonky.remote.Loan;
import com.github.triceo.robozonky.remote.Rating;
import com.github.triceo.robozonky.remote.RiskPortfolio;
import com.github.triceo.robozonky.remote.Statistics;
import com.github.triceo.robozonky.remote.ZonkyApi;
import com.github.triceo.robozonky.remote.ZonkyApiToken;
import com.github.triceo.robozonky.strategy.InvestmentStrategy;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.plugins.providers.RegisterBuiltin;
import org.jboss.resteasy.plugins.providers.jackson.ResteasyJackson2Provider;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Operations {

    /**
     * Decouples @{@link ZonkyApiToken} from {@link OperationsContext}, since we want the former to be short-lived.
     */
    public static class LoginResult {

        private final OperationsContext operationsContext;
        private final ZonkyApiToken zonkyApiToken;

        private LoginResult(final OperationsContext oc, final ZonkyApiToken token) {
            this.operationsContext = oc;
            this.zonkyApiToken = token;
        }

        public OperationsContext getOperationsContext() {
            return this.operationsContext;
        }

        public ZonkyApiToken getZonkyApiToken() {
            return this.zonkyApiToken;
        }

    }

    private static final Logger LOGGER = LoggerFactory.getLogger(Operations.class);

    private static final ResteasyProviderFactory RESTEASY;
    static {
        Operations.LOGGER.trace("Initializing RESTEasy.");
        RESTEASY = ResteasyProviderFactory.getInstance();
        RegisterBuiltin.register(Operations.RESTEASY);
        RESTEASY.registerProvider(ResteasyJackson2Provider.class);
        Operations.LOGGER.trace("RESTEasy initialized.");
    }

    public static final int MINIMAL_INVESTMENT_ALLOWED = 200;

    private static final String ZONKY_URL = "https://api.zonky.cz";
    private static final String ZOTIFY_URL = "http://zotify.cz";

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
        Arrays.stream(Rating.values()).filter(r -> !amounts.containsKey(r))
                .forEach(r -> amounts.put(r, BigDecimal.ZERO));
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
     * Choose from available loans the most important loan to invest money into.
     * @param oc Context for the current session.
     * @param stats Statistics retrieved from Zonky.
     * @param investments Previous investments for which to ignore loans.
     * @param balance Latest known Zonky account balance.
     * @return Present only if Zonky API confirmed money was invested or if dry run.
     */
    static Optional<Investment> identifyLoanToInvest(final OperationsContext oc, final Statistics stats,
                                                     final Collection<Investment> investments,
                                                     final BigDecimal balance) {
        // calculate share of particular ratings on the overall investment pie
        final Map<Rating, BigDecimal> shareOfRatings = Operations.calculateSharesPerRating(stats, investments);
        Operations.LOGGER.debug("Current share of unpaid loans with a given rating is: {}.", shareOfRatings);
        // find and sort acceptable loans
        final List<Loan> allLoansFromApi = oc.getZotifyApi().getLoans();
        final List<Loan> afterInvestmentsExcluded = allLoansFromApi.stream()
                .filter(l -> !Util.isLoanPresent(l, investments)).collect(Collectors.toList());
        final List<Loan> loans = oc.getStrategy().getMatchingLoans(afterInvestmentsExcluded, shareOfRatings, balance);
        if (loans == null || loans.size() == 0) {
            Operations.LOGGER.info("There are no loans matching the investment strategy.");
            return Optional.empty();
        } else {
            Operations.LOGGER.debug("Investment strategy accepted the following loans: {}", loans);
        }
        // and try investing until one loan succeeds
        for (final Loan l : loans) {
            final int invest = oc.getStrategy().recommendInvestmentAmount(l, shareOfRatings, balance);
            Operations.LOGGER.debug("Strategy recommended to invest {} CZK on balance of {} CZK.", invest, balance);
            final Optional<Investment> investment = Operations.actuallyInvest(oc, l, invest, balance);
            if (investment.isPresent()) {
                return investment;
            }
        }
        return Optional.empty();
    }

    static Optional<Investment> actuallyInvest(final OperationsContext oc, final Loan l, final int investment,
                                               final BigDecimal balance) {
        if (investment < Operations.MINIMAL_INVESTMENT_ALLOWED) {
            Operations.LOGGER.info("Not investing into loan '{}', since investment ({} CZK) less than bare minimum.",
                    l, investment);
            return Optional.empty();
        } else if (investment > balance.intValue()) {
            Operations.LOGGER.info("Not investing into loan '{}', {} CZK to invest is more than {} CZK balance.",
                    l, investment, balance);
            return Optional.empty();
        }
        return Operations.invest(oc, l, investment);
    }

    static BigDecimal getAvailableBalance(final OperationsContext oc) {
        final boolean isDryRun = oc.isDryRun();
        final BigDecimal dryRunInitialBalance = oc.getDryRunInitialBalance();
        return (isDryRun && dryRunInitialBalance.compareTo(BigDecimal.ZERO) > 0) ?
                dryRunInitialBalance : oc.getZonkyApi().getWallet().getAvailableBalance();
    }

    private static Collection<Investment> retrieveInvestmentsRepresentedByBlockedAmounts(final OperationsContext oc) {
        final ZonkyApi api = oc.getZonkyApi();
        final List<BlockedAmount> amounts = api.getBlockedAmounts();
        final List<Investment> investments = new ArrayList<>(amounts.size());
        for (final BlockedAmount blocked: amounts) {
            final Loan l = api.getLoan(blocked.getLoanId());
            final Investment i = new Investment(l, blocked.getAmount());
            investments.add(i);
            Operations.LOGGER.debug("{} CZK is being blocked by loan {}.", blocked.getAmount(), blocked.getLoanId());
        }
        return investments;
    }

    public static Collection<Investment> invest(final OperationsContext oc) {
        // make sure we have enough money to invest
        final int minimumInvestmentAmount = Operations.MINIMAL_INVESTMENT_ALLOWED;
        BigDecimal availableBalance = Operations.getAvailableBalance(oc);
        Operations.LOGGER.info("RoboZonky starting account balance is {} CZK.", availableBalance);
        if (availableBalance.compareTo(BigDecimal.valueOf(minimumInvestmentAmount)) < 0) {
            return Collections.emptyList(); // no need to do anything else
        }
        // retrieve a list of loans that the user already put money into
        final ZonkyApi api = oc.getZonkyApi();
        Collection<Investment> investments = Util.mergeInvestments(
                api.getInvestments(InvestmentStatuses.of(InvestmentStatus.SIGNED)),
                Operations.retrieveInvestmentsRepresentedByBlockedAmounts(oc));
        Operations.LOGGER.debug("The following loans are coming from the API as already invested into: {}",
                investments);
        final Statistics stats = api.getStatistics();
        // and start investing
        final Collection<Investment> investmentsMade = new ArrayList<>();
        do {
            final Optional<Investment> investment =
                    Operations.identifyLoanToInvest(oc, stats, investments, availableBalance);
            if (!investment.isPresent()) {
                break;
            }
            final Investment i = investment.get();
            investmentsMade.add(i);
            investments = Util.mergeInvestments(investments, Collections.singletonList(i));
            final BigDecimal amount = BigDecimal.valueOf(i.getAmount());
            availableBalance = availableBalance.subtract(amount);
            Operations.LOGGER.info("New account balance {} {} CZK.", oc.isDryRun() ? "would have been" : "is",
                    availableBalance);
        } while (availableBalance.compareTo(BigDecimal.valueOf(minimumInvestmentAmount)) >= 0);
        return Collections.unmodifiableCollection(investmentsMade);
    }

    private static Optional<Investment> invest(final OperationsContext oc, final Investment i) {
        if (oc.isDryRun()) {
            Operations.LOGGER.info("RoboZonky would have now invested {} CZK into loan {}, but this is a dry run.",
                    i.getAmount(), i.getLoanId());
            return Optional.of(i);
        } else {
            try {
                oc.getZonkyApi().invest(i);
                Operations.LOGGER.info("Invested {} CZK into loan {}.", i.getAmount(), i.getLoanId());
                return Optional.of(i);
            } catch (final Exception ex) {
                Operations.LOGGER.warn("Failed investing {} CZK into loan {}.", i.getAmount(), i.getLoanId());
                return Optional.empty();
            }
        }
    }

    private static Optional<Investment> invest(final OperationsContext oc, final Loan loan, final int loanAmount) {
        final Investment investment = new Investment(loan, loanAmount);
        return Operations.invest(oc, investment);
    }

    public static Optional<Investment> invest(final OperationsContext oc, final int loanId, final int loanAmount) {
        return Operations.invest(oc, oc.getZonkyApi().getLoan(loanId), loanAmount);
    }

    public static Optional<Operations.LoginResult> login(final Authenticator authenticationMethod, final boolean dryRun,
                                                         final int dryRunInitialBalance,
                                                         final InvestmentStrategy strategy) {
        try {
            Operations.LOGGER.trace("Preparing for login.");
            // authenticate
            final ResteasyClientBuilder clientBuilder = new ResteasyClientBuilder();
            clientBuilder.providerFactory(Operations.RESTEASY);
            Operations.LOGGER.trace("Login starting.");
            final Authentication auth = authenticationMethod.authenticate(Operations.ZONKY_URL, Operations.ZOTIFY_URL,
                            Util.getRoboZonkyVersion(), clientBuilder);
            final OperationsContext oc =
                    new OperationsContext(auth.getZonkyApi(), auth.getZotifyApi(), strategy, dryRun, dryRunInitialBalance);
            Operations.LOGGER.trace("Login complete.");
            return Optional.of(new Operations.LoginResult(oc, auth.getZonkyApiToken()));
        } catch (final RuntimeException ex) {
            Operations.LOGGER.error("Login failed.", ex);
            return Optional.empty();
        }
    }

    public static Optional<Operations.LoginResult> login(final Authenticator authenticationMethod, final boolean dryRun,
                                                         final int dryRunInitialBalance) {
        return Operations.login(authenticationMethod, dryRun, dryRunInitialBalance, null);
    }

    public static boolean logout(final OperationsContext oc) {
        try {
            Operations.LOGGER.trace("Logout started.");
            oc.getZonkyApi().logout();
            Operations.LOGGER.info("Logged out of Zonky.");
            return true;
        } catch (final RuntimeException ex) {
            Operations.LOGGER.warn("Logout failed.", ex);
            return false;
        }
    }

}
