/*
 * Copyright 2020 The RoboZonky Project
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;

import com.github.robozonky.api.Ratio;
import com.github.robozonky.strategy.natural.wrappers.Wrapper;

class RangeConditionTest {

    @Test
    void moreThan() {
        final RangeCondition<Integer> c = RangeCondition.moreThan(Wrapper::getOriginalTermInMonths,
                new Domain<>(Integer.class, 0, null), 5);
        final Wrapper<?> w = mock(Wrapper.class);
        when(w.getOriginalTermInMonths()).thenReturn(5);
        assertThat(c).rejects(w);
        when(w.getOriginalTermInMonths()).thenReturn(6);
        assertThat(c).accepts(w);
    }

    @Test
    void lessThan() {
        final RangeCondition<Integer> c = RangeCondition.lessThan(Wrapper::getOriginalTermInMonths,
                new Domain<>(Integer.class, 0, null), 5);
        final Wrapper<?> w = mock(Wrapper.class);
        when(w.getOriginalTermInMonths()).thenReturn(5);
        assertThat(c).rejects(w);
        when(w.getOriginalTermInMonths()).thenReturn(4);
        assertThat(c).accepts(w);
    }

    @Test
    void exact() {
        final RangeCondition<Integer> c = RangeCondition.exact(Wrapper::getOriginalTermInMonths,
                new Domain<>(Integer.class, 0, null), 5, 5);
        final Wrapper<?> w = mock(Wrapper.class);
        when(w.getOriginalTermInMonths()).thenReturn(5);
        assertThat(c).accepts(w);
        when(w.getOriginalTermInMonths()).thenReturn(4);
        assertThat(c).rejects(w);
        when(w.getOriginalTermInMonths()).thenReturn(6);
        assertThat(c).rejects(w);
    }

    @Test
    void relativeMoreThan() {
        final RangeCondition<Ratio> c = RangeCondition.relativeMoreThan(Wrapper::getRemainingTermInMonths,
                Wrapper::getOriginalTermInMonths,
                Ratio.fromPercentage(15));
        final Wrapper<?> w = mock(Wrapper.class);
        when(w.getOriginalTermInMonths()).thenReturn(100);
        when(w.getRemainingTermInMonths()).thenReturn(15);
        assertThat(c).rejects(w);
        when(w.getRemainingTermInMonths()).thenReturn(16);
        assertThat(c).accepts(w);
    }

    @Test
    void relativeLessThan() {
        final RangeCondition<Ratio> c = RangeCondition.relativeLessThan(Wrapper::getRemainingTermInMonths,
                Wrapper::getOriginalTermInMonths,
                Ratio.fromPercentage(15));
        final Wrapper<?> w = mock(Wrapper.class);
        when(w.getOriginalTermInMonths()).thenReturn(100);
        when(w.getRemainingTermInMonths()).thenReturn(15);
        assertThat(c).rejects(w);
        when(w.getRemainingTermInMonths()).thenReturn(14);
        assertThat(c).accepts(w);
    }

    @Test
    void relativeExact() {
        final RangeCondition<Ratio> c = RangeCondition.relativeExact(Wrapper::getRemainingTermInMonths,
                Wrapper::getOriginalTermInMonths,
                Ratio.fromPercentage(15), Ratio.fromPercentage(15));
        final Wrapper<?> w = mock(Wrapper.class);
        when(w.getOriginalTermInMonths()).thenReturn(100);
        when(w.getRemainingTermInMonths()).thenReturn(15);
        assertThat(c).accepts(w);
        when(w.getRemainingTermInMonths()).thenReturn(14);
        assertThat(c).rejects(w);
        when(w.getRemainingTermInMonths()).thenReturn(16);
        assertThat(c).rejects(w);
    }

}
