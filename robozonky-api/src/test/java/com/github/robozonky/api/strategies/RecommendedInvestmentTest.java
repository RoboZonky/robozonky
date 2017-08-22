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
import java.util.UUID;

import com.github.robozonky.api.remote.entities.Investment;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.mockito.Mockito;

public class RecommendedInvestmentTest {

    private static Investment mock() {
        final Investment i = Mockito.mock(Investment.class);
        Mockito.when(i.getRemainingPrincipal()).thenReturn(BigDecimal.TEN);
        return i;
    }

    @Test
    public void equals() {
        final Investment i = mock();
        final InvestmentDescriptor d = new InvestmentDescriptor(i);
        final RecommendedInvestment r = new RecommendedInvestment(d);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(r).isNotEqualTo(null);
            softly.assertThat(r).isNotEqualTo(UUID.randomUUID().toString());
            softly.assertThat(r).isEqualTo(r);
        });
        final RecommendedInvestment r2 = new RecommendedInvestment(d);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(r).isEqualTo(r2);
            softly.assertThat(r2).isEqualTo(r);
        });
        final RecommendedInvestment r3 = new RecommendedInvestment(new InvestmentDescriptor(mock()));
        Assertions.assertThat(r).isNotEqualTo(r3);
    }
}
