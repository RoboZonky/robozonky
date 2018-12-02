/*
 * Copyright 2018 The RoboZonky Project
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

import com.github.robozonky.api.remote.entities.Participation;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ParticipationDescriptorTest {

    private static Participation mockParticipation(final BigDecimal amount) {
        final Participation p = mock(Participation.class);
        when(p.getRemainingPrincipal()).thenReturn(amount);
        return p;
    }

    private static final Loan LOAN = Loan.custom().build();

    @Test
    void recommend() {
        final Participation p = mockParticipation(BigDecimal.TEN);
        final ParticipationDescriptor pd = new ParticipationDescriptor(p, () -> LOAN);
        final Optional<RecommendedParticipation> r = pd.recommend(p.getRemainingPrincipal());
        assertThat(r).isPresent();
        assertSoftly(softly -> {
            softly.assertThat(r.get().amount()).isEqualTo(p.getRemainingPrincipal());
            softly.assertThat(r.get().descriptor().related()).isSameAs(LOAN);
            softly.assertThat(r.get().descriptor()).isEqualTo(pd);
        });
    }

    @Test
    void recommendWrong() {
        final Participation p = mockParticipation(BigDecimal.TEN);
        final ParticipationDescriptor pd = new ParticipationDescriptor(p, () -> LOAN);
        final Optional<RecommendedParticipation> r = pd.recommend(p.getRemainingPrincipal().subtract(BigDecimal.ONE));
        assertThat(r).isEmpty();
    }

    @Test
    void equals() {
        final Participation p = mockParticipation(BigDecimal.TEN);
        final ParticipationDescriptor pd = new ParticipationDescriptor(p, () -> LOAN);
        assertSoftly(softly -> {
            softly.assertThat(pd).isNotEqualTo(null);
            softly.assertThat(pd).isNotEqualTo(UUID.randomUUID().toString());
            softly.assertThat(pd).isEqualTo(pd);
        });
        final ParticipationDescriptor pd2 = new ParticipationDescriptor(p, () -> LOAN);
        assertSoftly(softly -> {
            softly.assertThat(pd).isEqualTo(pd2);
            softly.assertThat(pd2).isEqualTo(pd);
        });
        final ParticipationDescriptor pd3 = new ParticipationDescriptor(mockParticipation(BigDecimal.ONE), () -> LOAN);
        assertSoftly(softly -> {
            softly.assertThat(pd).isNotEqualTo(pd3);
        });
    }
}
