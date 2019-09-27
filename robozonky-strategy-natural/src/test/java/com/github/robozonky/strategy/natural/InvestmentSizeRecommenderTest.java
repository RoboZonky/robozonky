/*
 * Copyright 2019 The RoboZonky Project
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

import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.api.remote.entities.Restrictions;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.test.mock.MockLoanBuilder;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class InvestmentSizeRecommenderTest {

    private static final int MAXIMUM_SHARE = 1;
    private static final int MAXIMUM_INVESTMENT = 1000;

    private static Loan mockLoan(final int amount) {
        return new MockLoanBuilder()
                .setRating(Rating.A)
                .setAmount(amount)
                .setNonReservedRemainingInvestment(amount)
                .build();
    }

    private static ParsedStrategy getStrategy() {
        // no filters, as the SUT doesn't do filtering; no portfolio, as that is not used either
        final DefaultValues defaults = new DefaultValues(DefaultPortfolio.EMPTY);
        defaults.setInvestmentShare(new DefaultInvestmentShare(MAXIMUM_SHARE));
        final InvestmentSize target = new InvestmentSize(MAXIMUM_INVESTMENT);
        return new ParsedStrategy(defaults, Collections.emptyList(),
                                  Collections.singletonMap(mockLoan(0).getRating(), target));
    }

    @Test
    void withSpecificRating() {
        final Restrictions restrictions = new Restrictions();
        final ParsedStrategy s = getStrategy();
        final InvestmentSizeRecommender r = new InvestmentSizeRecommender(s);
        // with unlimited balance, make maximum possible recommendation
        final Loan loan = mockLoan(50_000);
        final int actualInvestment = r.apply(loan, restrictions);
        // at most 1 percent of 50000, rounded down to nearest increment of 200
        assertThat(actualInvestment).isEqualTo(400);
    }

    @Test
    void byDefault() {
        final Restrictions restrictions = new Restrictions();
        final ParsedStrategy s = getStrategy();
        final Loan l = mockLoan(100_000);
        final InvestmentSizeRecommender r = new InvestmentSizeRecommender(s);
        // make maximum possible recommendation
        final int actualInvestment = r.apply(l, restrictions);
        assertThat(actualInvestment).isEqualTo(MAXIMUM_INVESTMENT);
    }

    @Test
    void nothingMoreToInvest() {
        final Restrictions restrictions = new Restrictions();
        final ParsedStrategy s = getStrategy();
        final Loan l = mockLoan(restrictions.getMinimumInvestmentAmount() - 1);
        final InvestmentSizeRecommender r = new InvestmentSizeRecommender(s);
        // with unlimited balance, make maximum possible recommendation
        final int actualInvestment = r.apply(l, restrictions);
        assertThat(actualInvestment).isEqualTo(0);
    }

    @Test
    void minimumOverRemaining() {
        final Restrictions restrictions = new Restrictions();
        final int minimumInvestment = restrictions.getMinimumInvestmentAmount();
        final Loan l = mockLoan(minimumInvestment - 1);
        final ParsedStrategy s = mock(ParsedStrategy.class);
        when(s.getMinimumInvestmentSizeInCzk(eq(l.getRating()))).thenReturn(minimumInvestment);
        when(s.getMaximumInvestmentSizeInCzk(eq(l.getRating())))
                .thenReturn(minimumInvestment * 2);
        when(s.getMaximumInvestmentShareInPercent()).thenReturn(100);
        final InvestmentSizeRecommender r = new InvestmentSizeRecommender(s);
        assertThat(r.apply(l, new Restrictions())).isEqualTo(0);
    }

    @Test
    void recommendationRoundedUnderMinimum() {
        final Restrictions restrictions = new Restrictions();
        final int minimumInvestment = restrictions.getMinimumInvestmentAmount();
        final Loan l = mockLoan(minimumInvestment - 1);
        final ParsedStrategy s = mock(ParsedStrategy.class);
        // next line will cause the recommendation to be rounded to 800, which will be below the minimum investment
        when(s.getMinimumInvestmentSizeInCzk(eq(l.getRating()))).thenReturn(
                minimumInvestment - 1);
        when(s.getMaximumInvestmentSizeInCzk(eq(l.getRating())))
                .thenReturn(minimumInvestment);
        when(s.getMaximumInvestmentShareInPercent()).thenReturn(100);
        final InvestmentSizeRecommender r = new InvestmentSizeRecommender(s);
        assertThat(r.apply(l, new Restrictions())).isEqualTo(0);
    }
}
