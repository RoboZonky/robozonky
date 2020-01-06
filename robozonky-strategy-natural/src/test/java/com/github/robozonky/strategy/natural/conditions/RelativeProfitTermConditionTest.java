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

import java.math.BigDecimal;
import java.util.Optional;

import com.github.robozonky.api.Ratio;
import com.github.robozonky.strategy.natural.Wrapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class RelativeProfitTermConditionTest {

    @Test
    void lessThan() {
        final MarketplaceFilterCondition condition = RelativeProfitCondition.lessThan(Ratio.fromPercentage(10));
        final Wrapper<?> w = mock(Wrapper.class);
        when(w.getOriginalPurchasePrice()).thenReturn(Optional.of(BigDecimal.ONE));
        when(w.getReturns()).thenReturn(Optional.of(BigDecimal.ONE));
        assertThat(condition).accepts(w);
        when(w.getReturns()).thenReturn(Optional.of(BigDecimal.TEN));
        assertThat(condition).rejects(w);
    }

    @Test
    void moreThan() {
        final MarketplaceFilterCondition condition = RelativeProfitCondition.moreThan(Ratio.fromPercentage(9));
        final Wrapper<?> w = mock(Wrapper.class);
        when(w.getOriginalPurchasePrice()).thenReturn(Optional.of(BigDecimal.ONE));
        when(w.getReturns()).thenReturn(Optional.of(BigDecimal.ONE));
        assertThat(condition).rejects(w);
        when(w.getReturns()).thenReturn(Optional.of(BigDecimal.TEN));
        assertThat(condition).accepts(w);
    }

    @Test
    void exact() {
        final MarketplaceFilterCondition condition = RelativeProfitCondition.exact(Ratio.fromPercentage(0),
                                                                                   Ratio.fromPercentage(10));
        final Wrapper<?> w = mock(Wrapper.class);
        when(w.getOriginalPurchasePrice()).thenReturn(Optional.of(BigDecimal.ONE));
        when(w.getReturns()).thenReturn(Optional.of(BigDecimal.ONE));
        assertThat(condition).accepts(w);
        when(w.getReturns()).thenReturn(Optional.of(BigDecimal.TEN));
        assertThat(condition).rejects(w);
    }
}

