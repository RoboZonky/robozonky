/*
 * Copyright 2017 Lukáš Petrovický
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

package com.github.triceo.robozonky.app.investing;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.triceo.robozonky.api.remote.entities.Investment;
import com.github.triceo.robozonky.api.strategies.LoanDescriptor;
import com.github.triceo.robozonky.internal.api.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class holds the state for the investment loop inside {@link Investor}. It's job is to keep track of which
 * loans came from Zonky, which have been invested into during this session, and which have been handled externally.
 */
final class InvestmentTracker {

    static final State.ClassSpecificState STATE = State.INSTANCE.forClass(InvestmentTracker.class);
    private static final String SEEN_INVESTMENTS_ID = "seenInvestments";
    private static final String UNTOUCHABLE_INVESTMENTS_ID = "untouchableInvestments";
    private static final Logger LOGGER = LoggerFactory.getLogger(InvestmentTracker.class);

    private static Collection<Integer> readInvestments(final String propertyName) {
        final Optional<String> result = InvestmentTracker.STATE.getValue(propertyName);
        return result.map(s -> Stream.of(s.split(","))
                .map(Integer::parseInt)
                .sorted()
                .collect(Collectors.toSet()))
                .orElse(new LinkedHashSet<>(0));
    }

    private static Collection<Integer> readUntouchableInvestments() {
        return InvestmentTracker.readInvestments(InvestmentTracker.UNTOUCHABLE_INVESTMENTS_ID);
    }

    private static Collection<Integer> readSeenInvestments() {
        return InvestmentTracker.readInvestments(InvestmentTracker.SEEN_INVESTMENTS_ID);
    }

    private static void writeInvestments(final String propertyName, final Collection<Integer> rejectedInvestments) {
        final String result = rejectedInvestments.stream()
                .distinct()
                .sorted()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        InvestmentTracker.STATE.setValue(propertyName, result);
    }

    private static void writeUntouchableInvestments(final Collection<Integer> rejectedInvestments) {
        InvestmentTracker.writeInvestments(InvestmentTracker.UNTOUCHABLE_INVESTMENTS_ID, rejectedInvestments);
    }

    private static void writeSeenInvestments(final Collection<Integer> seenInvestments) {
        InvestmentTracker.writeInvestments(InvestmentTracker.SEEN_INVESTMENTS_ID, seenInvestments);
    }

    private static Collection<Integer> cleanIds(final Collection<Integer> loanIds,
                                                final Collection<LoanDescriptor> availableLoans) {
        final Collection<Integer> availableLoanIds = availableLoans.stream()
                .map(l -> l.getLoan().getId())
                .collect(Collectors.toSet());
        loanIds.removeIf(id -> !availableLoanIds.contains(id)); // prevent old stale IDs from piling up
        return loanIds;
    }

    private final Collection<Integer> discardedLoans, seenLoans;
    private final List<LoanDescriptor> loansStillAvailable;
    private final Collection<Investment> investmentsMade = new LinkedHashSet<>();
    private final Collection<Investment> investmentsPreviouslyMade = new HashSet<>();
    private BigDecimal currentBalance;

    public InvestmentTracker(final Collection<LoanDescriptor> availableLoans, final BigDecimal currentBalance) {
        this.currentBalance = currentBalance;
        this.discardedLoans = InvestmentTracker.cleanIds(InvestmentTracker.readUntouchableInvestments(), availableLoans);
        InvestmentTracker.LOGGER.debug("Loans previously discarded: {}", discardedLoans);
        this.seenLoans = InvestmentTracker.cleanIds(InvestmentTracker.readSeenInvestments(), availableLoans);
        InvestmentTracker.LOGGER.debug("Loans previously seen: {}", seenLoans);
        this.loansStillAvailable = availableLoans.stream()
                .filter(l -> !this.discardedLoans.contains(l.getLoan().getId()))
                .collect(Collectors.toList());
    }

    /**
     * Get account balance reflecting the current state of the tracker.
     *
     * @return The balance.
     */
    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }

    /**
     * Register a successful investment and reflect it in account balance.
     *
     * @param investment Investment to register as successful.
     */
    public synchronized void makeInvestment(final Investment investment) {
        this.ignoreLoan(investment.getLoanId());
        this.investmentsMade.add(investment);
        this.currentBalance = this.currentBalance.subtract(BigDecimal.valueOf(investment.getAmount()));
    }

    public synchronized boolean isSeenBefore(final int loanId) {
        return this.seenLoans.contains(loanId);
    }

    public synchronized boolean isDiscarded(final int loanId) {
        return this.discardedLoans.contains(loanId);
    }

    /**
     * Mark a given loan as no longer relevant for this session.
     *
     * @param loanId ID of the loan in question.
     */
    public synchronized void ignoreLoan(final int loanId) {
        this.loansStillAvailable.removeIf(l -> loanId == l.getLoan().getId());
        this.seenLoans.add(loanId);
        InvestmentTracker.writeSeenInvestments(this.seenLoans);
    }

    /**
     * Mark a given loan as untouchable and persist the information. Every future session will respect this.
     *
     * @param loanId ID of the loan in question.
     */
    public synchronized void discardLoan(final int loanId) {
        this.ignoreLoan(loanId);
        this.discardedLoans.add(loanId);
        InvestmentTracker.writeUntouchableInvestments(this.discardedLoans);
    }

    /**
     * Mark a bunch of investments created externally (received from the API), so that they are not available in this
     * session but still could be used for portfolio maths.
     *
     * @param investments Investments to carry over from previous sessions.
     */
    public synchronized void registerExistingInvestments(final Collection<Investment> investments) {
        investments.forEach(i -> this.ignoreLoan(i.getLoanId()));
        investmentsPreviouslyMade.addAll(investments);
        InvestmentTracker.LOGGER.debug("Loans still available: {}.", this.loansStillAvailable.stream()
                .map(l -> l.getLoan().getId())
                .sorted()
                .map(String::valueOf)
                .collect(Collectors.joining(", ", "[", "]")));
    }

    /**
     *
     * @return Investments made during this session.
     */
    public synchronized Collection<Investment> getInvestmentsMade() {
        return Collections.unmodifiableCollection(new ArrayList<>(this.investmentsMade));
    }

    /**
     *
     * @return Investments made during this session, as well as unsettled investments from the API.
     */
    public synchronized Collection<Investment> getAllInvestments() {
        return Stream.concat(this.investmentsPreviouslyMade.stream(), this.investmentsMade.stream())
                .collect(Collectors.toList());
    }

    /**
     *
     * @return Loans still available for this session.
     */
    public synchronized List<LoanDescriptor> getAvailableLoans() {
        return Collections.unmodifiableList(new ArrayList<>(this.loansStillAvailable));
    }

}
