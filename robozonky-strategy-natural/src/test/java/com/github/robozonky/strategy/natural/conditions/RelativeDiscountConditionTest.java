/*
 * Copyright 2021 The RoboZonky Project
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
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.github.robozonky.api.Ratio;
import com.github.robozonky.strategy.natural.wrappers.Wrapper;

class RelativeDiscountConditionTest {

    @Test
    void noDiscount() {
        final Wrapper<?> w = mock(Wrapper.class);
        when(w.getRemainingPrincipal()).thenReturn(BigDecimal.valueOf(100));
        when(w.getSellPrice()).thenReturn(Optional.of(BigDecimal.valueOf(100)));
        final MarketplaceFilterCondition moreThanZeroDiscount = RelativeDiscountCondition
            .moreThan(Ratio.fromPercentage(0));
        final MarketplaceFilterCondition lessThanOneDiscount = RelativeDiscountCondition
            .lessThan(Ratio.fromPercentage(1));
        assertSoftly(softly -> {
            softly.assertThat(moreThanZeroDiscount)
                .rejects(w);
            softly.assertThat(lessThanOneDiscount)
                .accepts(w);
        });
    }

    @Test
    void lessThan() {
        final MarketplaceFilterCondition condition = RelativeDiscountCondition.lessThan(Ratio.fromPercentage(10));
        final Wrapper<?> w = mock(Wrapper.class);
        when(w.getRemainingPrincipal()).thenReturn(BigDecimal.valueOf(100));
        when(w.getSellPrice()).thenReturn(Optional.of(BigDecimal.valueOf(90)));
        assertThat(condition).rejects(w);
        when(w.getSellPrice()).thenReturn(Optional.of(BigDecimal.valueOf(91)));
        assertThat(condition).accepts(w);
        when(w.getSellPrice()).thenReturn(Optional.of(BigDecimal.valueOf(89)));
        assertThat(condition).rejects(w);
    }

    @Test
    void moreThan() {
        final MarketplaceFilterCondition condition = RelativeDiscountCondition.moreThan(Ratio.fromPercentage(10));
        final Wrapper<?> w = mock(Wrapper.class);
        when(w.getRemainingPrincipal()).thenReturn(BigDecimal.valueOf(100));
        when(w.getSellPrice()).thenReturn(Optional.of(BigDecimal.valueOf(90)));
        assertThat(condition).rejects(w);
        when(w.getSellPrice()).thenReturn(Optional.of(BigDecimal.valueOf(89)));
        assertThat(condition).accepts(w);
        when(w.getSellPrice()).thenReturn(Optional.of(BigDecimal.valueOf(91)));
        assertThat(condition).rejects(w);
    }

    @Test
    void exact() {
        final MarketplaceFilterCondition condition = RelativeDiscountCondition.exact(Ratio.fromPercentage(8),
                Ratio.fromPercentage(10));
        final Wrapper<?> w = mock(Wrapper.class);
        when(w.getRemainingPrincipal()).thenReturn(BigDecimal.valueOf(100));
        when(w.getSellPrice()).thenReturn(Optional.of(BigDecimal.valueOf(90)));
        assertThat(condition).accepts(w);
        when(w.getSellPrice()).thenReturn(Optional.of(BigDecimal.valueOf(91)));
        assertThat(condition).accepts(w);
        when(w.getSellPrice()).thenReturn(Optional.of(BigDecimal.valueOf(92)));
        assertThat(condition).accepts(w);
        when(w.getSellPrice()).thenReturn(Optional.of(BigDecimal.valueOf(93)));
        assertThat(condition).rejects(w);
        when(w.getSellPrice()).thenReturn(Optional.of(BigDecimal.valueOf(89)));
        assertThat(condition).rejects(w);
    }

    @Test
    void hasDescription() {
        final MarketplaceFilterCondition condition = RelativeDiscountCondition.exact(Ratio.ONE, Ratio.ONE);
        assertThat(condition.getDescription()).isNotEmpty();
    }
}
