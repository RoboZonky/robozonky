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

import com.github.robozonky.api.Money;
import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.internal.remote.entities.InvestmentImpl;

public class MockInvestmentBuilder extends BaseMockBuilder<InvestmentImpl, MockInvestmentBuilder> {

    private MockInvestmentBuilder() {
        super(InvestmentImpl.class);
    }

    public static MockInvestmentBuilder fresh() {
        return new MockInvestmentBuilder();
    }

    public static MockInvestmentBuilder fresh(final Loan loan, final int invested) {
        return fresh(loan, BigDecimal.valueOf(invested));
    }

    public static MockInvestmentBuilder fresh(final Loan loan, final BigDecimal invested) {
        return fresh()
            .set(InvestmentImpl::setId, RANDOM.nextLong())
            .set(InvestmentImpl::setLoanId, loan.getId())
            .set(InvestmentImpl::setInterestRate, loan.getInterestRate())
            .set(InvestmentImpl::setRevenueRate, loan.getRevenueRate()
                .orElse(null))
            .set(InvestmentImpl::setCurrency, loan.getCurrency())
            .set(InvestmentImpl::setAmount, Money.from(invested))
            .set(InvestmentImpl::setLoanAnnuity, loan.getAnnuity())
            .set(InvestmentImpl::setLoanAmount, loan.getAmount())
            .set(InvestmentImpl::setLoanTermInMonth, loan.getTermInMonths())
            .set(InvestmentImpl::setRemainingPrincipal, Money.from(invested))
            .set(InvestmentImpl::setPurchasePrice, Money.from(invested))
            .set(InvestmentImpl::setRating, loan.getRating())
            .set(InvestmentImpl::setPaidInterest, Money.from(BigDecimal.ZERO))
            .set(InvestmentImpl::setPaidPenalty, Money.from(BigDecimal.ZERO))
            .set(InvestmentImpl::setPaidPrincipal, Money.from(BigDecimal.ZERO))
            .set(InvestmentImpl::setInvestmentDate, OffsetDateTime.now())
            .set(InvestmentImpl::setInsuranceActive, loan.isInsuranceActive());
    }

}
