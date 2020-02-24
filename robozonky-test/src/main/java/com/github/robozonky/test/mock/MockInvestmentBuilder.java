/*
 * Copyright 2020 The RoboZonky Project
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

package com.github.robozonky.test.mock;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Currency;
import java.util.Optional;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.Ratio;
import com.github.robozonky.api.remote.entities.Investment;
import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.api.remote.enums.InvestmentStatus;
import com.github.robozonky.api.remote.enums.LoanHealth;
import com.github.robozonky.api.remote.enums.PaymentStatus;
import com.github.robozonky.api.remote.enums.Rating;

import static org.mockito.Mockito.*;

public class MockInvestmentBuilder extends BaseMockBuilder<Investment, MockInvestmentBuilder> {

    public static MockInvestmentBuilder fresh() {
        return new MockInvestmentBuilder();
    }

    public static MockInvestmentBuilder fresh(final Loan loan, final int invested) {
        return fresh(loan, BigDecimal.valueOf(invested));
    }

    public static MockInvestmentBuilder fresh(final Loan loan, final BigDecimal invested) {
        return fresh()
                .setId(RANDOM.nextInt())
                .setLoanId(loan.getId())
                .setInterestRate(loan.getInterestRate())
                .setRevenueRate(loan.getRevenueRate().orElse(null))
                .setCurrency(loan.getCurrency())
                .setAmount(invested)
                .setLoanAnnuity(loan.getAnnuity())
                .setLoanAmount(loan.getAmount())
                .setLoanTermInMonth(loan.getTermInMonths())
                .setRemainingPrincipal(invested)
                .setPurchasePrice(invested)
                .setRating(loan.getRating())
                .setPaidInterest(BigDecimal.ZERO)
                .setPaidPenalty(BigDecimal.ZERO)
                .setPaidPrincipal(BigDecimal.ZERO)
                .setInvestmentDate(OffsetDateTime.now());
    }

    public MockInvestmentBuilder() {
        super(Investment.class);
        when(mock.getId()).thenReturn(RANDOM.nextLong());
    }

    public MockInvestmentBuilder setId(final long id) {
        when(mock.getId()).thenReturn(id);
        return this;
    }

    public MockInvestmentBuilder setAmount(final BigDecimal amount) {
        when(mock.getAmount()).thenReturn(Money.from(amount));
        return this;
    }

    public MockInvestmentBuilder setLoanAnnuity(final Money annuity) {
        when(mock.getLoanAnnuity()).thenReturn(annuity);
        return this;
    }

    public MockInvestmentBuilder setLoanAmount(final Money amount) {
        when(mock.getLoanAmount()).thenReturn(amount);
        return this;
    }

    public MockInvestmentBuilder setRevenueRate(final Ratio rate) {
        when(mock.getRevenueRate()).thenReturn(Optional.ofNullable(rate));
        return this;
    }

    public MockInvestmentBuilder setInterestRate(final Ratio rate) {
        when(mock.getInterestRate()).thenReturn(rate);
        return this;
    }

    public MockInvestmentBuilder setRating(final Rating rating) {
        when(mock.getRating()).thenReturn(rating);
        return this;
    }

    public MockInvestmentBuilder setCurrency(final Currency currency) {
        when(mock.getCurrency()).thenReturn(currency);
        return this;
    }

    public MockInvestmentBuilder setLoanId(final int loanId) {
        when(mock.getLoanId()).thenReturn(loanId);
        return this;
    }

    public MockInvestmentBuilder setSmpFee(final BigDecimal bigDecimal) {
        if (bigDecimal == null) {
            when(mock.getSmpFee()).thenReturn(Optional.empty());
        } else {
            when(mock.getSmpFee()).thenReturn(Optional.ofNullable(Money.from(bigDecimal)));
        }
        return this;
    }

    public MockInvestmentBuilder setPurchasePrice(final BigDecimal bigDecimal) {
        when(mock.getPurchasePrice()).thenReturn(Money.from(bigDecimal));
        return this;
    }

    public MockInvestmentBuilder setSellPrice(final BigDecimal bigDecimal) {
        when(mock.getSmpPrice()).thenReturn(Optional.of(Money.from(bigDecimal)));
        return this;
    }

    public MockInvestmentBuilder setOnSmp(final boolean isOnSmp) {
        when(mock.isOnSmp()).thenReturn(isOnSmp);
        return this;
    }


    public MockInvestmentBuilder setStatus(final InvestmentStatus status) {
        when(mock.getStatus()).thenReturn(status);
        return this;
    }

    public MockInvestmentBuilder setPaymentStatus(final PaymentStatus status) {
        when(mock.getPaymentStatus()).thenReturn(Optional.ofNullable(status));
        return this;
    }

    public MockInvestmentBuilder setRemainingPrincipal(final BigDecimal remainingPrincipal) {
        when(mock.getRemainingPrincipal()).thenReturn(Optional.of(Money.from(remainingPrincipal)));
        return this;
    }

    public MockInvestmentBuilder setExpectedInterest(final BigDecimal expectedInterest) {
        when(mock.getExpectedInterest()).thenReturn(Money.from(expectedInterest));
        return this;
    }

    public MockInvestmentBuilder setPaidInterest(final BigDecimal paidInterest) {
        when(mock.getPaidInterest()).thenReturn(Money.from(paidInterest));
        return this;
    }

    public MockInvestmentBuilder setLoanTermInMonth(final int loanTermInMonth) {
        when(mock.getLoanTermInMonth()).thenReturn(loanTermInMonth);
        return this;
    }

    public MockInvestmentBuilder setLoanHealthInfo(final LoanHealth loanHealthInfo) {
        when(mock.getLoanHealthInfo()).thenReturn(Optional.of(loanHealthInfo));
        return this;
    }

    public MockInvestmentBuilder setIsCanBeOffered(final boolean isCanBeOffered) {
        when(mock.isCanBeOffered()).thenReturn(isCanBeOffered);
        return this;
    }

    public MockInvestmentBuilder setInWithdrawal(final boolean isInWithdrawal) {
        when(mock.isInWithdrawal()).thenReturn(isInWithdrawal);
        return this;
    }

    public MockInvestmentBuilder setLegalDpd(final int legalDpd) {
        when(mock.getLegalDpd()).thenReturn(legalDpd);
        return this;
    }

    public MockInvestmentBuilder setPaidPenalty(final BigDecimal paidPenalty) {
        when(mock.getPaidPenalty()).thenReturn(Money.from(paidPenalty));
        return this;
    }

    public MockInvestmentBuilder setPaidPrincipal(final BigDecimal paidPrincipal) {
        when(mock.getPaidPrincipal()).thenReturn(Money.from(paidPrincipal));
        return this;
    }

    public MockInvestmentBuilder setInvestmentDate(final OffsetDateTime investmentDate) {
        when(mock.getInvestmentDate()).thenReturn(investmentDate);
        return this;
    }

}
