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

import java.util.stream.Stream;

import com.github.robozonky.api.Ratio;
import com.github.robozonky.api.remote.enums.Rating;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.DynamicContainer.dynamicContainer;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

class DefaultPortfolioTest {

    private static void hasValue(final DefaultPortfolio p, final Rating r) {
        assertThat(p.getDefaultShare(r)).isEqualTo(Ratio.ZERO);
    }

    private static void unknownValue(final DefaultPortfolio p) {
        assertThatThrownBy(() -> p.getDefaultShare(null)).isInstanceOf(IllegalStateException.class);
    }

    private static Stream<DynamicTest> forRating(final Rating r) {
        return Stream.of(
                dynamicTest("has value", () -> hasValue(DefaultPortfolio.EMPTY, r)),
                dynamicTest("has unknown value", () -> unknownValue(DefaultPortfolio.EMPTY))
        );
    }

    @TestFactory
    Stream<DynamicNode> ratings() {
        return Stream.of(Rating.values()).map(r -> dynamicContainer(r.toString(), forRating(r)));
    }
}
