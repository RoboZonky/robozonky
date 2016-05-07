/*
 * Copyright 2016 Lukáš Petrovický
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.petrovicky.zonkybot;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import net.petrovicky.zonkybot.remote.Authorization;
import net.petrovicky.zonkybot.remote.Investment;
import net.petrovicky.zonkybot.remote.Loan;
import net.petrovicky.zonkybot.remote.Rating;
import net.petrovicky.zonkybot.remote.Ratings;
import net.petrovicky.zonkybot.remote.Statistics;
import net.petrovicky.zonkybot.remote.Token;
import net.petrovicky.zonkybot.remote.ZonkyAPI;
import net.petrovicky.zonkybot.strategy.InvestmentStrategy;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.plugins.providers.RegisterBuiltin;
import org.jboss.resteasy.plugins.providers.jackson.ResteasyJackson2Provider;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Operations {

    private static final String ZONKY_URL = "https://api.zonky.cz";
    private static final Logger LOGGER = LoggerFactory.getLogger(Operations.class);
    private final String username, password;
    private final InvestmentStrategy strategy;
    private ZonkyAPI authenticatedClient;
    private final boolean dryRun;
    private final int dryRunBalance;
    private ExecutorService backgroundThreadExecutor;

    public Operations(final String username, final String password, final InvestmentStrategy strategy, final boolean dryRun, final int dryRunBalance) {
        this.username = username;
        this.password = password;
        this.strategy = strategy;
        this.dryRun = dryRun;
        this.dryRunBalance = dryRunBalance;
    }

    private static Map<Rating, BigDecimal> calculateSharesPerRating(final Statistics stats,
                                                                    final Iterable<Investment> previousInvestments) {
        final Map<Rating, BigDecimal> amounts = stats.getRiskPortfolio().stream().collect(
                Collectors.toMap(risk -> Rating.valueOf(risk.getRating()), risk -> BigDecimal.valueOf(risk.getUnpaid()))
        );
        previousInvestments.forEach(previousInvestment -> {
            // make sure the share reflects investments made by ZonkyBot which have not yet been reflected in the API
            final Rating r = previousInvestment.getLoan().getRating();
            final BigDecimal investment = BigDecimal.valueOf(previousInvestment.getInvestedAmount());
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

    private static boolean isLoanPresent(final Loan loan, final Iterable<Investment> previousInvestments) {
        for (final Investment i : previousInvestments) {
            if (loan.getId() == i.getLoan().getId()) {
                return true;
            }
        }
        return false;
    }

    private Optional<Investment> makeInvestment(final Loan l, final List<Investment> previousInvestments) {
        if (Operations.isLoanPresent(l, previousInvestments)) { // should only happen in dry run
            Operations.LOGGER.info("ZonkyBot already invested in loan '{}', skipping.", l);
            return Optional.empty();
        } else if (!strategy.isAcceptable(l)) {
            Operations.LOGGER.info("According to the investment strategy, loan '{}' is not acceptable.", l);
            return Optional.empty();
        }
        final int toInvest = (int) Math.min(strategy.getMaximumInvestmentAmount(l.getRating()),
                l.getRemainingInvestment());
        final Optional<Investment> optional = Optional.of(new Investment(l, toInvest));
        if (dryRun) {
            Operations.LOGGER.info("This is a dry run. Not investing {} CZK into loan '{}'.", toInvest, l);
            return optional;
        } else {
            Operations.LOGGER.info("Attempting to invest {} CZK into loan '{}'.", toInvest, l);
            try {
                authenticatedClient.invest(optional.get());
                Operations.LOGGER.warn("Investment operating succeeded.");
                return optional;
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

    private Optional<Investment> makeInvestment(Rating r, final Future<Collection<Loan>> loansFuture,
                                                final List<Investment> previousInvestments) {
        final Collection<Loan> loans;
        try {
            loans = loansFuture.get();
        } catch (Exception e) {
            Operations.LOGGER.warn("Could not list loans with rating '{}'. Can not invest in that rating.", r);
            return Optional.empty();
        }
        if (loans.size() == 0) {
            Operations.LOGGER.info("There are no loans of rating '{}' matching the investment strategy.", r);
            return Optional.empty();
        }
        // sort loans by their term
        final List<Loan> sortedLoans = Operations.sortLoansByTerm(loans);
        if (strategy.prefersLongerTerms(r)) {
            Operations.LOGGER.info("According to the investment strategy, loans with rating '{}' will be evaluated starting from the longest terms.", r);
        } else {
            Operations.LOGGER.info("According to the investment strategy, loans with rating '{}' will be evaluated starting from the shortest terms.", r);
            Collections.reverse(sortedLoans);
        }
        // start investing
        for (final Loan l : sortedLoans) {
            final Optional<Investment> investment = this.makeInvestment(l, previousInvestments);
            if (investment.isPresent()) {
                return investment;
            }
        }
        return Optional.empty();
    }

    private SortedMap<BigDecimal, Rating> rankRankinsByDemand(final Map<Rating, BigDecimal> shareOfRatings) {
        final SortedMap<BigDecimal, Rating> mostWantedRatings = new TreeMap<>(Comparator.reverseOrder());
        shareOfRatings.forEach((r, currentShare) -> {
            final BigDecimal maximumAllowedShare = strategy.getTargetShare(r);
            if (currentShare.compareTo(maximumAllowedShare) >= 0) { // we bought too many of this rating; ignore
                return;
            }
            // sort the ratings by how much we miss them based on the strategy
            mostWantedRatings.put(maximumAllowedShare.subtract(currentShare), r);
        });
        return Collections.unmodifiableSortedMap(mostWantedRatings);
    }

    private Optional<Investment> makeInvestment(final List<Investment> previousInvestments) {
        final Map<Rating, BigDecimal> shareOfRatings =
                Operations.calculateSharesPerRating(authenticatedClient.getStatistics(), previousInvestments);
        final SortedMap<BigDecimal, Rating> mostWantedRatings = this.rankRankinsByDemand(shareOfRatings);
        Operations.LOGGER.debug("Current share of unpaid loans with a given rating is currently: {}.", shareOfRatings);
        Operations.LOGGER.info("According to the investment strategy, the portfolio is low on the following ratings: {}.",
                mostWantedRatings.values());
        final Map<Rating, Future<Collection<Loan>>> availableLoans = new EnumMap<>(Rating.class);
        // submit HTTP requests ahead of time
        mostWantedRatings.forEach((s, r) -> {
            final Callable<Collection<Loan>> future =
                    () -> authenticatedClient.getLoans(Ratings.of(r), strategy.getMinimumInvestmentAmount(r));
            availableLoans.put(r, this.backgroundThreadExecutor.submit(future));
        });
        for (final Rating r : mostWantedRatings.values()) { // try to invest in a given rating
            final Optional<Investment> investment = makeInvestment(r, availableLoans.get(r), previousInvestments);
            if (investment.isPresent()) {
                return investment;
            }
        }
        return Optional.empty();
    }

    private BigDecimal getAvailableBalance(final Iterable<Investment> previousInvestments) {
        BigDecimal balance = (dryRun && dryRunBalance >= 0) ? BigDecimal.valueOf(dryRunBalance) :
                authenticatedClient.getWallet().getAvailableBalance();
        if (dryRun) {
            for (final Investment i : previousInvestments) {
                balance = balance.subtract(BigDecimal.valueOf(i.getInvestedAmount()));
            }
        }
        return balance;
    }

    public List<Investment> invest() {
        final int minimumInvestmentAmount = strategy.getMinimumInvestmentAmount();
        final List<Investment> investmentsMade = new ArrayList<>();
        BigDecimal availableBalance = getAvailableBalance(investmentsMade);
        Operations.LOGGER.info("ZonkyBot starting account balance is {} CZK.", availableBalance);
        while (availableBalance.compareTo(BigDecimal.valueOf(minimumInvestmentAmount)) >= 0) {
            final Optional<Investment> investment = makeInvestment(investmentsMade);
            if (investment.isPresent()) {
                investmentsMade.add(investment.get());
                availableBalance = getAvailableBalance(investmentsMade);
                Operations.LOGGER.info("New account balance is {} CZK.", availableBalance);
            } else {
                break;
            }
        }
        if (availableBalance.compareTo(BigDecimal.valueOf(minimumInvestmentAmount)) < 0) {
            Operations.LOGGER.info("Account balance ({} CZK) less than investment minimum ({} CZK) defined by strategy.",
                    availableBalance, minimumInvestmentAmount);
        }
        return investmentsMade;
    }

    public void login() {
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
        // provide authenticated clients
        authenticatedClient = clientBuilder.build().register(new AuthenticatedFilter(authorization))
                .target(Operations.ZONKY_URL).proxy(ZonkyAPI.class);
        this.backgroundThreadExecutor = Executors.newSingleThreadExecutor();
    }

    public void logout() {
        authenticatedClient.logout();
        this.backgroundThreadExecutor.shutdownNow();
    }

}
