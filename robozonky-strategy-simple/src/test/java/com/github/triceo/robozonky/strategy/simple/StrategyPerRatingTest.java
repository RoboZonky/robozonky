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

package com.github.triceo.robozonky.strategy.simple;

import java.math.BigDecimal;

import com.github.triceo.robozonky.remote.Loan;
import com.github.triceo.robozonky.remote.Rating;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.Mockito;

public class StrategyPerRatingTest {

    private static final Rating DIFFERENT_RATING = Rating.B;
    private static final int TESTED_TERM_LENGTH = 2;
    private static final int MAXIMUM_LOAN_INVESTMENT = 2000;
    private static final int MINIMUM_ASK = StrategyPerRatingTest.MAXIMUM_LOAN_INVESTMENT / 10;
    private static final int MAXIMUM_ASK = StrategyPerRatingTest.MAXIMUM_LOAN_INVESTMENT * 10;
    private static final BigDecimal MAXIMUM_LOAN_SHARE = BigDecimal.valueOf(0.01);
    private static final StrategyPerRating STRATEGY = new StrategyPerRating(Rating.A, BigDecimal.valueOf(0.15), StrategyPerRatingTest.TESTED_TERM_LENGTH - 1, -1, StrategyPerRatingTest.MAXIMUM_LOAN_INVESTMENT, StrategyPerRatingTest.MAXIMUM_LOAN_SHARE, StrategyPerRatingTest.MINIMUM_ASK, StrategyPerRatingTest.MAXIMUM_ASK, true
    );

    @Test(expected = IllegalArgumentException.class)
    public void loanIsNotAcceptableWithoutMatchingRating() {
        final Loan mockLoan = Mockito.mock(Loan.class);
        Mockito.when(mockLoan.getRating()).thenReturn(StrategyPerRatingTest.DIFFERENT_RATING);

        StrategyPerRatingTest.STRATEGY.isAcceptable(mockLoan);
    }

    @Test(expected = IllegalArgumentException.class)
    public void loanIsNotAcceptableWithoutMatchingRating2() {
        final Loan mockLoan = Mockito.mock(Loan.class);
        Mockito.when(mockLoan.getRating()).thenReturn(StrategyPerRatingTest.DIFFERENT_RATING);

        StrategyPerRatingTest.STRATEGY.recommendInvestmentAmount(mockLoan);
    }

    @Test
    public void loanIsNotAcceptableDueToTermMismatch() {
        final Loan mockLoan = Mockito.mock(Loan.class);
        Mockito.when(mockLoan.getAmount()).thenReturn((double)StrategyPerRatingTest.MAXIMUM_ASK);
        Mockito.when(mockLoan.getRating()).thenReturn(StrategyPerRatingTest.STRATEGY.getRating());

        // term length within limits
        Mockito.when(mockLoan.getTermInMonths()).thenReturn(StrategyPerRatingTest.TESTED_TERM_LENGTH);
        Assertions.assertThat(StrategyPerRatingTest.STRATEGY.isAcceptable(mockLoan)).isTrue();

        // less than minimal term length
        Mockito.when(mockLoan.getTermInMonths()).thenReturn(0);
        Assertions.assertThat(StrategyPerRatingTest.STRATEGY.isAcceptable(mockLoan)).isFalse();
    }

    @Test
    public void subminimalLoanTerm() {
        final Loan mockLoan = Mockito.mock(Loan.class);
        Mockito.when(mockLoan.getTermInMonths()).thenReturn(0);
        Mockito.when(mockLoan.getRating()).thenReturn(StrategyPerRatingTest.STRATEGY.getRating());

        final int recommendedInvestment = StrategyPerRatingTest.STRATEGY.recommendInvestmentAmount(mockLoan);
        Assertions.assertThat(recommendedInvestment).isEqualTo(0);
    }

    @Test
    public void maxLoanTerm() {
        final Loan mockLoan = Mockito.mock(Loan.class);
        Mockito.when(mockLoan.getAmount()).thenReturn(10000.0);
        Mockito.when(mockLoan.getTermInMonths()).thenReturn(Integer.MAX_VALUE);
        Mockito.when(mockLoan.getRating()).thenReturn(StrategyPerRatingTest.STRATEGY.getRating());

        final int recommendedInvestment = StrategyPerRatingTest.STRATEGY.recommendInvestmentAmount(mockLoan);
        Assertions.assertThat(recommendedInvestment).isGreaterThan(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidTermInConstructor() {
        new StrategyPerRating(Rating.A, BigDecimal.valueOf(0.15), -1, 0, StrategyPerRatingTest.MAXIMUM_LOAN_INVESTMENT, StrategyPerRatingTest.MAXIMUM_LOAN_SHARE, StrategyPerRatingTest.MINIMUM_ASK, StrategyPerRatingTest.MAXIMUM_ASK, true
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void reversedTermsInConstructor() {
        new StrategyPerRating(Rating.A, BigDecimal.valueOf(0.15), 2, 1, StrategyPerRatingTest.MAXIMUM_LOAN_INVESTMENT, StrategyPerRatingTest.MAXIMUM_LOAN_SHARE, StrategyPerRatingTest.MINIMUM_ASK, StrategyPerRatingTest.MAXIMUM_ASK, true
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void subzeroShareInConstructor() {
        new StrategyPerRating(Rating.A, BigDecimal.valueOf(0.15), StrategyPerRatingTest.TESTED_TERM_LENGTH - 1, -1, StrategyPerRatingTest.MAXIMUM_LOAN_INVESTMENT, BigDecimal.ONE.negate(), StrategyPerRatingTest.MINIMUM_ASK, StrategyPerRatingTest.MAXIMUM_ASK, true
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void overShareInConstructor() {
        new StrategyPerRating(Rating.A, BigDecimal.valueOf(0.15), StrategyPerRatingTest.TESTED_TERM_LENGTH - 1, -1, StrategyPerRatingTest.MAXIMUM_LOAN_INVESTMENT, BigDecimal.TEN, StrategyPerRatingTest.MINIMUM_ASK, StrategyPerRatingTest.MAXIMUM_ASK, true
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidAmountInConstructor() {
        new StrategyPerRating(Rating.A, BigDecimal.valueOf(0.15), StrategyPerRatingTest.TESTED_TERM_LENGTH - 1, -1, -1, StrategyPerRatingTest.MAXIMUM_LOAN_SHARE, StrategyPerRatingTest.MINIMUM_ASK, StrategyPerRatingTest.MAXIMUM_ASK, true
        );
    }

    @Test
    public void boundaryValuesInConstructor() { // should not throw any exceptions
        new StrategyPerRating(Rating.A, BigDecimal.valueOf(0.15), 0, -1, 0, BigDecimal.ONE, StrategyPerRatingTest.MINIMUM_ASK, StrategyPerRatingTest.MAXIMUM_ASK, true
        );
        new StrategyPerRating(Rating.A, BigDecimal.valueOf(0.15), 0, -1, 0, BigDecimal.ZERO, StrategyPerRatingTest.MINIMUM_ASK, StrategyPerRatingTest.MAXIMUM_ASK, true
        );
        new StrategyPerRating(Rating.A, BigDecimal.valueOf(0.15), 1, 1, 0, BigDecimal.ZERO, StrategyPerRatingTest.MINIMUM_ASK, StrategyPerRatingTest.MAXIMUM_ASK, true
        );
    }

}
