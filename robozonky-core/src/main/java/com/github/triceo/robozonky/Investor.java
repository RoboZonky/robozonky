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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.github.triceo.robozonky.operations.InvestOperation;
import com.github.triceo.robozonky.remote.Investment;
import com.github.triceo.robozonky.remote.InvestmentStatus;
import com.github.triceo.robozonky.remote.InvestmentStatuses;
import com.github.triceo.robozonky.remote.Loan;
import com.github.triceo.robozonky.remote.Statistics;
import com.github.triceo.robozonky.remote.ZonkyApi;
import com.github.triceo.robozonky.remote.ZotifyApi;
import com.github.triceo.robozonky.strategy.InvestmentStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Investor {

    private static final Logger LOGGER = LoggerFactory.getLogger(Investor.class);

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

    static Optional<Investment> invest(final ZonkyApi api, final Loan l, final int amount, final BigDecimal balance) {
        if (amount < InvestmentStrategy.MINIMAL_INVESTMENT_ALLOWED) {
            Investor.LOGGER.info("Not investing into loan '{}', since investment ({} CZK) less than bare minimum.",
                    l, amount);
            return Optional.empty();
        } else if (amount > balance.intValue()) {
            Investor.LOGGER.info("Not investing into loan '{}', {} CZK to invest is more than {} CZK balance.",
                    l, amount, balance);
            return Optional.empty();
        } else if (amount > l.getAmount()) {
            Investor.LOGGER.info("Not investing into loan '{}', {} CZK to invest is more than {} CZK loan amount.",
                    l, amount, l.getAmount());
            return Optional.empty();
        }
        final Investment investment = new Investment(l, amount);
        return new InvestOperation().apply(api, investment);
    }

    static List<Investment> retrieveInvestmentsRepresentedByBlockedAmounts(final ZonkyApi api) {
        return Collections.unmodifiableList(api.getBlockedAmounts().stream()
                .filter(blocked -> blocked.getLoanId() > 0) // 0 == Zonky investors' fee
                .map(blocked -> {
                    final int loanId = blocked.getLoanId();
                    final int loanAmount = blocked.getAmount();
                    final Loan l = api.getLoan(loanId);
                    final Investment i = new Investment(l, loanAmount);
                    Investor.LOGGER.debug("{} CZK is being blocked by loan {}.", loanAmount, loanId);
                    return i;
                }).collect(Collectors.toList()));
    }

    private final ZonkyApi zonkyApi;
    private final ZotifyApi zotifyApi;
    private final BigDecimal initialBalance;
    private final InvestmentStrategy strategy;

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
     * @param portfolio Overview of the current user's portfolio.
     * @return List of loans available to be invested into, in the order of decreasing priority.
     */
    List<Loan> askStrategyForLoans(final Collection<Investment> investments,
                                   final PortfolioOverview portfolio) {
        final List<Loan> allLoansFromApi = this.zotifyApi.getLoans();
        final List<Loan> afterInvestmentsExcluded = allLoansFromApi.stream()
                .filter(l -> !Investor.isLoanPresent(l, investments)).collect(Collectors.toList());
        return this.strategy.getMatchingLoans(afterInvestmentsExcluded, portfolio);
    }

    /**
     * Choose from available loans the most important loan and invest into it.
     *
     * @param loans List of loans to choose from, ordered from most important.
     * @param portfolio Overview of the current user's portfolio.
     * @return Present only if Zonky API confirmed money was invested or if dry run.
     */
    Optional<Investment> findLoanAndInvest(final List<Loan> loans, final PortfolioOverview portfolio) {
        for (final Loan l : loans) { // try investing until one loan succeeds
            final int invest = this.strategy.recommendInvestmentAmount(l, portfolio);
            Investor.LOGGER.debug("Strategy recommended to invest {} CZK on balance of {} CZK.", invest,
                    portfolio.getCzkAvailable());
            final Optional<Investment> investment = Investor.invest(this.zonkyApi, l, invest,
                    portfolio.getCzkAvailable());
            if (investment.isPresent()) {
                return investment;
            }
        }
        return Optional.empty();
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
                Investor.retrieveInvestmentsRepresentedByBlockedAmounts(this.zonkyApi));
        Investor.LOGGER.debug("The following loans are coming from the API as already invested into: {}", investments);
        final Statistics stats = this.zonkyApi.getStatistics();
        // and start investing
        final Collection<Investment> investmentsMade = new ArrayList<>();
        do {
            // calculate share of particular ratings on the overall investment pie
            final PortfolioOverview portfolio = PortfolioOverview.calculate(balance, stats, investments);
            Investor.LOGGER.debug("Current share of unpaid loans with a given rating is: {}.",
                    portfolio.getSharesOnInvestment());
            final List<Loan> loans = this.askStrategyForLoans(investments, portfolio);
            if (loans == null || loans.size() == 0) {
                Investor.LOGGER.info("There are no loans matching the investment strategy.");
                break;
            } else {
                Investor.LOGGER.debug("Investment strategy accepted the following loans: {}", loans);
            }
            final Optional<Investment> investment = this.findLoanAndInvest(loans, portfolio);
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
