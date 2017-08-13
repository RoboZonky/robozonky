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

import com.github.triceo.robozonky.api.remote.entities.Participation;

public final class RecommendedParticipation
        implements Recommended<RecommendedParticipation, ParticipationDescriptor, Participation> {

    private final ParticipationDescriptor participationDescriptor;

    RecommendedParticipation(final ParticipationDescriptor participationDescriptor) {
        this.participationDescriptor = participationDescriptor;
    }

    @Override
    public ParticipationDescriptor descriptor() {
        return participationDescriptor;
    }

    @Override
    public BigDecimal amount() {
        return participationDescriptor.item().getRemainingPrincipal();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final RecommendedParticipation that = (RecommendedParticipation) o;
        return Objects.equals(participationDescriptor, that.participationDescriptor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(participationDescriptor);
    }

    @Override
    public String toString() {
        return "RecommendedParticipation{" +
                "participationDescriptor=" + participationDescriptor +
                '}';
    }
}

