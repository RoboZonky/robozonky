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

class RelativeRemainingInterestConditionTest {

    @Test
    void noRemainder() {
        final Wrapper<?> w = mock(Wrapper.class);
        when(w.getRemainingInterest()).thenReturn(Optional.of(BigDecimal.valueOf(0)));
        when(w.getOriginalInterest()).thenReturn(Optional.of(BigDecimal.valueOf(100)));
        final MarketplaceFilterCondition moreThanZero = RelativeRemainingInterestCondition
            .moreThan(Ratio.fromPercentage(0));
        final MarketplaceFilterCondition lessThanOne = RelativeRemainingInterestCondition
            .lessThan(Ratio.fromPercentage(1));
        assertSoftly(softly -> {
            softly.assertThat(moreThanZero)
                .rejects(w);
            softly.assertThat(lessThanOne)
                .accepts(w);
        });
    }

    @Test
    void fullRemainder() {
        final Wrapper<?> w = mock(Wrapper.class);
        when(w.getRemainingInterest()).thenReturn(Optional.of(BigDecimal.valueOf(100)));
        when(w.getOriginalInterest()).thenReturn(Optional.of(BigDecimal.valueOf(100)));
        final MarketplaceFilterCondition moreThanHundred = RelativeRemainingInterestCondition
            .moreThan(Ratio.fromPercentage(100));
        final MarketplaceFilterCondition exactHundred = RelativeRemainingInterestCondition.exact(
                Ratio.fromPercentage(100),
                Ratio.fromPercentage(100));
        final MarketplaceFilterCondition lessThanHundred = RelativeRemainingInterestCondition
            .lessThan(Ratio.fromPercentage(100));
        assertSoftly(softly -> {
            softly.assertThat(moreThanHundred)
                .rejects(w);
            softly.assertThat(exactHundred)
                .accepts(w);
            softly.assertThat(lessThanHundred)
                .rejects(w);
        });
    }

    @Test
    void hasDescription() {
        final MarketplaceFilterCondition condition = RelativeDiscountCondition.exact(Ratio.ONE, Ratio.ONE);
        assertThat(condition.getDescription()).isNotEmpty();
    }
}
