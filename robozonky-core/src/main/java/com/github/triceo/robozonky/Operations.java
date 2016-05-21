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
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import com.github.triceo.robozonky.remote.Authorization;
import com.github.triceo.robozonky.remote.Investment;
import com.github.triceo.robozonky.remote.InvestmentStatus;
import com.github.triceo.robozonky.remote.InvestmentStatuses;
import com.github.triceo.robozonky.remote.Loan;
import com.github.triceo.robozonky.remote.Rating;
import com.github.triceo.robozonky.remote.Ratings;
import com.github.triceo.robozonky.remote.Statistics;
import com.github.triceo.robozonky.remote.Token;
import com.github.triceo.robozonky.remote.ZonkyAPI;
import com.github.triceo.robozonky.strategy.InvestmentStrategy;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
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
    private static final String ZONKY_URL = "https://api.zonky.cz";

    private static final Logger LOGGER = LoggerFactory.getLogger(Operations.class);

    /**
     * Get the share of 'payments due for each rating' on the overall portfolio.
     * @param stats
     * @param investmentsInZonky Loans which have already been funded but are not yet part of your portfolio.
     * @param investmentsInSession Loans invested made in this session of RoboZonky.
     * @return Map where each rating is the key and value is the share of that rating among overall due payments.
     */
    private static Map<Rating, BigDecimal> calculateSharesPerRating(final Statistics stats,
                                                                    final Collection<Investment> investmentsInZonky,
                                                                    final Collection<Investment> investmentsInSession) {
        final Map<Rating, BigDecimal> amounts = stats.getRiskPortfolio().stream().collect(
                Collectors.toMap(risk -> Rating.valueOf(risk.getRating()), risk -> BigDecimal.valueOf(risk.getUnpaid()))
        );
        final Collection<Investment> investments = Operations.mergeInvestments(investmentsInZonky, investmentsInSession);
        investments.forEach(previousInvestment -> {
            // make sure the share reflects investments made by ZonkyBot which have not yet been reflected in the API
            final Rating r = previousInvestment.getRating();
            final BigDecimal investment = BigDecimal.valueOf(previousInvestment.getAmount());
            amounts.put(r, amounts.get(r).add(investment));
        });
        final BigDecimal total = Operations.sum(amounts.values());
        final Map<Rating, BigDecimal> result = new EnumMap<>(Rating.class);
        amounts.forEach((rating, amount) -> result.put(rating, amount.divide(total, 4, RoundingMode.HALF_EVEN)));
        return Collections.unmodifiableMap(result);
    }

    private static BigDecimal sum(final Collection<BigDecimal> vals) {
        return vals.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Determine whether or not a given loan is present among existing investments.
     *
     * @param loan Loan in question.
     * @param investments Known investments.
     * @return True if present.
     */
    private static boolean isLoanPresent(final Loan loan, final Iterable<Investment> investments) {
        for (final Investment i : investments) {
            if (loan.getId() == i.getLoanId()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Put money into an already selected loan.
     * @param oc Context for the current session.
     * @param l Loan to invest into.
     * @param investmentsInSession Previous loans invested in this session.
     * @param balance Latest known Zonky account balance.
     * @return Present only if Zonky API confirmed money was invested or if dry run.
     */
    private static Optional<Investment> makeInvestment(final OperationsContext oc, final Loan l,
                                                       final List<Investment> investmentsInSession,
                                                       final BigDecimal balance) {
        if (Operations.isLoanPresent(l, investmentsInSession)) {
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
        }
        // and now actually invest
        final Investment investment = new Investment(l, resultingInvestment);
        if (oc.isDryRun()) {
            Operations.LOGGER.info("This is a dry run. Not investing {} CZK into loan '{}'.", resultingInvestment, l);
            return Optional.of(investment);
        } else {
            Operations.LOGGER.info("Attempting to invest {} CZK into loan '{}'.", resultingInvestment, l);
            try {
                oc.getAPI().invest(investment);
                Operations.LOGGER.warn("Investment operating succeeded.");
                return Optional.of(investment);
            } catch (final Exception ex) {
                Operations.LOGGER.warn("Investment operating failed.", ex);
                return Optional.empty();
            }
        }
    }

    private static List<Loan> sortLoansByTerm(final Collection<Loan> loans) {
        final List<Loan> sortedLoans = new ArrayList<>(loans.size());
        while (!loans.isEmpty()) {
            Loan longestTerm = null;
            for (final Loan l : loans) {
                if (longestTerm == null || longestTerm.getTermInMonths() < l.getTermInMonths()) {
                    longestTerm = l;
                }
            }
            loans.remove(longestTerm);
            sortedLoans.add(longestTerm);
        }
        return sortedLoans;
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
    private static Optional<Investment> makeInvestment(final OperationsContext oc, final Rating r,
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
        // sort loans by their term
        final List<Loan> sortedLoans = Operations.sortLoansByTerm(loans);
        if (oc.getStrategy().prefersLongerTerms(r)) {
            Operations.LOGGER.info("According to the investment strategy, loans with rating '{}' will be evaluated starting from the longest terms.", r);
        } else {
            Operations.LOGGER.info("According to the investment strategy, loans with rating '{}' will be evaluated starting from the shortest terms.", r);
            Collections.reverse(sortedLoans);
        }
        // start investing
        for (final Loan l : sortedLoans) {
            final Optional<Investment> investment = Operations.makeInvestment(oc, l, investmentsInSession, balance);
            if (investment.isPresent()) {
                return investment;
            }
        }
        return Optional.empty();
    }

    private static Collection<Investment> mergeInvestments(final Collection<Investment> online,
                                                           final Collection<Investment> session) {
        // merge investments made in this session with not-yet-active investments reported by Zonky
        final Collection<Investment> investments = new ArrayList<>(online);
        if (investments.size() == 0) {
            investments.addAll(session);
        } else {
            session.stream().forEach(investmentFromThisSession -> {
                for (final Investment investmentReportedByZonky : investments) {
                    if (investmentFromThisSession.getLoanId() == investmentReportedByZonky.getLoanId()) {
                        continue; // we already got this from the API
                    }
                    investments.add(investmentFromThisSession);
                }
            });
        }
        return investments;
    }

    private static SortedMap<BigDecimal, Rating> rankRankinsByDemand(final OperationsContext oc,
                                                                     final Map<Rating, BigDecimal> shareOfRatings) {
        final SortedMap<BigDecimal, Rating> mostWantedRatings = new TreeMap<>(Comparator.reverseOrder());
        shareOfRatings.forEach((r, currentShare) -> {
            final BigDecimal maximumAllowedShare = oc.getStrategy().getTargetShare(r);
            if (currentShare.compareTo(maximumAllowedShare) >= 0) { // we bought too many of this rating; ignore
                return;
            }
            // sort the ratings by how much we miss them based on the strategy
            mostWantedRatings.put(maximumAllowedShare.subtract(currentShare), r);
        });
        return Collections.unmodifiableSortedMap(mostWantedRatings);
    }

    /**
     * Choose from available loans the most important loan to invest money into.
     * @param oc Context for the current session.
     * @param investmentsInSession Previous investments made in this session.
     * @param balance Latest known Zonky account balance.
     * @return Present only if Zonky API confirmed money was invested or if dry run.
     */
    private static Optional<Investment> makeInvestment(final OperationsContext oc,
                                                       final List<Investment> investmentsInSession,
                                                       final BigDecimal balance) {
        final ZonkyAPI api = oc.getAPI();
        final Map<Rating, BigDecimal> shareOfRatings = Operations.calculateSharesPerRating(
                api.getStatistics(), api.getInvestments(InvestmentStatuses.of(InvestmentStatus.SIGNED)),
                investmentsInSession);
        final SortedMap<BigDecimal, Rating> mostWantedRatings = Operations.rankRankinsByDemand(oc, shareOfRatings);
        Operations.LOGGER.debug("Current share of unpaid loans with a given rating is currently: {}.", shareOfRatings);
        Operations.LOGGER.info("According to the investment strategy, the portfolio is low on following ratings: {}.",
                mostWantedRatings.values());
        final Map<Rating, Future<Collection<Loan>>> availableLoans = new EnumMap<>(Rating.class);
        mostWantedRatings.forEach((s, r) -> { // submit HTTP requests ahead of time
            // FIXME make sure that loans where I invested but which are not yet funded do not show up in here
            final Callable<Collection<Loan>> future = () -> api.getLoans(Ratings.of(r), Operations.MINIMAL_INVESTMENT_ALLOWED);
            availableLoans.put(r, oc.getBackgroundExecutor().submit(future));
        });
        for (final Rating r : mostWantedRatings.values()) { // try to invest in a given rating
            final Optional<Investment> investment =
                    Operations.makeInvestment(oc, r, availableLoans.get(r), investmentsInSession, balance);
            if (investment.isPresent()) {
                return investment;
            }
        }
        return Optional.empty();
    }

    public static String getRoboZonkyVersion() {
        try {
            final URLClassLoader cl = (URLClassLoader) Operations.class.getClassLoader();
            final URL url = cl.findResource("META-INF/maven/com.github.triceo.robozonky/robozonky-core/pom.properties");
            final Properties props = new Properties();
            props.load(url.openStream());
            return props.getProperty("version", Operations.ZONKY_VERSION_UNKNOWN);
        } catch (Exception ex) {
            return Operations.ZONKY_VERSION_UNDETECTED;
        }
    }

    protected static BigDecimal getAvailableBalance(final OperationsContext oc,
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
            final Optional<Investment> investment = Operations.makeInvestment(oc, investmentsMade, availableBalance);
            if (investment.isPresent()) {
                investmentsMade.add(investment.get());
                availableBalance = Operations.getAvailableBalance(oc, investmentsMade);
                Operations.LOGGER.info("New account balance is {} CZK.", availableBalance);
            } else {
                break;
            }
        }
        return Collections.unmodifiableList(investmentsMade);
    }

    public static OperationsContext login(final String username, final String password,
                                          final InvestmentStrategy strategy, final boolean dryRun,
                                          final int dryRunInitialBalance) {
        // register Jackson
        final ResteasyProviderFactory instance = ResteasyProviderFactory.getInstance();
        RegisterBuiltin.register(instance);
        instance.registerProvider(ResteasyJackson2Provider.class);
        // authenticate
        final ResteasyClientBuilder clientBuilder = new ResteasyClientBuilder();
        clientBuilder.providerFactory(instance);
        final ResteasyClient client = clientBuilder.build();
        client.register(new AuthorizationFilter());
        final Authorization auth = client.target(Operations.ZONKY_URL).proxy(Authorization.class);
        final Token authorization = auth.login(username, password, "password", "SCOPE_APP_WEB");
        client.close();
        Operations.LOGGER.info("Logged in with Zonky as user '{}'.", username);
        // provide authenticated clients
        clientBuilder.connectionPoolSize(Operations.CONNECTION_POOL_SIZE);
        final ZonkyAPI api = clientBuilder.build().register(new AuthenticatedFilter(authorization))
                .target(Operations.ZONKY_URL).proxy(ZonkyAPI.class);
        return new OperationsContext(api, strategy, dryRun, dryRunInitialBalance, Operations.CONNECTION_POOL_SIZE);
    }

    public static void logout(final OperationsContext oc) {
        oc.getAPI().logout();
        oc.dispose();
        Operations.LOGGER.info("Logged out of Zonky.");
    }

}
