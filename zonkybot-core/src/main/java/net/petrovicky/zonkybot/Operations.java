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


import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;

import net.petrovicky.zonkybot.remote.Authorization;
import net.petrovicky.zonkybot.remote.Investment;
import net.petrovicky.zonkybot.remote.Loan;
import net.petrovicky.zonkybot.remote.Rating;
import net.petrovicky.zonkybot.remote.Ratings;
import net.petrovicky.zonkybot.remote.RiskPortfolio;
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

    public Operations(final String username, final String password, final InvestmentStrategy strategy, final boolean dryRun, final int dryRunBalance) {
        this.username = username;
        this.password = password;
        this.strategy = strategy;
        this.dryRun = dryRun;
        this.dryRunBalance = dryRunBalance;
    }

    private static Map<Rating, BigDecimal> calculateSharesPerRating(final Statistics stats,
                                                                    final Iterable<Investment> previousInvestments) {
        final Map<Rating, BigDecimal> amounts = new EnumMap<>(Rating.class);
        for (final RiskPortfolio risk : stats.getRiskPortfolio()) {
            final BigDecimal value = BigDecimal.valueOf(risk.getUnpaid());
            amounts.put(Rating.valueOf(risk.getRating()), value);
        }
        previousInvestments.forEach(previousInvestment -> {
            /*
             make sure the share reflects the investments made by ZonkyBot which have not yet been reflected in the API
              */
            final Rating r = previousInvestment.getLoan().getRating();
            final BigDecimal investment = BigDecimal.valueOf(previousInvestment.getInvestedAmount());
            amounts.put(r, amounts.get(r).add(investment));
        });
        final BigDecimal total = Operations.sum(amounts.values());
        final Map<Rating, BigDecimal> result = new EnumMap<>(Rating.class);
        amounts.forEach((rating, amount) -> result.put(rating, amount.divide(total, 4, RoundingMode.HALF_EVEN)));
        return Collections.unmodifiableMap(result);
    }

    private static BigDecimal sum(final Iterable<BigDecimal> vals) {
        BigDecimal result = BigDecimal.ZERO;
        for (final BigDecimal val : vals) {
            result = result.add(val);
        }
        return result;
    }

    private static boolean isLoanPresent(final Loan loan, final Iterable<Investment> previousInvestments) {
        for (final Investment i : previousInvestments) {
            if (loan.getId() == i.getLoan().getId()) {
                return true;
            }
        }
        return false;
    }

    private Optional<Investment> makeInvestment(final List<Investment> previousInvestments) {
        final Map<Rating, BigDecimal> shareOfRatings = Operations.calculateSharesPerRating(authenticatedClient.getStatistics(),
                previousInvestments);
        Operations.LOGGER.info("Current share of unpaid loans with a given rating is currently: {}.", shareOfRatings);
        final SortedMap<BigDecimal, Rating> mostWantedRatings = new TreeMap<>(Comparator.reverseOrder());
        for (final Map.Entry<Rating, BigDecimal> entry : shareOfRatings.entrySet()) {
            final Rating r = entry.getKey();
            final BigDecimal currentShare = entry.getValue();
            final BigDecimal maximumAllowedShare = strategy.getTargetShare(r);
            if (currentShare.compareTo(maximumAllowedShare) >= 0) { // we bought too many of this rating; ignore
                continue;
            }
            // sort the ratings by how much we miss them based on the strategy
            mostWantedRatings.put(maximumAllowedShare.subtract(currentShare), r);
        }
        Operations.LOGGER.info("According to the investment strategy, the portfolio is low on the following ratings: {}.",
                mostWantedRatings.values());
        for (final Rating r : mostWantedRatings.values()) { // try to invest in a given rating
            final Collection<Loan> loans = authenticatedClient.getLoans(Ratings.of(r), strategy.getMinimumInvestmentAmount(r));
            if (loans.size() == 0) {
                Operations.LOGGER.info("There are no loans of rating '{}' that match criteria defined by investment strategy.", r);
                continue;
            }
            // sort loans by their term
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
            if (strategy.prefersLongerTerms(r)) {
                Operations.LOGGER.info("According to the investment strategy, loans with rating '{}' will be evaluated starting from the longest terms.", r);
            } else {
                Operations.LOGGER.info("According to the investment strategy, loans with rating '{}' will be evaluated starting from the shortest terms.", r);
                Collections.reverse(sortedLoans);
            }
            // start investing
            for (final Loan l : sortedLoans) {
                if (Operations.isLoanPresent(l, previousInvestments)) { // should only happen in dry run
                    Operations.LOGGER.info("ZonkyBot already invested in loan '{}', skipping.", l);
                    continue;
                } else if (!strategy.isAcceptable(l)) {
                    Operations.LOGGER.info("According to the investment strategy, loan '{}' is not acceptable.", l);
                    continue;
                }
                final int toInvest = (int) Math.min(strategy.getMaximumInvestmentAmount(r), l.getRemainingInvestment());
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
        client.register(new Operations.AuthorizationFilter());
        final Authorization auth = client.target(Operations.ZONKY_URL).proxy(Authorization.class);
        final Token authorization = auth.login(username, password, "password", "SCOPE_APP_WEB");
        client.close();
        // provide authenticated clients
        authenticatedClient = clientBuilder.build().register(new Operations.AuthenticatedFilter(authorization))
                .target(Operations.ZONKY_URL).proxy(ZonkyAPI.class);
    }

    public void logout() {
        authenticatedClient.logout();
    }

    private static class AuthorizationFilter implements ClientRequestFilter {

        private static final Logger LOGGER = LoggerFactory.getLogger(Operations.AuthorizationFilter.class);

        @Override
        public void filter(final ClientRequestContext clientRequestContext) throws IOException {
            Operations.AuthorizationFilter.LOGGER.debug("Will '{}' to '{}'.", clientRequestContext.getMethod(), clientRequestContext.getUri());
            final String authCode = Base64.getEncoder().encodeToString("web:web".getBytes());
            clientRequestContext.getHeaders().add("Authorization", "Basic " + authCode);
        }
    }

    private static class AuthenticatedFilter implements ClientRequestFilter {

        private static final Logger LOGGER = LoggerFactory.getLogger(Operations.AuthenticatedFilter.class);

        private final Token authorization;

        public AuthenticatedFilter(final Token token) {
            authorization = token;
            Operations.AuthenticatedFilter.LOGGER.debug("Using Zonky access token '{}'.", authorization.getAccessToken());
        }

        @Override
        public void filter(final ClientRequestContext clientRequestContext) throws IOException {
            Operations.AuthenticatedFilter.LOGGER.debug("Will '{}' to '{}'.", clientRequestContext.getMethod(), clientRequestContext.getUri());
            clientRequestContext.getHeaders().add("Authorization", "Bearer " + authorization.getAccessToken());
        }
    }

}
