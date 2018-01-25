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

package com.github.robozonky.strategy.natural.conditions;

import java.math.BigDecimal;

import com.github.robozonky.strategy.natural.Wrapper;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

class RangeConditionTest {

    @Test
    public void constructor() {
        final RangeCondition<Wrapper> c = new RangeCondition<>((w) -> 0, 0, 1);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(c.getMinInclusive()).isEqualTo(BigDecimal.ZERO);
            softly.assertThat(c.getMaxInclusive()).isEqualTo(BigDecimal.ONE);
        });
    }

    @Test
    public void constructorReversed() {
        final RangeCondition<Wrapper> c = new RangeCondition<>((w) -> 0, 1, 0);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(c.getMinInclusive()).isEqualTo(BigDecimal.ZERO);
            softly.assertThat(c.getMaxInclusive()).isEqualTo(BigDecimal.ONE);
        });
    }
}
