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

import com.github.robozonky.strategy.natural.Wrapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class SmpFeePresenceConditionTest {

    @Test
    void present() {
        final Wrapper<?> l = mock(Wrapper.class);
        when(l.getSellFee()).thenReturn(Optional.empty());
        assertThat(SmpFeePresenceCondition.PRESENT.test(l)).isFalse();
        when(l.getSellFee()).thenReturn(Optional.of(BigDecimal.ZERO));
        assertThat(SmpFeePresenceCondition.PRESENT.test(l)).isFalse();
        when(l.getSellFee()).thenReturn(Optional.of(BigDecimal.ONE));
        assertThat(SmpFeePresenceCondition.PRESENT.test(l)).isTrue();
    }

    @Test
    void notPresent() {
        final Wrapper<?> l = mock(Wrapper.class);
        when(l.getSellFee()).thenReturn(Optional.empty());
        assertThat(SmpFeePresenceCondition.NOT_PRESENT.test(l)).isTrue();
        when(l.getSellFee()).thenReturn(Optional.of(BigDecimal.ZERO));
        assertThat(SmpFeePresenceCondition.NOT_PRESENT.test(l)).isTrue();
        when(l.getSellFee()).thenReturn(Optional.of(BigDecimal.ONE));
        assertThat(SmpFeePresenceCondition.NOT_PRESENT.test(l)).isFalse();
    }
}
