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

import com.github.robozonky.strategy.natural.Wrapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LoanAmountConditionTest {

    @Test
    void leftBoundWrong() {
        assertSoftly(softly -> {
            softly.assertThatThrownBy(() -> new LoanAmountCondition(-1, 0))
                    .isInstanceOf(IllegalArgumentException.class);
            softly.assertThatThrownBy(() -> new LoanAmountCondition(0, -1))
                    .isInstanceOf(IllegalArgumentException.class);
            softly.assertThatThrownBy(() -> new LoanAmountCondition(-1, -1))
                    .isInstanceOf(IllegalArgumentException.class);
        });
    }

    @Test
    void boundaryCorrect() {
        final Wrapper<?> l = mock(Wrapper.class);
        when(l.getOriginalAmount()).thenReturn(0);
        final MarketplaceFilterConditionImpl condition = new LoanAmountCondition(0, 0);
        assertThat(condition.test(l)).isTrue();
    }

    @Test
    void leftOutOfBounds() {
        final Wrapper<?> l = mock(Wrapper.class);
        when(l.getOriginalAmount()).thenReturn(0);
        final MarketplaceFilterConditionImpl condition = new LoanAmountCondition(1, 1);
        assertThat(condition.test(l)).isFalse();
    }

    @Test
    void rightOutOfBounds() {
        final Wrapper<?> l = mock(Wrapper.class);
        when(l.getOriginalAmount()).thenReturn(2);
        final MarketplaceFilterConditionImpl condition = new LoanAmountCondition(1, 1);
        assertThat(condition.test(l)).isFalse();
    }
}
