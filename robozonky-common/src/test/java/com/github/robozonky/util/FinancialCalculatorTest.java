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
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.robozonky.api.remote.entities.Investment;
import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.PortfolioOverview;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.mockito.Mockito;

import static org.junit.jupiter.api.DynamicContainer.dynamicContainer;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

class FinancialCalculatorTest {

    private static PortfolioOverview getPortfolioOverview(final int total) {
        final int amountPerRating = total / Rating.values().length;
        final Map<Rating, Integer> shares = Arrays.stream(Rating.values())
                .collect(Collectors.toMap(Function.identity(), r -> amountPerRating));
        return PortfolioOverview.calculate(BigDecimal.valueOf(1000), shares);
    }

    private static void fees(final Rating rating, final int threshold) {
        final Loan l = Mockito.spy(new Loan(1, 100000));
        Mockito.when(l.getRating()).thenReturn(rating);
        Mockito.when(l.getTermInMonths()).thenReturn(84);
        final Investment i = new Investment(l, 1000);
        final BigDecimal before = FinancialCalculator.estimateFeeRate(i, getPortfolioOverview(threshold - 1));
        final BigDecimal after = FinancialCalculator.estimateFeeRate(i, getPortfolioOverview(threshold));
        Assertions.assertThat(after).isLessThan(before);
    }

    private static void expectedInterestRate(final Rating rating, final int threshold) {
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

    private static void actualInterestRate(final Rating rating, final int threshold) {
        final Loan l = Mockito.spy(new Loan(1, 100000));
        Mockito.when(l.getRating()).thenReturn(rating);
        Mockito.when(l.getTermInMonths()).thenReturn(84);
        final Investment i = Mockito.spy(new Investment(l, 1000));
        Mockito.when(i.getPaidInterest()).thenReturn(BigDecimal.valueOf(500));
        Mockito.when(i.getPaidPenalty()).thenReturn(BigDecimal.ONE);
        Mockito.when(i.getCurrentTerm()).thenReturn(10);
        Mockito.when(i.getSmpFee()).thenReturn(BigDecimal.ONE);
        final BigDecimal wrong = FinancialCalculator.actualInterestRateAfterFees(i,
                                                                                 getPortfolioOverview(threshold - 1),
                                                                                 10);
        Mockito.when(i.getPurchasePrice()).thenReturn(BigDecimal.valueOf(1200)); // override the amount
        final BigDecimal before = FinancialCalculator.actualInterestRateAfterFees(i,
                                                                                  getPortfolioOverview(threshold - 1),
                                                                                  10);
        Assertions.assertThat(wrong).isGreaterThan(before);
        final BigDecimal after = FinancialCalculator.actualInterestRateAfterFees(i, getPortfolioOverview(threshold),
                                                                                 10);
        Assertions.assertThat(after).isGreaterThan(before);
        final BigDecimal afterSmpFee = FinancialCalculator.actualInterestRateAfterFees(i,
                                                                                       getPortfolioOverview(threshold),
                                                                                       10,
                                                                                       true);
        Assertions.assertThat(after).isGreaterThan(afterSmpFee);
    }

    private static void actualInterest(final Rating rating, final int threshold) {
        final Loan l = Mockito.spy(new Loan(1, 100000));
        Mockito.when(l.getRating()).thenReturn(rating);
        Mockito.when(l.getTermInMonths()).thenReturn(84);
        final Investment i = Mockito.spy(new Investment(l, 1000));
        Mockito.when(i.getPaidInterest()).thenReturn(BigDecimal.valueOf(500));
        Mockito.when(i.getPaidPenalty()).thenReturn(BigDecimal.ONE);
        Mockito.when(i.getCurrentTerm()).thenReturn(50);
        Mockito.when(i.getSmpFee()).thenReturn(BigDecimal.ONE);
        final BigDecimal before = FinancialCalculator.actualInterestAfterFees(i, getPortfolioOverview(threshold - 1));
        final BigDecimal after = FinancialCalculator.actualInterestAfterFees(i, getPortfolioOverview(threshold));
        Assertions.assertThat(after).isGreaterThan(before);
        final BigDecimal afterSmpFee = FinancialCalculator.actualInterestAfterFees(i,
                                                                                   getPortfolioOverview(threshold),
                                                                                   true);
        Assertions.assertThat(after).isGreaterThan(afterSmpFee);
    }

    private static Stream<DynamicNode> getTestsPerRating(final Rating rating) {
        final Collection<Integer> thresholds = Arrays.asList(150_000, 200_000, 500_000, 1_000_000);
        return thresholds.stream()
                .map(threshold -> dynamicContainer(" with portfolio size " + threshold,
                                                   getTestsPerThreshold(rating, threshold)));
    }

    private static Stream<DynamicTest> getTestsPerThreshold(final Rating rating, final int threshold) {
        return Stream.of(
                dynamicTest("has proper fees", () -> fees(rating, threshold)),
                dynamicTest("has proper expected interest rate", () -> expectedInterestRate(rating, threshold)),
                dynamicTest("has proper actual interest rate", () -> actualInterestRate(rating, threshold)),
                dynamicTest("has proper actual interest", () -> actualInterest(rating, threshold))
        );
    }

    @TestFactory
    public Stream<DynamicNode> ratings() {
        return Stream.of(Rating.values())
                .map(rating -> dynamicContainer(rating.getCode(), getTestsPerRating(rating)));
    }
}
