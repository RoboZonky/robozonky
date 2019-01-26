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

package com.github.robozonky.api.strategies;

import java.math.BigDecimal;
import java.util.UUID;

import com.github.robozonky.api.remote.entities.Participation;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.*;

class RecommendedParticipationTest {

    private static final Loan LOAN = Loan.custom().build();

    private static Participation mockParticipation() {
        final Participation p = mock(Participation.class);
        when(p.getRemainingPrincipal()).thenReturn(BigDecimal.TEN);
        return p;
    }

    @Test
    void equals() {
        final Participation p = mockParticipation();
        final ParticipationDescriptor d = new ParticipationDescriptor(p, () -> LOAN);
        final RecommendedParticipation r = new RecommendedParticipation(d);
        assertSoftly(softly -> {
            softly.assertThat(r).isNotEqualTo(null);
            softly.assertThat(r).isNotEqualTo(UUID.randomUUID().toString());
            softly.assertThat(r).isEqualTo(r);
        });
        final RecommendedParticipation r2 = new RecommendedParticipation(d);
        assertSoftly(softly -> {
            softly.assertThat(r).isEqualTo(r2);
            softly.assertThat(r2).isEqualTo(r);
        });
        final RecommendedParticipation r3 =
                new RecommendedParticipation(new ParticipationDescriptor(mockParticipation(), () -> LOAN));
        assertThat(r).isNotEqualTo(r3);
    }
}
