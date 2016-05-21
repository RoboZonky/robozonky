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
package com.github.triceo.robozonky.strategy;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;

import com.github.triceo.robozonky.remote.Loan;
import com.github.triceo.robozonky.remote.Rating;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.Mockito;

public class InvestmentStategyTest {

    private static final Rating RATING_A = Rating.A;
    private static final Rating RATING_B = Rating.B;
    private static final int TESTED_TERM_LENGTH = 2;
    private static final int MAXIMUM_LOAN_INVESTMENT = 10000;
    private static final BigDecimal MAXIMUM_LOAN_SHARE_A = BigDecimal.valueOf(0.01);
    private static final BigDecimal TARGET_SHARE_A = BigDecimal.valueOf(0.2);
    private static final StrategyPerRating STRATEGY_A = new StrategyPerRating(InvestmentStategyTest.RATING_A,
            InvestmentStategyTest.TARGET_SHARE_A, InvestmentStategyTest.TESTED_TERM_LENGTH - 1, -1,
            InvestmentStategyTest.MAXIMUM_LOAN_INVESTMENT, InvestmentStategyTest.MAXIMUM_LOAN_SHARE_A, true);

    private static final BigDecimal MAXIMUM_LOAN_SHARE_B = BigDecimal.valueOf(0.001);
    private static final BigDecimal TARGET_SHARE_B = InvestmentStategyTest.TARGET_SHARE_A.add(BigDecimal.valueOf(0.5));
    private static final StrategyPerRating STRATEGY_B = new StrategyPerRating(InvestmentStategyTest.RATING_B,
            InvestmentStategyTest.TARGET_SHARE_B, InvestmentStategyTest.TESTED_TERM_LENGTH - 1,
            InvestmentStategyTest.TESTED_TERM_LENGTH + 1, InvestmentStategyTest.MAXIMUM_LOAN_INVESTMENT,
            InvestmentStategyTest.MAXIMUM_LOAN_SHARE_B, false);

    private final InvestmentStrategy overallStrategy;

    public InvestmentStategyTest() {
        final Map<Rating, StrategyPerRating> strategies = new EnumMap<>(Rating.class);
        strategies.put(Rating.AAAAA, Mockito.mock(StrategyPerRating.class));
        strategies.put(Rating.AAAA, Mockito.mock(StrategyPerRating.class));
        strategies.put(Rating.AAA, Mockito.mock(StrategyPerRating.class));
        strategies.put(Rating.AA, Mockito.mock(StrategyPerRating.class));
        strategies.put(InvestmentStategyTest.RATING_A, InvestmentStategyTest.STRATEGY_A);
        strategies.put(InvestmentStategyTest.RATING_B, InvestmentStategyTest.STRATEGY_B);
        strategies.put(Rating.C, Mockito.mock(StrategyPerRating.class));
        strategies.put(Rating.D, Mockito.mock(StrategyPerRating.class));
        overallStrategy = new InvestmentStrategy(strategies);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorEnsuringAllStrategiesPresent() {
        final Map<Rating, StrategyPerRating> strategies = new EnumMap<>(Rating.class);
        strategies.put(InvestmentStategyTest.RATING_A, InvestmentStategyTest.STRATEGY_A);
        strategies.put(InvestmentStategyTest.RATING_B, InvestmentStategyTest.STRATEGY_B);
        new InvestmentStrategy(strategies);
    }

    @Test
    public void properlySwitchingShares() {
        Assertions.assertThat(overallStrategy.getTargetShare(InvestmentStategyTest.RATING_A))
                .isEqualTo(InvestmentStategyTest.TARGET_SHARE_A);
        Assertions.assertThat(overallStrategy.getTargetShare(InvestmentStategyTest.RATING_B))
                .isEqualTo(InvestmentStategyTest.TARGET_SHARE_B);
    }

    @Test
    public void properlySwitchingPreferences() {
        Assertions.assertThat(overallStrategy.prefersLongerTerms(InvestmentStategyTest.RATING_A)).isTrue();
        Assertions.assertThat(overallStrategy.prefersLongerTerms(InvestmentStategyTest.RATING_B)).isFalse();
    }

    @Test
    public void properlySwitchingAcceptability() {
        final Loan mockLoan = Mockito.mock(Loan.class);
        Mockito.when(mockLoan.getTermInMonths()).thenReturn(InvestmentStategyTest.TESTED_TERM_LENGTH + 10);

        Mockito.when(mockLoan.getRating()).thenReturn(InvestmentStategyTest.RATING_A);
        Assertions.assertThat(overallStrategy.isAcceptable(mockLoan)).isTrue();

        Mockito.when(mockLoan.getRating()).thenReturn(InvestmentStategyTest.RATING_B);
        Assertions.assertThat(overallStrategy.isAcceptable(mockLoan)).isFalse();
    }

    @Test
    public void properlyRecommendingInvestmentSize() {
        final BigDecimal loanAmount = BigDecimal.valueOf(100000.0);
        final Loan mockLoan = Mockito.mock(Loan.class);
        Mockito.when(mockLoan.getRating()).thenReturn(InvestmentStategyTest.RATING_A);
        Mockito.when(mockLoan.getRemainingInvestment()).thenReturn(loanAmount.doubleValue());
        Mockito.when(mockLoan.getAmount()).thenReturn(loanAmount.doubleValue());

        // with unlimited balance
        final int maxInvestment = Math.min(loanAmount.multiply(InvestmentStategyTest.MAXIMUM_LOAN_SHARE_A).intValue(),
                InvestmentStategyTest.MAXIMUM_LOAN_INVESTMENT);
        final int actualInvestment = overallStrategy.recommendInvestmentAmount(mockLoan,
                BigDecimal.valueOf(Integer.MAX_VALUE));
        Assertions.assertThat(actualInvestment).isLessThanOrEqualTo(maxInvestment);

        // with balance just a little less than the recommended investment
        final int adjustedForBalance =
                overallStrategy.recommendInvestmentAmount(mockLoan, BigDecimal.valueOf(actualInvestment - 1));
        Assertions.assertThat(adjustedForBalance).isLessThanOrEqualTo(actualInvestment);

        // and make sure the recommendation is 200 times X, where X is a positive integer
        final BigDecimal remainder = BigDecimal.valueOf(adjustedForBalance)
                .remainder(BigDecimal.valueOf(InvestmentStrategy.MINIMAL_INVESTMENT_INCREMENT));
        Assertions.assertThat(remainder.intValue()).isEqualTo(0);
    }

}
