/*
 * Copyright 2017 The RoboZonky Project
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

import java.util.Collections;
import java.util.function.BiFunction;

import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.internal.api.Defaults;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class InvestmentSizeRecommenderTest {

    private static final int MAXIMUM_SHARE = 1;
    private static final int MAXIMUM_INVESTMENT = 1000;

    private static Loan mockLoan(final int amount) {
        return Loan.custom()
                .setId(1)
                .setRating(Rating.A)
                .setAmount(amount)
                .setRemainingInvestment(amount)
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
        final ParsedStrategy s = getStrategy();
        final InvestmentSizeRecommender r = new InvestmentSizeRecommender(s, Defaults.MAXIMUM_INVESTMENT_IN_CZK);
        // with unlimited balance, make maximum possible recommendation
        final Loan loan = mockLoan(50_000);
        final int actualInvestment = r.apply(loan, Integer.MAX_VALUE);
        // at most 1 percent of 50000, rounded down to nearest increment of 200
        assertThat(actualInvestment).isEqualTo(400);

        // with balance less that the recommendation, recommend less than 400 but more than 0; 200 only possible
        final int investmentOnLowBalance = r.apply(loan, actualInvestment - 1);
        assertThat(investmentOnLowBalance)
                .isEqualTo(actualInvestment - Defaults.MINIMUM_INVESTMENT_INCREMENT_IN_CZK);
        // with no balance, don't make a recommendation
        final int investmentOnNoBalance = r.apply(loan, investmentOnLowBalance - 1);
        assertThat(investmentOnNoBalance).isEqualTo(0);
    }

    @Test
    void byDefault() {
        final ParsedStrategy s = getStrategy();
        final Loan l = mockLoan(100_000);
        final InvestmentSizeRecommender r = new InvestmentSizeRecommender(s, Defaults.MAXIMUM_INVESTMENT_IN_CZK);
        // with unlimited balance, make maximum possible recommendation
        final int actualInvestment = r.apply(l, Integer.MAX_VALUE);
        assertThat(actualInvestment).isEqualTo(MAXIMUM_INVESTMENT);
        // with balance less that the recommendation, go just under maximum
        final int investmentOnLowBalance = r.apply(l, actualInvestment - 1);
        assertThat(investmentOnLowBalance)
                .isEqualTo(MAXIMUM_INVESTMENT - Defaults.MINIMUM_INVESTMENT_INCREMENT_IN_CZK);
    }

    @Test
    void nothingMoreToInvest() {
        final ParsedStrategy s = getStrategy();
        final Loan l = mockLoan(Defaults.MINIMUM_INVESTMENT_IN_CZK - 1);
        final InvestmentSizeRecommender r = new InvestmentSizeRecommender(s, Defaults.MAXIMUM_INVESTMENT_IN_CZK);
        // with unlimited balance, make maximum possible recommendation
        final int actualInvestment = r.apply(l, Integer.MAX_VALUE);
        assertThat(actualInvestment).isEqualTo(0);
    }

    @Test
    void minimumOverBalance() {
        final Loan l = mockLoan(100_000);
        final ParsedStrategy s = mock(ParsedStrategy.class);
        final int minimumInvestment = 1000;
        when(s.getMinimumInvestmentSizeInCzk(eq(l.getRating()))).thenReturn(minimumInvestment);
        when(s.getMaximumInvestmentSizeInCzk(eq(l.getRating())))
                .thenReturn(minimumInvestment * 2);
        when(s.getMaximumInvestmentShareInPercent()).thenReturn(100);
        final BiFunction<Loan, Integer, Integer> r = new InvestmentSizeRecommender(s, minimumInvestment * 10);
        assertThat(r.apply(l, minimumInvestment - 1)).isEqualTo(0);
    }

    @Test
    void minimumOverRemaining() {
        final int minimumInvestment = 1000;
        final Loan l = mockLoan(minimumInvestment - 1);
        final ParsedStrategy s = mock(ParsedStrategy.class);
        when(s.getMinimumInvestmentSizeInCzk(eq(l.getRating()))).thenReturn(minimumInvestment);
        when(s.getMaximumInvestmentSizeInCzk(eq(l.getRating())))
                .thenReturn(minimumInvestment * 2);
        when(s.getMaximumInvestmentShareInPercent()).thenReturn(100);
        final BiFunction<Loan, Integer, Integer> r = new InvestmentSizeRecommender(s, minimumInvestment * 10);
        assertThat(r.apply(l, minimumInvestment * 2)).isEqualTo(0);
    }

    @Test
    void recommendationRoundedUnderMinimum() {
        final int minimumInvestment = 1000;
        final Loan l = mockLoan(minimumInvestment - 1);
        final ParsedStrategy s = mock(ParsedStrategy.class);
        // next line will cause the recommendation to be rounded to 800, which will be below the minimum investment
        when(s.getMinimumInvestmentSizeInCzk(eq(l.getRating()))).thenReturn(
                minimumInvestment - 1);
        when(s.getMaximumInvestmentSizeInCzk(eq(l.getRating())))
                .thenReturn(minimumInvestment);
        when(s.getMaximumInvestmentShareInPercent()).thenReturn(100);
        final BiFunction<Loan, Integer, Integer> r = new InvestmentSizeRecommender(s, minimumInvestment * 10);
        assertThat(r.apply(l, minimumInvestment * 2)).isEqualTo(0);
    }
}
