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

import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.internal.api.Defaults;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public class InvestmentSizeRecommenderTest {

    private static final int MAXIMUM_SHARE = 1;
    private static final int MAXIMUM_INVESTMENT = 1000;
    private static final Loan LOAN = new Loan(1, 50000);

    private static ParsedStrategy getStrategy() {
        // no filters, as the SUT doesn't do filtering; no portfolio, as that is not used either
        final DefaultValues defaults = new DefaultValues(DefaultPortfolio.EMPTY);
        defaults.setInvestmentShare(new DefaultInvestmentShare(MAXIMUM_SHARE));
        final InvestmentSize target = new InvestmentSize(MAXIMUM_INVESTMENT);
        return new ParsedStrategy(defaults, Collections.emptyList(),
                                  Collections.singletonMap(LOAN.getRating(), target));
    }

    @Test
    public void withSpecificRating() {
        final ParsedStrategy s = getStrategy();
        final InvestmentSizeRecommender r = new InvestmentSizeRecommender(s, Defaults.MAXIMUM_INVESTMENT_IN_CZK);
        // with unlimited balance, make maximum possible recommendation
        final int actualInvestment = r.apply(LOAN, Integer.MAX_VALUE);
        // at most 1 percent of 50000, rounded down to nearest increment of 200
        Assertions.assertThat(actualInvestment).isEqualTo(400);

        // with balance less that the recommendation, recommend less than 400 but more than 0; 200 only possible
        final int investmentOnLowBalance = r.apply(LOAN, actualInvestment - 1);
        Assertions.assertThat(investmentOnLowBalance)
                .isEqualTo(actualInvestment - Defaults.MINIMUM_INVESTMENT_INCREMENT_IN_CZK);

        // with no balance, don't make a recommendation
        final int investmentOnNoBalance = r.apply(LOAN, investmentOnLowBalance - 1);
        Assertions.assertThat(investmentOnNoBalance).isEqualTo(0);
    }

    @Test
    public void byDefault() {
        final ParsedStrategy s = getStrategy();
        final Loan l = Mockito.spy(LOAN);
        Mockito.doReturn(Rating.A).when(l).getRating();
        Mockito.doReturn(100000.0).when(l).getAmount();
        final InvestmentSizeRecommender r = new InvestmentSizeRecommender(s, Defaults.MAXIMUM_INVESTMENT_IN_CZK);
        // with unlimited balance, make maximum possible recommendation
        final int actualInvestment = r.apply(l, Integer.MAX_VALUE);
        Assertions.assertThat(actualInvestment).isEqualTo(MAXIMUM_INVESTMENT);

        // with balance less that the recommendation, go just under maximum
        final int investmentOnLowBalance = r.apply(l, actualInvestment - 1);
        Assertions.assertThat(investmentOnLowBalance)
                .isEqualTo(MAXIMUM_INVESTMENT - Defaults.MINIMUM_INVESTMENT_INCREMENT_IN_CZK);
    }

    @Test
    public void nothingMoreToInvest() {
        final ParsedStrategy s = getStrategy();
        final Loan l = new Loan(1, Defaults.MINIMUM_INVESTMENT_IN_CZK - 1);
        final InvestmentSizeRecommender r = new InvestmentSizeRecommender(s, Defaults.MAXIMUM_INVESTMENT_IN_CZK);
        // with unlimited balance, make maximum possible recommendation
        final int actualInvestment = r.apply(l, Integer.MAX_VALUE);
        Assertions.assertThat(actualInvestment).isEqualTo(0);
    }

    @Test
    public void minimumOverBalance() {
        final Loan l = new Loan(1, 100_000);
        final ParsedStrategy s = Mockito.mock(ParsedStrategy.class);
        final int minimumInvestment = 1000;
        Mockito.when(s.getMinimumInvestmentSizeInCzk(ArgumentMatchers.eq(l.getRating()))).thenReturn(minimumInvestment);
        Mockito.when(s.getMaximumInvestmentSizeInCzk(ArgumentMatchers.eq(l.getRating())))
                .thenReturn(minimumInvestment * 2);
        Mockito.when(s.getMaximumInvestmentShareInPercent()).thenReturn(100);
        final BiFunction<Loan, Integer, Integer> r = new InvestmentSizeRecommender(s, minimumInvestment * 10);
        Assertions.assertThat(r.apply(l, minimumInvestment - 1)).isEqualTo(0);
    }

    @Test
    public void minimumOverRemaining() {
        final int minimumInvestment = 1000;
        final Loan l = new Loan(1, minimumInvestment - 1);
        final ParsedStrategy s = Mockito.mock(ParsedStrategy.class);
        Mockito.when(s.getMinimumInvestmentSizeInCzk(ArgumentMatchers.eq(l.getRating()))).thenReturn(minimumInvestment);
        Mockito.when(s.getMaximumInvestmentSizeInCzk(ArgumentMatchers.eq(l.getRating())))
                .thenReturn(minimumInvestment * 2);
        Mockito.when(s.getMaximumInvestmentShareInPercent()).thenReturn(100);
        final BiFunction<Loan, Integer, Integer> r = new InvestmentSizeRecommender(s, minimumInvestment * 10);
        Assertions.assertThat(r.apply(l, minimumInvestment * 2)).isEqualTo(0);
    }

    @Test
    public void recommendationRoundedUnderMinimum() {
        final int minimumInvestment = 1000;
        final Loan l = new Loan(1, minimumInvestment - 1);
        final ParsedStrategy s = Mockito.mock(ParsedStrategy.class);
        // next line will cause the recommendation to be rounded to 800, which will be below the minimum investment
        Mockito.when(s.getMinimumInvestmentSizeInCzk(ArgumentMatchers.eq(l.getRating()))).thenReturn(
                minimumInvestment - 1);
        Mockito.when(s.getMaximumInvestmentSizeInCzk(ArgumentMatchers.eq(l.getRating())))
                .thenReturn(minimumInvestment);
        Mockito.when(s.getMaximumInvestmentShareInPercent()).thenReturn(100);
        final BiFunction<Loan, Integer, Integer> r = new InvestmentSizeRecommender(s, minimumInvestment * 10);
        Assertions.assertThat(r.apply(l, minimumInvestment * 2)).isEqualTo(0);
    }
}
