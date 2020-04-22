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

package com.github.robozonky.strategy.natural;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.internal.remote.entities.LoanImpl;
import com.github.robozonky.test.AbstractMinimalRoboZonkyTest;
import com.github.robozonky.test.mock.MockLoanBuilder;

class InvestmentSizeRecommenderTest extends AbstractMinimalRoboZonkyTest {

    private static final int MAXIMUM_SHARE = 1;
    private static final int MAXIMUM_INVESTMENT = 1000;

    private static Loan mockLoan(final int amount) {
        return new MockLoanBuilder()
            .set(LoanImpl::setRating, Rating.A)
            .set(LoanImpl::setAmount, Money.from(amount))
            .set(LoanImpl::setRemainingInvestment, Money.from(amount))
            .set(LoanImpl::setReservedAmount, Money.from(0))
            .build();
    }

    private static ParsedStrategy getStrategy() {
        // no filters, as the SUT doesn't do filtering; no portfolio, as that is not used either
        final DefaultValues defaults = new DefaultValues(DefaultPortfolio.EMPTY);
        defaults.setInvestmentShare(new DefaultInvestmentShare(MAXIMUM_SHARE));
        final MoneyRange target = new MoneyRange(MAXIMUM_INVESTMENT);
        return new ParsedStrategy(defaults, Collections.emptyList(),
                Collections.singletonMap(mockLoan(0).getRating(), target),
                Collections.emptyMap());
    }

    @Test
    void withSpecificRating() {
        final ParsedStrategy s = getStrategy();
        final InvestmentSizeRecommender r = new InvestmentSizeRecommender(s);
        // with unlimited balance, make maximum possible recommendation
        final Loan loan = mockLoan(50_000);
        final Money actualInvestment = r.apply(loan, mockSessionInfo());
        // at most 1 percent of 50000, rounded down to nearest increment of 200
        assertThat(actualInvestment).isEqualTo(Money.from(400));
    }

    @Test
    void byDefault() {
        final ParsedStrategy s = getStrategy();
        final Loan l = mockLoan(100_000);
        final InvestmentSizeRecommender r = new InvestmentSizeRecommender(s);
        // make maximum possible recommendation
        final Money actualInvestment = r.apply(l, mockSessionInfo());
        assertThat(actualInvestment).isEqualTo(Money.from(MAXIMUM_INVESTMENT));
    }

    @Test
    void nothingMoreToInvest() {
        final SessionInfo sessionInfo = mockSessionInfo();
        final ParsedStrategy s = getStrategy();
        final Loan l = mockLoan(sessionInfo.getMinimumInvestmentAmount()
            .getValue()
            .intValue() - 1);
        final InvestmentSizeRecommender r = new InvestmentSizeRecommender(s);
        // with unlimited balance, make maximum possible recommendation
        final Money actualInvestment = r.apply(l, sessionInfo);
        assertThat(actualInvestment).isEqualTo(Money.ZERO);
    }

    @Test
    void minimumOverRemaining() {
        final SessionInfo sessionInfo = mockSessionInfo();
        final Money minimumInvestment = sessionInfo.getMinimumInvestmentAmount();
        final Loan l = mockLoan(minimumInvestment.getValue()
            .intValue() - 1);
        final ParsedStrategy s = mock(ParsedStrategy.class);
        when(s.getMinimumInvestmentSize(eq(l.getRating()))).thenReturn(minimumInvestment);
        when(s.getMaximumInvestmentSize(eq(l.getRating()))).thenReturn(minimumInvestment.add(minimumInvestment));
        when(s.getMaximumInvestmentShareInPercent()).thenReturn(100);
        final InvestmentSizeRecommender r = new InvestmentSizeRecommender(s);
        assertThat(r.apply(l, sessionInfo)).isEqualTo(Money.ZERO);
    }

    @Test
    void recommendationRoundedUnderMinimum() {
        final SessionInfo sessionInfo = mockSessionInfo();
        final Money minimumInvestment = sessionInfo.getMinimumInvestmentAmount();
        final Loan l = mockLoan(minimumInvestment.getValue()
            .intValue() - 1);
        final ParsedStrategy s = mock(ParsedStrategy.class);
        // next line will cause the recommendation to be rounded to 800, which will be below the minimum investment
        when(s.getMinimumInvestmentSize(eq(l.getRating()))).thenReturn(
                minimumInvestment.subtract(1));
        when(s.getMaximumInvestmentSize(eq(l.getRating())))
            .thenReturn(minimumInvestment);
        when(s.getMaximumInvestmentShareInPercent()).thenReturn(100);
        final InvestmentSizeRecommender r = new InvestmentSizeRecommender(s);
        assertThat(r.apply(l, sessionInfo)).isEqualTo(Money.ZERO);
    }
}
