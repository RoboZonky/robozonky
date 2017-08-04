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
import java.util.Objects;
import java.util.Optional;

import com.github.triceo.robozonky.api.remote.entities.Participation;

public final class ParticipationDescriptor
        implements Descriptor<RecommendedParticipation, ParticipationDescriptor, Participation> {

    private final Participation participation;

    public ParticipationDescriptor(final Participation participation) {
        this.participation = participation;
    }

    @Override
    public Participation item() {
        return null;
    }

    public Optional<RecommendedParticipation> recommend() {
        return recommend(participation.getRemainingPrincipal());
    }

    @Override
    public Optional<RecommendedParticipation> recommend(final BigDecimal amount) {
        if (Objects.equals(amount, participation.getRemainingPrincipal())) {
            return Optional.of(new RecommendedParticipation(this));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final ParticipationDescriptor that = (ParticipationDescriptor) o;
        return Objects.equals(participation, that.participation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(participation);
    }

    @Override
    public String toString() {
        return "ParticipationDescriptor{" +
                "participation=" + participation +
                '}';
    }
}
