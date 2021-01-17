/*
 * Copyright 2021 The RoboZonky Project
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

import java.util.Objects;
import java.util.function.Supplier;

import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.api.remote.entities.Participation;
import com.github.robozonky.api.remote.entities.ParticipationDetail;
import com.github.robozonky.internal.util.functional.Memoizer;

public final class ParticipationDescriptor implements Descriptor<Participation> {

    private final Participation participation;
    private final Supplier<Loan> related;
    private final Supplier<ParticipationDetail> detail;

    public ParticipationDescriptor(final Participation participation, final Supplier<Loan> related) {
        this(participation, related, () -> null); // Simplified constructor for testing.
    }

    /**
     *
     * @param participation Participation in question.
     * @param related       Provided as a Supplier in order to allow the calling code to retrieve the (likely remote)
     *                      entity on-demand.
     * @param detail        Provided as a {@link Supplier} in order to allow the calling code to retrieve the (likely
     *                      remote) entity on-demand.
     */
    public ParticipationDescriptor(final Participation participation, final Supplier<Loan> related,
            final Supplier<ParticipationDetail> detail) {
        this.participation = Objects.requireNonNull(participation);
        this.related = Memoizer.memoize(Objects.requireNonNull(related));
        this.detail = Memoizer.memoize(Objects.requireNonNull(detail));
    }

    @Override
    public Participation item() {
        return participation;
    }

    @Override
    public Loan related() {
        return related.get();
    }

    public ParticipationDetail detail() {
        return detail.get();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !Objects.equals(getClass(), o.getClass())) {
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
