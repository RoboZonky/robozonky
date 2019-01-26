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

package com.github.robozonky.strategy.natural.conditions;

import java.util.Arrays;
import java.util.stream.Stream;

import com.github.robozonky.strategy.natural.Wrapper;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.DynamicContainer.dynamicContainer;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

class AbstractEnumeratedConditionTest {

    private static <T> Stream<DynamicTest> forSpec(final AbstractEnumeratedConditionTest.ConditionSpec<T> spec) {
        return Stream.of(
                dynamicTest("has description", () -> nonEmptyDescription(spec)),
                dynamicTest("works with collection", () -> properAsCollection(spec)),
                dynamicTest("works with single item", () -> properAsOne(spec))
        );
    }

    private static <T> void properAsCollection(final AbstractEnumeratedConditionTest.ConditionSpec<T> spec) {
        final Wrapper<?> i = spec.getMocked();
        final AbstractEnumeratedCondition<T> sut = spec.getImplementation();
        assertThat(sut.test(i)).isFalse();
        sut.add(Arrays.asList(spec.getTriggerItem(), spec.getNotTriggerItem()));
        assertThat(sut.test(i)).isTrue();
        assertThat(sut.getDescription()).isPresent();
    }

    private static <T> void properAsOne(final AbstractEnumeratedConditionTest.ConditionSpec<T> spec) {
        final Wrapper<?> i = spec.getMocked();
        final AbstractEnumeratedCondition<T> sut = spec.getImplementation();
        assertThat(sut.test(i)).isFalse();
        sut.add(spec.getTriggerItem());
        sut.add(spec.getNotTriggerItem());
        assertThat(sut.test(i)).isTrue();
        assertThat(sut.getDescription()).isPresent();
    }

    private static <T> void nonEmptyDescription(final AbstractEnumeratedConditionTest.ConditionSpec<T> spec) {
        assertThat(spec.getImplementation().getDescription()).isPresent();
    }

    @TestFactory
    Stream<DynamicNode> conditions() {
        return Stream.of(new BorrowerIncomeConditionSpec(), new BorrowerRegionConditionSpec(),
                         new LoanPurposeConditionSpec(), new LoanRatingEnumeratedConditionSpec())
                .map(spec -> dynamicContainer(spec.getImplementation().getClass().getSimpleName(), forSpec(spec)));
    }

    public interface ConditionSpec<T> {

        AbstractEnumeratedCondition<T> getImplementation();

        Wrapper<?> getMocked();

        T getTriggerItem();

        T getNotTriggerItem();
    }
}
