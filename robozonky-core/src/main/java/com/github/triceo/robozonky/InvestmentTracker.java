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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.triceo.robozonky.api.State;
import com.github.triceo.robozonky.api.remote.entities.Investment;
import com.github.triceo.robozonky.api.strategies.LoanDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class holds the state for the investment loop inside {@link Investor}. It's job is to keep track of which
 * loans came from Zonky, which have been invested into during this session, and which have been handled externally.
 */
final class InvestmentTracker {

    static final State.ClassSpecificState STATE = State.INSTANCE.forClass(InvestmentTracker.class);
    private static final String UNTOUCHABLE_INVESTMENTS_ID = "untouchableInvestments";
    private static final Logger LOGGER = LoggerFactory.getLogger(InvestmentTracker.class);

    private static Collection<Integer> readUntouchableInvestments() {
        final Optional<String> result = InvestmentTracker.STATE.getValue(InvestmentTracker.UNTOUCHABLE_INVESTMENTS_ID);
        return result.map(s -> Stream.of(s.split(","))
                .map(Integer::parseInt)
                .collect(Collectors.toList()))
                .orElse(new ArrayList<>());
    }

    private static void writeUntouchableInvestments(final Collection<Integer> rejectedInvestments) {
        final String result = rejectedInvestments.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        InvestmentTracker.STATE.setValue(InvestmentTracker.UNTOUCHABLE_INVESTMENTS_ID, result);
    }

    private final Collection<Integer> rejectedLoanIds;
    private final List<LoanDescriptor> loansStillAvailable;
    private final Collection<Investment> investmentsMade = new LinkedHashSet<>();
    private final Collection<Investment> investmentsPreviouslyMade = new HashSet<>();
    private BigDecimal currentBalance;

    public InvestmentTracker(final Collection<LoanDescriptor> availableLoans, final BigDecimal currentBalance) {
        this.currentBalance = currentBalance;
        this.rejectedLoanIds = InvestmentTracker.readUntouchableInvestments();
        InvestmentTracker.LOGGER.info("Loans previously rejected: {}", rejectedLoanIds);
        this.loansStillAvailable = availableLoans.stream()
                .filter(l -> !this.rejectedLoanIds.contains(l.getLoan().getId()))
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
        this.ignoreLoanForNow(investment.getLoanId());
        this.investmentsMade.add(investment);
        this.currentBalance = this.currentBalance.subtract(BigDecimal.valueOf(investment.getAmount()));
    }

    /**
     * Mark a given loan as no longer relevant for this session.
     *
     * @param loanId ID of the loan in question.
     */
    public synchronized void ignoreLoanForNow(final int loanId) {
        this.loansStillAvailable.removeIf(l -> loanId == l.getLoan().getId());
    }

    /**
     * Mark a given loan as untouchable and persist the information. Every future session will respect this.
     *
     * @param loanId ID of the loan in question.
     */
    public synchronized void ignoreLoanForever(final int loanId) {
        this.ignoreLoanForNow(loanId);
        this.rejectedLoanIds.add(loanId);
        InvestmentTracker.writeUntouchableInvestments(this.rejectedLoanIds);
    }

    /**
     * Mark a bunch of investments created externally (received from the API), so that they are not available in this
     * session but still could be used for portfolio maths.
     *
     * @param investments Investments to carry over from previous sessions.
     */
    public synchronized void registerExistingInvestments(final Collection<Investment> investments) {
        investments.forEach(i -> this.ignoreLoanForNow(i.getLoanId()));
        investmentsPreviouslyMade.addAll(investments);
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
