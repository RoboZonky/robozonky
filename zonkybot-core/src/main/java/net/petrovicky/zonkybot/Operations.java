package net.petrovicky.zonkybot;


import java.io.IOException;
import java.math.BigDecimal;
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

import net.petrovicky.zonkybot.api.remote.Authorization;
import net.petrovicky.zonkybot.api.remote.Investment;
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
    private static final Logger LOGGER = LoggerFactory.getLogger(Operations.class);
    private final String username, password;
    private final InvestmentStrategy strategy;
    private ZonkyAPI authenticatedClient;
    private final boolean dryRun;
    private final int dryRunBalance;

    public Operations(String username, String password, InvestmentStrategy strategy, boolean dryRun, int dryRunBalance) {
        this.username = username;
        this.password = password;
        this.strategy = strategy;
        this.dryRun = dryRun;
        this.dryRunBalance = dryRunBalance;
    }

    private static Map<Rating, BigDecimal> calculateSharesPerRating(Statistics stats,
                                                                    List<Investment> previousInvestments) {
        final Map<Rating, BigDecimal> amounts = new EnumMap<>(Rating.class);
        for (RiskPortfolio risk : stats.getRiskPortfolio()) {
            final BigDecimal value = BigDecimal.valueOf(risk.getUnpaid());
            amounts.put(Rating.valueOf(risk.getRating()), value);
        }
        previousInvestments.forEach(previousInvestment -> {
            /*
             make sure the share reflects the investments made by ZonkyBot which have not yet been reflected in the API
              */
            Rating r = previousInvestment.getLoan().getRating();
            BigDecimal investment = BigDecimal.valueOf(previousInvestment.getInvestedAmount());
            amounts.put(r, amounts.get(r).add(investment));
        });
        final BigDecimal total = sum(amounts.values());
        final Map<Rating, BigDecimal> result = new EnumMap<>(Rating.class);
        amounts.forEach((rating, amount) -> result.put(rating, amount.divide(total, 4, BigDecimal.ROUND_HALF_EVEN)));
        return Collections.unmodifiableMap(result);
    }

    private static BigDecimal sum(Collection<BigDecimal> vals) {
        BigDecimal result = BigDecimal.ZERO;
        for (BigDecimal val : vals) {
            result = result.add(val);
        }
        return result;
    }

    private static boolean isLoanPresent(Loan loan, List<Investment> previousInvestments) {
        for (Investment i : previousInvestments) {
            if (loan.getId() == i.getLoan().getId()) {
                return true;
            }
        }
        return false;
    }

    private Optional<Investment> makeInvestment(List<Investment> previousInvestments) {
        Map<Rating, BigDecimal> shareOfRatings = calculateSharesPerRating(authenticatedClient.getStatistics(),
                previousInvestments);
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
        LOGGER.info("According to the investment strategy, the portfolio is low on the following ratings: {}.",
                mostWantedRatings.values());
        for (Rating r : mostWantedRatings.values()) { // try to invest in a given rating
            Collection<Loan> loans = authenticatedClient.getLoans(Ratings.of(r), strategy.getMinimumInvestmentAmount(r));
            if (loans.size() == 0) {
                LOGGER.info("There are no loans of rating '{}' that match criteria defined by investment strategy.", r);
                continue;
            }
            // sort loans by their term
            List<Loan> sortedLoans = new ArrayList<>();
            while (!loans.isEmpty()) {
                Loan longestTerm = null;
                for (Loan l : loans) {
                    if (longestTerm == null || longestTerm.getTermInMonths() < l.getTermInMonths()) {
                        longestTerm = l;
                    }
                }
                loans.remove(longestTerm);
                sortedLoans.add(longestTerm);
            }
            if (strategy.prefersLongerTerms(r)) {
                LOGGER.info("According to the investment strategy, loans with rating '{}' will be evaluated starting from the longest terms.", r);
            } else {
                LOGGER.info("According to the investment strategy, loans with rating '{}' will be evaluated starting from the shortest terms.", r);
                Collections.reverse(sortedLoans);
            }
            // start investing
            for (Loan l : sortedLoans) {
                if (isLoanPresent(l, previousInvestments)) { // should only happen in dry run
                    LOGGER.info("ZonkyBot already invested in loan '{}', skipping.", l);
                    continue;
                } else if (!strategy.isAcceptable(l)) {
                    LOGGER.info("According to the investment strategy, loan '{}' is not acceptable.", l);
                    continue;
                }
                int toInvest = (int) Math.min(strategy.getMaximumInvestmentAmount(r), l.getRemainingInvestment());
                Optional<Investment> optional = Optional.of(new Investment(l, toInvest));
                if (dryRun) {
                    LOGGER.info("This is a dry run. Not investing {} CZK into loan '{}'.", toInvest, l);
                    return optional;
                } else {
                    LOGGER.info("Attempting to invest {} CZK into loan '{}'.", toInvest, l);
                    try {
                        authenticatedClient.invest(optional.get());
                        LOGGER.warn("Investment operating succeeded.");
                        return optional;
                    } catch (Exception ex) {
                        LOGGER.warn("Investment operating failed.", ex);
                        return Optional.empty();
                    }
                }
            }
        }
        return Optional.empty();
    }

    private BigDecimal getAvailableBalance(List<Investment> previousInvestments) {
        BigDecimal balance = (dryRun && dryRunBalance >= 0) ? BigDecimal.valueOf(dryRunBalance) :
                authenticatedClient.getWallet().getAvailableBalance();
        if (dryRun) {
            for (Investment i : previousInvestments) {
                balance = balance.subtract(BigDecimal.valueOf(i.getInvestedAmount()));
            }
        }
        return balance;
    }

    public int invest() {
        int minimumInvestmentAmount = strategy.getMinimumInvestmentAmount();
        List<Investment> investmentsMade = new ArrayList<>();
        BigDecimal availableBalance = this.getAvailableBalance(investmentsMade);
        LOGGER.info("ZonkyBot starting account balance is {} CZK.", availableBalance);
        while (availableBalance.compareTo(BigDecimal.valueOf(minimumInvestmentAmount)) >= 0) {
            Optional<Investment> investment = makeInvestment(investmentsMade);
            if (investment.isPresent()) {
                investmentsMade.add(investment.get());
                availableBalance = this.getAvailableBalance(investmentsMade);
                LOGGER.info("New account balance is {} CZK.", availableBalance);
            } else {
                break;
            }
        }
        if (availableBalance.compareTo(BigDecimal.valueOf(minimumInvestmentAmount)) < 0) {
            LOGGER.info("Account balance ({} CZK) less than investment minimum ({} CZK) defined by strategy.",
                    availableBalance, minimumInvestmentAmount);
        }
        return investmentsMade.size();
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

}
