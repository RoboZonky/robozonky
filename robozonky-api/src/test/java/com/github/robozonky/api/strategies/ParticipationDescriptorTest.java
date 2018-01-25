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

import com.github.robozonky.api.remote.entities.Participation;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ParticipationDescriptorTest {

    private static Participation mock(final BigDecimal amount) {
        final Participation p = Mockito.mock(Participation.class);
        Mockito.when(p.getRemainingPrincipal()).thenReturn(amount);
        return p;
    }

    @Test
    public void recommend() {
        final Participation p = mock(BigDecimal.TEN);
        final ParticipationDescriptor pd = new ParticipationDescriptor(p);
        final Optional<RecommendedParticipation> r = pd.recommend(p.getRemainingPrincipal());
        Assertions.assertThat(r).isPresent();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(r.get().amount()).isEqualTo(p.getRemainingPrincipal());
            softly.assertThat(r.get().descriptor()).isEqualTo(pd);
        });
    }

    @Test
    public void recommendWrong() {
        final Participation p = mock(BigDecimal.TEN);
        final ParticipationDescriptor pd = new ParticipationDescriptor(p);
        final Optional<RecommendedParticipation> r = pd.recommend(p.getRemainingPrincipal().subtract(BigDecimal.ONE));
        Assertions.assertThat(r).isEmpty();
    }

    @Test
    public void equals() {
        final Participation p = mock(BigDecimal.TEN);
        final ParticipationDescriptor pd = new ParticipationDescriptor(p);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(pd).isNotEqualTo(null);
            softly.assertThat(pd).isNotEqualTo(UUID.randomUUID().toString());
            softly.assertThat(pd).isEqualTo(pd);
        });
        final ParticipationDescriptor pd2 = new ParticipationDescriptor(p);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(pd).isEqualTo(pd2);
            softly.assertThat(pd2).isEqualTo(pd);
        });
        final ParticipationDescriptor pd3 = new ParticipationDescriptor(mock(BigDecimal.ONE));
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(pd).isNotEqualTo(pd3);
        });
    }
}
