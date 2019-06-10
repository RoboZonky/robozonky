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

import com.github.robozonky.strategy.natural.Wrapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ElapsedLoanTermConditionTest {

    @Test
    void lessThan() {
        final MarketplaceFilterCondition condition = ElapsedLoanTermCondition.lessThan(1);
        final Wrapper<?> w = mock(Wrapper.class);
        when(w.getOriginalTermInMonths()).thenReturn(10);
        when(w.getRemainingTermInMonths()).thenReturn(10);
        assertThat(condition).accepts(w);
        when(w.getRemainingTermInMonths()).thenReturn(9);
        assertThat(condition).rejects(w);
    }

    @Test
    void moreThan() {
        final MarketplaceFilterCondition condition = ElapsedLoanTermCondition.moreThan(0);
        final Wrapper<?> w = mock(Wrapper.class);
        when(w.getOriginalTermInMonths()).thenReturn(10);
        when(w.getRemainingTermInMonths()).thenReturn(10);
        assertThat(condition).rejects(w);
        when(w.getRemainingTermInMonths()).thenReturn(9);
        assertThat(condition).accepts(w);
    }

    @Test
    void exact() {
        final MarketplaceFilterCondition condition = ElapsedLoanTermCondition.exact(0, 1);
        final Wrapper<?> w = mock(Wrapper.class);
        when(w.getOriginalTermInMonths()).thenReturn(10);
        when(w.getRemainingTermInMonths()).thenReturn(10);
        assertThat(condition).accepts(w);
        when(w.getRemainingTermInMonths()).thenReturn(9);
        assertThat(condition).accepts(w);
        when(w.getRemainingTermInMonths()).thenReturn(8);
        assertThat(condition).rejects(w);
    }

    @Test
    void hasDescription() {
        final MarketplaceFilterCondition condition = ElapsedLoanTermCondition.exact(1, 1);
        assertThat(condition.getDescription()).isNotEmpty();
    }
}

