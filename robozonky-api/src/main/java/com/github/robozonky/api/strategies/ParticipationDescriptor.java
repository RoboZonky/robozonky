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
import java.util.Objects;
import java.util.Optional;

import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.api.remote.entities.Participation;

public final class ParticipationDescriptor
        implements Descriptor<RecommendedParticipation, ParticipationDescriptor, Participation> {

    private final Participation participation;
    private final Loan related;

    ParticipationDescriptor(final Participation participation) { // for testing purposes only
        this(participation, null);
    }

    public ParticipationDescriptor(final Participation participation, final Loan related) {
        this.participation = participation;
        this.related = related;
    }

    @Override
    public Participation item() {
        return participation;
    }

    @Override
    public Loan related() {
        return related;
    }

    public Optional<RecommendedParticipation> recommend() {
        return recommend(participation.getRemainingPrincipal());
    }

    @Override
    public Optional<RecommendedParticipation> recommend(final BigDecimal amount) {
        if (participation.isWillExceedLoanInvestmentLimit()) {
            return Optional.empty();
        } else if (!Objects.equals(amount, participation.getRemainingPrincipal())) {
            return Optional.empty();
        }
        return Optional.of(new RecommendedParticipation(this));
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
