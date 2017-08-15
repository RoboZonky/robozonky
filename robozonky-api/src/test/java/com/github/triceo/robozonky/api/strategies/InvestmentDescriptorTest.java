/*
 * Copyright 2017 Lukáš Petrovický
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

package com.github.triceo.robozonky.api.strategies;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import com.github.triceo.robozonky.api.remote.entities.Investment;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.mockito.Mockito;

public class InvestmentDescriptorTest {

    private static Investment mock(final BigDecimal amount) {
        final Investment i = Mockito.mock(Investment.class);
        Mockito.when(i.getRemainingPrincipal()).thenReturn(amount);
        return i;
    }

    @Test
    public void recommend() {
        final Investment i = mock(BigDecimal.TEN);
        final InvestmentDescriptor id = new InvestmentDescriptor(i);
        final Optional<RecommendedInvestment> r = id.recommend(i.getRemainingPrincipal());
        Assertions.assertThat(r).isPresent();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(r.get().amount()).isEqualTo(i.getRemainingPrincipal());
            softly.assertThat(r.get().descriptor()).isEqualTo(id);
        });
    }

    @Test
    public void recommendWrong() {
        final Investment i = mock(BigDecimal.TEN);
        final InvestmentDescriptor id = new InvestmentDescriptor(i);
        final Optional<RecommendedInvestment> r = id.recommend(i.getRemainingPrincipal().subtract(BigDecimal.ONE));
        Assertions.assertThat(r).isEmpty();
    }

    @Test
    public void equals() {
        final Investment i = mock(BigDecimal.TEN);
        final InvestmentDescriptor id = new InvestmentDescriptor(i);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(id).isNotEqualTo(null);
            softly.assertThat(id).isNotEqualTo(UUID.randomUUID().toString());
            softly.assertThat(id).isEqualTo(id);
        });
        final InvestmentDescriptor id2 = new InvestmentDescriptor(i);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(id).isEqualTo(id2);
            softly.assertThat(id2).isEqualTo(id);
        });
        final InvestmentDescriptor id3 = new InvestmentDescriptor(mock(BigDecimal.ONE));
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(id).isNotEqualTo(id3);
        });
    }
}
