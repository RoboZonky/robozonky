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
import java.util.function.Function;
import java.util.stream.Collectors;

import com.github.triceo.robozonky.operations.InvestOperation;
import com.github.triceo.robozonky.remote.BlockedAmount;
import com.github.triceo.robozonky.remote.Investment;
import com.github.triceo.robozonky.remote.InvestmentStatus;
import com.github.triceo.robozonky.remote.InvestmentStatuses;
import com.github.triceo.robozonky.remote.Loan;
import com.github.triceo.robozonky.remote.Rating;
import com.github.triceo.robozonky.remote.RiskPortfolio;
import com.github.triceo.robozonky.remote.Statistics;
import com.github.triceo.robozonky.remote.ZonkyApi;
import com.github.triceo.robozonky.remote.ZotifyApi;
import com.github.triceo.robozonky.strategy.InvestmentStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Investor {

    private static final Logger LOGGER = LoggerFactory.getLogger(Investor.class);

    private final ZonkyApi zonkyApi;
    private final ZotifyApi zotifyApi;
    private final BigDecimal initialBalance;
    private final InvestmentStrategy strategy;

    /**
     * Determine whether or not a given loan is present among existing investments.
     *
     * @param loan Loan in question.
     * @param investments Known investments.
     * @return True if present.
     */
    static boolean isLoanPresent(final Loan loan, final Iterable<Investment> investments) {
        for (final Investment i : investments) {
            if (loan.getId() == i.getLoanId()) {
                return true;
            }
        }
        return false;
    }

    static Collection<Investment> mergeInvestments(final Collection<Investment> left,
                                                   final Collection<Investment> right) {
        if (left.isEmpty() && right.isEmpty()) {
            return Collections.emptyList();
        } else if (left.isEmpty()) {
            return Collections.unmodifiableCollection(right);
        } else if (right.isEmpty()) {
            return Collections.unmodifiableCollection(left);
        } else {
            final Map<Integer, Investment> investments
                    = left.stream().collect(Collectors.toMap(Investment::getLoanId, Function.identity()));
            right.stream().filter(investment -> !investments.containsKey(investment.getLoanId()))
                    .forEach(investment -> investments.put(investment.getLoanId(), investment));
            return Collections.unmodifiableCollection(investments.values());
        }
    }

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

    static Optional<Investment> invest(final ZonkyApi api, final Loan l, final int amount, final BigDecimal balance) {
        if (amount < InvestmentStrategy.MINIMAL_INVESTMENT_ALLOWED) {
            Investor.LOGGER.info("Not investing into loan '{}', since investment ({} CZK) less than bare minimum.",
                    l, amount);
            return Optional.empty();
        } else if (amount > balance.intValue()) {
            Investor.LOGGER.info("Not investing into loan '{}', {} CZK to invest is more than {} CZK balance.",
                    l, amount, balance);
            return Optional.empty();
        }
        final Investment investment = new Investment(l, amount);
        return new InvestOperation().apply(api, investment);
    }

    public Investor(final ZonkyApi zonky, final ZotifyApi zotify, final InvestmentStrategy strategy,
                    final BigDecimal initialBalance) {
        this.zonkyApi = zonky;
        this.zotifyApi = zotify;
        this.initialBalance = initialBalance;
        Investor.LOGGER.info("RoboZonky starting account balance is {} CZK.", this.initialBalance);
        this.strategy = strategy;
    }

    /**
     * Ask the strategy for a prioritized list of loans to invest into.
     *
     * @param investments Investments already made, not to choose any particular loan twice.
     * @param ratingShare How much do investments with a given rating make up of the total invested money.
     * @param balance Latest known Zonky account balance.
     * @return List of loans available to be invested into, in the order of decreasing priority.
     */
    private List<Loan> askStrategyForLoans(final Collection<Investment> investments,
                                           final Map<Rating, BigDecimal> ratingShare, final BigDecimal balance) {
        final List<Loan> allLoansFromApi = this.zotifyApi.getLoans();
        final List<Loan> afterInvestmentsExcluded = allLoansFromApi.stream()
                .filter(l -> !Investor.isLoanPresent(l, investments)).collect(Collectors.toList());
        return this.strategy.getMatchingLoans(afterInvestmentsExcluded, ratingShare, balance);
    }

    /**
     * Choose from available loans the most important loan and invest into it.
     *
     * @param loans List of loans to choose from, ordered from most important.
     * @param ratingShare How much do investments with a given rating make up of the total invested money.
     * @param balance Latest known Zonky account balance.
     * @return Present only if Zonky API confirmed money was invested or if dry run.
     */
    private Optional<Investment> findLoanAndInvest(final List<Loan> loans, final Map<Rating, BigDecimal> ratingShare,
                                                   final BigDecimal balance) {
        for (final Loan l : loans) { // try investing until one loan succeeds
            final int invest = this.strategy.recommendInvestmentAmount(l, ratingShare, balance);
            Investor.LOGGER.debug("Strategy recommended to invest {} CZK on balance of {} CZK.", invest, balance);
            final Optional<Investment> investment = Investor.invest(this.zonkyApi, l, invest, balance);
            if (investment.isPresent()) {
                return investment;
            }
        }
        return Optional.empty();
    }

    private Collection<Investment> retrieveInvestmentsRepresentedByBlockedAmounts() {
        final List<BlockedAmount> amounts = this.zonkyApi.getBlockedAmounts();
        final List<Investment> investments = new ArrayList<>(amounts.size());
        for (final BlockedAmount blocked: amounts) {
            final Loan l = this.zonkyApi.getLoan(blocked.getLoanId());
            final Investment i = new Investment(l, blocked.getAmount());
            investments.add(i);
            Investor.LOGGER.debug("{} CZK is being blocked by loan {}.", blocked.getAmount(), blocked.getLoanId());
        }
        return investments;
    }

    public Collection<Investment> invest() {
        // make sure we have enough money to invest
        final int minimumInvestmentAmount = InvestmentStrategy.MINIMAL_INVESTMENT_ALLOWED;
        BigDecimal balance = this.initialBalance;
        if (balance.compareTo(BigDecimal.valueOf(minimumInvestmentAmount)) < 0) {
            return Collections.emptyList(); // no need to do anything else
        }
        // retrieve a list of loans that the user already put money into
        Collection<Investment> investments = Investor.mergeInvestments(
                this.zonkyApi.getInvestments(InvestmentStatuses.of(InvestmentStatus.SIGNED)),
                this.retrieveInvestmentsRepresentedByBlockedAmounts());
        Investor.LOGGER.debug("The following loans are coming from the API as already invested into: {}", investments);
        final Statistics stats = this.zonkyApi.getStatistics();
        // and start investing
        final Collection<Investment> investmentsMade = new ArrayList<>();
        do {
            // calculate share of particular ratings on the overall investment pie
            final Map<Rating, BigDecimal> ratingShare = Investor.calculateSharesPerRating(stats, investments);
            Investor.LOGGER.debug("Current share of unpaid loans with a given rating is: {}.", ratingShare);
            final List<Loan> loans = this.askStrategyForLoans(investments, ratingShare, balance);
            if (loans == null || loans.size() == 0) {
                Investor.LOGGER.info("There are no loans matching the investment strategy.");
                break;
            } else {
                Investor.LOGGER.debug("Investment strategy accepted the following loans: {}", loans);
            }
            final Optional<Investment> investment = this.findLoanAndInvest(loans, ratingShare, balance);
            if (!investment.isPresent()) {
                break;
            }
            final Investment i = investment.get();
            investmentsMade.add(i);
            investments = Investor.mergeInvestments(investments, Collections.singletonList(i));
            final BigDecimal amount = BigDecimal.valueOf(i.getAmount());
            balance = balance.subtract(amount);
            Investor.LOGGER.info("New account balance is {} CZK.", balance);
        } while (balance.compareTo(BigDecimal.valueOf(minimumInvestmentAmount)) >= 0);
        return Collections.unmodifiableCollection(investmentsMade);
    }

    public Optional<Investment> invest(final int loanId, final int loanAmount) {
        return Investor.invest(this.zonkyApi, this.zonkyApi.getLoan(loanId), loanAmount, this.initialBalance);
    }

}
