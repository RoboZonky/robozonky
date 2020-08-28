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

import com.github.robozonky.api.Money;
import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.api.remote.entities.LoanHealthStats;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.internal.remote.entities.AmountsImpl;
import com.github.robozonky.internal.remote.entities.InvestmentImpl;
import com.github.robozonky.internal.remote.entities.InvestmentLoanDataImpl;
import com.github.robozonky.internal.remote.entities.LoanImpl;

public class MockInvestmentBuilder extends BaseMockBuilder<InvestmentImpl, MockInvestmentBuilder> {

    private MockInvestmentBuilder() {
        super(InvestmentImpl.class);
        set(InvestmentImpl::setId, RANDOM.nextLong());
    }

    public static MockInvestmentBuilder fresh() {
        return new MockInvestmentBuilder();
    }

    public static MockInvestmentBuilder fresh(final Loan loan, final int invested) {
        return fresh(loan, null, invested);
    }

    public static MockInvestmentBuilder fresh(final Loan loan, final LoanHealthStats loanHealthStats,
            final int invested) {
        return fresh(loan, loanHealthStats, BigDecimal.valueOf(invested));
    }

    public static MockInvestmentBuilder fresh(final Loan loan, final BigDecimal invested) {
        return fresh()
            .set(InvestmentImpl::setLoan, new InvestmentLoanDataImpl(loan))
            .set(InvestmentImpl::setPrincipal, new AmountsImpl(Money.from(invested)));
    }

    public static MockInvestmentBuilder fresh(final Loan loan, final LoanHealthStats loanHealthStats,
            final BigDecimal invested) {
        LoanImpl loanImpl = (LoanImpl) loan;
        if (loanImpl.getRating() == null) {
            loanImpl.setRating(Rating.AAAAA);
        }
        InvestmentLoanDataImpl ild = loanHealthStats == null ? new InvestmentLoanDataImpl(loanImpl)
                : new InvestmentLoanDataImpl(loanImpl, loanHealthStats);
        return fresh()
            .set(InvestmentImpl::setLoan, ild)
            .set(InvestmentImpl::setPrincipal, new AmountsImpl(Money.from(invested)));
    }

}
