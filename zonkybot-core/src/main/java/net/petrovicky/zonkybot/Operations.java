package net.petrovicky.zonkybot;


import java.io.IOException;
import java.math.BigDecimal;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;

import net.petrovicky.zonkybot.api.remote.Authorization;
import net.petrovicky.zonkybot.api.remote.Loan;
import net.petrovicky.zonkybot.api.remote.Rating;
import net.petrovicky.zonkybot.api.remote.Ratings;
import net.petrovicky.zonkybot.api.remote.RiskPortfolio;
import net.petrovicky.zonkybot.api.remote.Statistics;
import net.petrovicky.zonkybot.api.remote.Token;
import net.petrovicky.zonkybot.api.remote.ZonkyAPI;
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

    private static class AuthorizationFilter implements ClientRequestFilter {

        private static final Logger LOGGER = LoggerFactory.getLogger(AuthorizationFilter.class);

        @Override
        public void filter(ClientRequestContext clientRequestContext) throws IOException {
            LOGGER.debug("Will '{}' to '{}'.", clientRequestContext.getMethod(), clientRequestContext.getUri());
            String authCode = Base64.getEncoder().encodeToString("web:web".getBytes());
            clientRequestContext.getHeaders().add("Authorization", "Basic " + authCode);
        }
    }

    private static class AuthenticatedFilter implements ClientRequestFilter {

        private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticatedFilter.class);

        private final Token authorization;

        public AuthenticatedFilter(final Token token) {
            this.authorization = token;
            LOGGER.debug("Using Zonky access token '{}'.", authorization.getAccessToken());
        }

        @Override
        public void filter(ClientRequestContext clientRequestContext) throws IOException {
            LOGGER.debug("Will '{}' to '{}'.", clientRequestContext.getMethod(), clientRequestContext.getUri());
            clientRequestContext.getHeaders().add("Authorization", "Bearer " + authorization.getAccessToken());
        }
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(Operations.class);

    private final String username, password;
    private final InvestmentStrategy strategy;
    private ZonkyAPI authenticatedClient;

    public Operations(String username, String password, InvestmentStrategy strategy) {
        this.username = username;
        this.password = password;
        this.strategy = strategy;
    }

    private BigDecimal getAvailableBalance() {
        return authenticatedClient.getWallet().getAvailableBalance();
    }

    private static Map<Rating, BigDecimal> calculateSharesPerRating(Statistics stats) {
        final Map<Rating, BigDecimal> amounts = new EnumMap<>(Rating.class);
        for (RiskPortfolio risk : stats.getRiskPortfolio()) {
            final BigDecimal value = BigDecimal.valueOf(risk.getUnpaid());
            amounts.put(Rating.valueOf(risk.getRating()), value);
        }
        final BigDecimal total = sum(amounts.values());
        final Map<Rating, BigDecimal> result = new EnumMap<>(Rating.class);
        amounts.forEach((rating, amount) -> {
            result.put(rating, amount.divide(total, 4, BigDecimal.ROUND_HALF_EVEN));
        });
        return Collections.unmodifiableMap(result);
    }

    private static BigDecimal sum(Collection<BigDecimal> vals) {
        BigDecimal result = BigDecimal.ZERO;
        for (BigDecimal val : vals) {
            result = result.add(val);
        }
        return result;
    }

    private Loan makeInvestment() {
        Map<Rating, BigDecimal> shareOfRatings = calculateSharesPerRating(authenticatedClient.getStatistics());
        LOGGER.info("Current share of unpaid loans with a given rating is currently: {}.", shareOfRatings);
        SortedMap<BigDecimal, Rating> mostWantedRatings = new TreeMap<>(Comparator.reverseOrder());
        for (Map.Entry<Rating, BigDecimal> entry : shareOfRatings.entrySet()) {
            Rating r = entry.getKey();
            BigDecimal currentShare = entry.getValue();
            BigDecimal maximumAllowedShare = strategy.getTargetShare(r);
            if (currentShare.compareTo(maximumAllowedShare) >= 0) { // we bought too many of this rating; ignore
                continue;
            }
            // sort the ratings by how much we miss them based on the strategy
            mostWantedRatings.put(maximumAllowedShare.subtract(currentShare), r);
        }
        LOGGER.info("According to the investment strategy, the portfolio is low on the following ratings: {}.", mostWantedRatings.values());
        for (Rating r : mostWantedRatings.values()) { // try to invest in a given rating
            Collection<Loan> loans = authenticatedClient.getLoans(Ratings.of(r), strategy.getMinimumInvestmentAmount(r));
            if (loans.size() == 0) {
                LOGGER.info("There are no loans of rating '{}' that match criteria defined by strategy.", r);
                continue;
            }
            for (Loan l : loans) {
                boolean accepted = strategy.isAcceptable(l);
                if (!accepted) {
                    LOGGER.info("According to the investment strategy, loan '{}' is not acceptable.", l);
                    continue;
                }
                int toInvest = (int) Math.min(strategy.getMaximumInvestmentAmount(r), l.getRemainingInvestment());
                LOGGER.info("Attempting to invest {} CZK into loan '{}'.", toInvest, l);
                // FIXME actually invest
                return l;
            }
        }
        return null;
    }

    public int invest() {
        BigDecimal availableBalance = this.getAvailableBalance();
        int minimumInvestmentAmount = strategy.getMinimumInvestmentAmount();
        int investmentsMade = 0;
        // while (availableBalance.compareTo(BigDecimal.valueOf(minimumInvestmentAmount)) >= 0) {
            makeInvestment();
            investmentsMade++;
            availableBalance = this.getAvailableBalance();
        //}
        LOGGER.info("Account balance ({} CZK) less than investment minimum ({} CZK) defined by strategy.",
                availableBalance, minimumInvestmentAmount);
        return investmentsMade;
    }

    public void login() {
        // register Jackson
        ResteasyProviderFactory instance = ResteasyProviderFactory.getInstance();
        RegisterBuiltin.register(instance);
        instance.registerProvider(ResteasyJackson2Provider.class);
        // authenticate
        ResteasyClientBuilder clientBuilder = new ResteasyClientBuilder();
        clientBuilder.providerFactory(instance);
        ResteasyClient client = clientBuilder.build();
        client.register(new AuthorizationFilter());
        Authorization auth = client.target(ZONKY_URL).proxy(Authorization.class);
        Token authorization = auth.login(username, password, "password", "SCOPE_APP_WEB");
        client.close();
        // provide authenticated clients
        authenticatedClient = clientBuilder.build().register(new AuthenticatedFilter(authorization))
                .target(ZONKY_URL).proxy(ZonkyAPI.class);
    }

    public void logout() {
        authenticatedClient.logout();
    }

}
