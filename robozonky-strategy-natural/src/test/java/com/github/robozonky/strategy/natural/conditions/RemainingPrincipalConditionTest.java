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

import java.math.BigDecimal;

import com.github.robozonky.strategy.natural.Wrapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class RemainingPrincipalConditionTest {

    @Test
    void lessThan() {
        final MarketplaceFilterCondition condition = RemainingPrincipalCondition.lessThan(1);
        final Wrapper<?> w = mock(Wrapper.class);
        when(w.getRemainingPrincipal()).thenReturn(BigDecimal.ZERO);
        assertThat(condition).accepts(w);
        when(w.getRemainingPrincipal()).thenReturn(BigDecimal.ONE);
        assertThat(condition).rejects(w);
    }

    @Test
    void moreThan() {
        final MarketplaceFilterCondition condition = RemainingPrincipalCondition.moreThan(0);
        final Wrapper<?> w = mock(Wrapper.class);
        when(w.getRemainingPrincipal()).thenReturn(BigDecimal.ZERO);
        assertThat(condition).rejects(w);
        when(w.getRemainingPrincipal()).thenReturn(BigDecimal.ONE);
        assertThat(condition).accepts(w);
    }

    @Test
    void exact() {
        final MarketplaceFilterCondition condition = RemainingPrincipalCondition.exact(0, 1);
        final Wrapper<?> w = mock(Wrapper.class);
        when(w.getRemainingPrincipal()).thenReturn(BigDecimal.ZERO);
        assertThat(condition).accepts(w);
        when(w.getRemainingPrincipal()).thenReturn(BigDecimal.ONE);
        assertThat(condition).accepts(w);
        when(w.getRemainingPrincipal()).thenReturn(BigDecimal.TEN);
        assertThat(condition).rejects(w);
    }
}
