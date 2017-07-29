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

package com.github.triceo.robozonky.app.delinquency;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Stream;

import com.github.triceo.robozonky.api.remote.entities.Loan;
import com.github.triceo.robozonky.common.remote.Zonky;

final class Delinquent {

    private final int loanId;
    private final SortedMap<OffsetDateTime, Delinquency> delinquencies = new TreeMap<>();

    Delinquent(final int loanId, final OffsetDateTime since) {
        this.loanId = loanId;
        this.delinquencies.put(since, new Delinquency(this, since));
    }

    Delinquent(final int loanId) {
        this.loanId = loanId;
    }

    public int getLoanId() {
        return loanId;
    }

    public Loan getLoan(final Zonky zonky) {
        return zonky.getLoan(loanId);
    }

    public Optional<Delinquency> getActiveDelinquency() {
        if (delinquencies.isEmpty()) {
            return Optional.empty();
        }
        final Delinquency latestDelinquency = delinquencies.get(delinquencies.lastKey());
        return latestDelinquency.getFixedOn()
                .map(d -> Optional.<Delinquency>empty())
                .orElse(Optional.of(latestDelinquency));
    }

    public boolean hasActiveDelinquency() {
        return getActiveDelinquency().isPresent();
    }

    public Stream<Delinquency> getDelinquencies() {
        return delinquencies.values().stream();
    }

    Delinquency addDelinquency(final OffsetDateTime since) {
        if (delinquencies.containsKey(since)) {
            return delinquencies.get(since);
        }
        final Delinquency d = new Delinquency(this, since);
        delinquencies.put(since, d);
        return d;
    }

    Delinquency addDelinquency(final OffsetDateTime since, final OffsetDateTime until) {
        if (delinquencies.containsKey(since)) {
            final Delinquency d = delinquencies.get(since);
            d.setFixedOn(until);
            return d;
        }
        final Delinquency d = new Delinquency(this, since, until);
        delinquencies.put(since, d);
        return d;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final Delinquent that = (Delinquent) o;
        return loanId == that.loanId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(loanId);
    }

    @Override
    public String toString() {
        return "Delinquent{" +
                "loanId=" + loanId +
                ", delinquencies=" + delinquencies +
                '}';
    }
}
