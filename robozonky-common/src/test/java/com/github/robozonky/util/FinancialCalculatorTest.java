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

package com.github.robozonky.util;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.github.robozonky.api.remote.entities.Investment;
import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.PortfolioOverview;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mockito;

@RunWith(Parameterized.class)
public class FinancialCalculatorTest {

    @Parameterized.Parameters(name = "{1},{0}")
    public static Collection<Object[]> parameters() {
        final Collection<Integer> thresholds = Arrays.asList(150_000, 200_000, 500_000, 1_000_000);
        final Collection<Rating> ratings = Arrays.asList(Rating.values());
        final Collection<Object[]> result = new ArrayList<>();
        for (final int threshold : thresholds) {
            for (final Rating rating : ratings) {
                result.add(new Object[]{threshold, rating});
            }
        }
        return result;
    }

    @Parameterized.Parameter
    public int threshold;
    @Parameterized.Parameter(1)
    public Rating rating;

    private static PortfolioOverview getPortfolioOverview(final int total) {
        final int amountPerRating = total / Rating.values().length;
        final Map<Rating, Integer> shares = Arrays.stream(Rating.values())
                .collect(Collectors.toMap(Function.identity(), r -> amountPerRating));
        return new PortfolioOverview(BigDecimal.valueOf(1000), shares);
    }

    @Test
    public void fees() {
        final Loan l = Mockito.spy(new Loan(1, 100000));
        Mockito.when(l.getRating()).thenReturn(rating);
        Mockito.when(l.getTermInMonths()).thenReturn(84);
        final Investment i = new Investment(l, 1000);
        final BigDecimal before = FinancialCalculator.estimateFeeRate(i, getPortfolioOverview(threshold - 1));
        final BigDecimal after = FinancialCalculator.estimateFeeRate(i, getPortfolioOverview(threshold));
        Assertions.assertThat(after).isLessThan(before);
    }

    @Test
    public void expectedInterestRate() {
        final Loan l = Mockito.spy(new Loan(1, 100000));
        Mockito.when(l.getRating()).thenReturn(rating);
        Mockito.when(l.getTermInMonths()).thenReturn(84);
        final Investment i = Mockito.spy(new Investment(l, 1000));
        Mockito.when(i.getRemainingMonths()).thenReturn(50);
        final BigDecimal before = FinancialCalculator.expectedInterestRateAfterFees(i, getPortfolioOverview(
                threshold - 1));
        final BigDecimal after = FinancialCalculator.expectedInterestRateAfterFees(i, getPortfolioOverview(threshold));
        Assertions.assertThat(after).isGreaterThan(before);
    }

    @Test
    public void actualInterestRate() {
        final Loan l = Mockito.spy(new Loan(1, 100000));
        Mockito.when(l.getRating()).thenReturn(rating);
        Mockito.when(l.getTermInMonths()).thenReturn(84);
        final Investment i = Mockito.spy(new Investment(l, 1000));
        Mockito.when(i.getPaidInterest()).thenReturn(BigDecimal.valueOf(500));
        Mockito.when(i.getPaidPenalty()).thenReturn(BigDecimal.ONE);
        Mockito.when(i.getRemainingMonths()).thenReturn(10);
        final BigDecimal before = FinancialCalculator.actualInterestRateAfterFees(i,
                                                                                  getPortfolioOverview(threshold - 1),
                                                                                  10);
        final BigDecimal after = FinancialCalculator.actualInterestRateAfterFees(i, getPortfolioOverview(threshold),
                                                                                 10);
        Assertions.assertThat(after).isGreaterThan(before);
    }

    @Test
    public void actualInterest() {
        final Loan l = Mockito.spy(new Loan(1, 100000));
        Mockito.when(l.getRating()).thenReturn(rating);
        Mockito.when(l.getTermInMonths()).thenReturn(84);
        final Investment i = Mockito.spy(new Investment(l, 1000));
        Mockito.when(i.getPaidInterest()).thenReturn(BigDecimal.valueOf(500));
        Mockito.when(i.getPaidPenalty()).thenReturn(BigDecimal.ONE);
        Mockito.when(i.getRemainingMonths()).thenReturn(50);
        final BigDecimal before = FinancialCalculator.actualInterestAfterFees(i, getPortfolioOverview(threshold - 1));
        final BigDecimal after = FinancialCalculator.actualInterestAfterFees(i, getPortfolioOverview(threshold));
        Assertions.assertThat(after).isGreaterThan(before);
    }
}
