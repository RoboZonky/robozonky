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

package com.github.robozonky.api.strategies;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import com.github.robozonky.api.remote.entities.Investment;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.*;
import static org.mockito.Mockito.*;

class InvestmentDescriptorTest {

    private static Investment mockInvestment(final BigDecimal amount) {
        final Investment i = mock(Investment.class);
        when(i.getRemainingPrincipal()).thenReturn(amount);
        return i;
    }

    @Test
    void recommend() {
        final Investment i = mockInvestment(BigDecimal.TEN);
        final InvestmentDescriptor id = new InvestmentDescriptor(i);
        final Optional<RecommendedInvestment> r = id.recommend(i.getRemainingPrincipal());
        assertThat(r).isPresent();
        assertSoftly(softly -> {
            softly.assertThat(r.get().amount()).isEqualTo(i.getRemainingPrincipal());
            softly.assertThat(r.get().descriptor()).isEqualTo(id);
        });
    }

    @Test
    void recommendWrong() {
        final Investment i = mockInvestment(BigDecimal.TEN);
        final InvestmentDescriptor id = new InvestmentDescriptor(i);
        final Optional<RecommendedInvestment> r = id.recommend(i.getRemainingPrincipal().subtract(BigDecimal.ONE));
        assertThat(r).isEmpty();
    }

    @Test
    void equals() {
        final Investment i = mockInvestment(BigDecimal.TEN);
        final InvestmentDescriptor id = new InvestmentDescriptor(i);
        assertSoftly(softly -> {
            softly.assertThat(id).isNotEqualTo(null);
            softly.assertThat(id).isNotEqualTo(UUID.randomUUID().toString());
            softly.assertThat(id).isEqualTo(id);
        });
        final InvestmentDescriptor id2 = new InvestmentDescriptor(i);
        assertSoftly(softly -> {
            softly.assertThat(id).isEqualTo(id2);
            softly.assertThat(id2).isEqualTo(id);
        });
        final InvestmentDescriptor id3 = new InvestmentDescriptor(mockInvestment(BigDecimal.ONE));
        assertSoftly(softly -> {
            softly.assertThat(id).isNotEqualTo(id3);
        });
    }
}
