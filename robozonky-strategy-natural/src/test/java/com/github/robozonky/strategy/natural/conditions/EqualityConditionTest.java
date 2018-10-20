/*
 * Copyright 2018 The RoboZonky Project
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

package com.github.robozonky.strategy.natural.conditions;

import java.util.stream.Stream;

import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.LoanDescriptor;
import com.github.robozonky.strategy.natural.Wrapper;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.junit.jupiter.api.DynamicContainer.dynamicContainer;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

class EqualityConditionTest {

    private static Stream<DynamicTest> betterThan(final Rating current) {
        return Stream.of(Rating.values())
                .filter(r -> r.compareTo(current) < 0)
                .map(r -> dynamicTest(r.getCode(), () -> testBetterThan(current, r)));
    }

    private static Stream<DynamicTest> worseThan(final Rating current) {
        return Stream.of(Rating.values())
                .filter(r -> r.compareTo(current) > 0)
                .map(r -> dynamicTest(r.getCode(), () -> testWorseThan(current, r)));
    }

    private static Wrapper<?> mockLoan(final Rating r) {
        final Loan loan = Loan.custom().setRating(r).build();
        return Wrapper.wrap(new LoanDescriptor(loan));
    }

    private static void testBetterThan(final Rating current, final Rating r) {
        final MarketplaceFilterCondition c = new LoanRatingBetterCondition(current);
        assertSoftly(softly -> {
            softly.assertThat(c.test(mockLoan(current))).isFalse();
            softly.assertThat(c.test(mockLoan(r))).isTrue();
        });
    }

    private static void testWorseThan(final Rating current, final Rating r) {
        final MarketplaceFilterCondition c = new LoanRatingWorseCondition(current);
        assertSoftly(softly -> {
            softly.assertThat(c.test(mockLoan(current))).isFalse();
            softly.assertThat(c.test(mockLoan(r))).isTrue();
        });
    }

    @TestFactory
    Stream<DynamicNode> ratings() {
        return Stream.of(Rating.values())
                .map(rating -> dynamicContainer(rating.getCode(), Stream.of(
                        dynamicContainer("is better than", betterThan(rating)),
                        dynamicContainer("is worse than", worseThan(rating))
                )));
    }
}
