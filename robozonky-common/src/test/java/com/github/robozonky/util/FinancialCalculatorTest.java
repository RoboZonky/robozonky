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
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.robozonky.api.remote.entities.MyInvestment;
import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.PortfolioOverview;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.DynamicContainer.dynamicContainer;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

class FinancialCalculatorTest {

    private static PortfolioOverview getPortfolioOverview(final int total) {
        final int amountPerRating = total / Rating.values().length;
        final Map<Rating, BigDecimal> shares = Arrays.stream(Rating.values())
                .collect(Collectors.toMap(Function.identity(), r -> BigDecimal.valueOf(amountPerRating)));
        return PortfolioOverview.calculate(BigDecimal.valueOf(1000), shares);
    }

    private static Loan mockLoan(final Rating rating) {
        final MyInvestment investment = Mockito.mock(MyInvestment.class);
        Mockito.when(investment.getTimeCreated()).thenReturn(OffsetDateTime.now());
        return Loan.custom()
                .setId(1)
                .setAmount(100_000)
                .setMyInvestment(investment)
                .setRating(rating)
                .setTermInMonths(84)
                .build();
    }

    private static void fees(final Rating rating, final int threshold) {
        final Investment i = Investment.fresh(mockLoan(rating), 1000);
        final BigDecimal before = FinancialCalculator.estimateFeeRate(i, getPortfolioOverview(threshold - 1));
        final BigDecimal after = FinancialCalculator.estimateFeeRate(i, getPortfolioOverview(threshold));
        assertThat(after).isLessThan(before);
    }

    private static void expectedInterest(final Rating rating, final int threshold) {
        final Investment i = Investment.fresh(mockLoan(rating), 1000)
                .setInterestRate(BigDecimal.TEN)
                .setRemainingMonths(50).build();
        final BigDecimal before = FinancialCalculator.expectedInterestAfterFees(i, getPortfolioOverview(threshold - 1));
        final BigDecimal after = FinancialCalculator.expectedInterestAfterFees(i, getPortfolioOverview(threshold));
        assertThat(after).isGreaterThan(before);
    }

    private static void expectedInterestRate(final Rating rating, final int threshold) {
        final Investment i = Investment.fresh(mockLoan(rating), 1000)
                .setInterestRate(BigDecimal.TEN)
                .setRemainingMonths(50);
        final BigDecimal before = FinancialCalculator.expectedInterestRateAfterFees(i, getPortfolioOverview(
                threshold - 1));
        final BigDecimal after = FinancialCalculator.expectedInterestRateAfterFees(i, getPortfolioOverview(threshold));
        assertThat(after).isGreaterThan(before);
    }

    private static void actualInterest(final Rating rating, final int threshold) {
        final Investment i = Investment.fresh(mockLoan(rating), 1000)
                .setInterestRate(BigDecimal.TEN)
                .setPaidInterest(BigDecimal.valueOf(500))
                .setPaidPenalty(BigDecimal.ONE)
                .setCurrentTerm(50)
                .setSmpFee(BigDecimal.ONE);
        final BigDecimal before = FinancialCalculator.actualInterestAfterFees(i, getPortfolioOverview(threshold - 1));
        final BigDecimal after = FinancialCalculator.actualInterestAfterFees(i, getPortfolioOverview(threshold));
        assertThat(after).isGreaterThan(before);
        final BigDecimal afterSmpFee = FinancialCalculator.actualInterestAfterFees(i,
                                                                                   getPortfolioOverview(threshold),
                                                                                   true);
        assertThat(after).isGreaterThan(afterSmpFee);
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
                dynamicTest("has proper expected interest", () -> expectedInterest(rating, threshold)),
                dynamicTest("has proper expected interest rate", () -> expectedInterestRate(rating, threshold)),
                dynamicTest("has proper actual interest", () -> actualInterest(rating, threshold))
        );
    }

    @TestFactory
    Stream<DynamicNode> ratings() {
        return Stream.of(Rating.values())
                .map(rating -> dynamicContainer(rating.getCode(), getTestsPerRating(rating)));
    }
}
