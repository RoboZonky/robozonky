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

package com.github.triceo.robozonky.app.investing.delinquency;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.stream.Collectors;

import com.github.triceo.robozonky.api.remote.entities.Investment;
import com.github.triceo.robozonky.internal.api.State;

public final class PresentDelinquents {

    private final State.ClassSpecificState state = State.INSTANCE.forClass(this.getClass());

    private static Collection<Delinquent> loadDelinquents(final State.ClassSpecificState state) {
        return state.getKeys().stream()
                .map(key -> {
                    final int loanId = Integer.parseInt(key);
                    final String rawSince =
                            state.getValue(key).orElseThrow(() -> new IllegalStateException("Impossible."));
                    final OffsetDateTime since = OffsetDateTime.parse(rawSince);
                    return new Delinquent(loanId, since);
                })
                .collect(Collectors.toSet());
    }

    private static void saveDelinquents(final State.ClassSpecificState state,
                                        final Collection<Delinquent> delinquents) {
        state.reset();
        delinquents.forEach(d -> state.setValue(String.valueOf(d.getLoanId()), d.getSince().toString()));
    }

    public void update(final Collection<Investment> receivedDelinquents) {
        final OffsetDateTime detectionTimestamp = OffsetDateTime.now();
        final Collection<Delinquent> received = receivedDelinquents.stream()
                .map(i -> {
                    final int daysPastDue = i.getDpd();
                    final OffsetDateTime lapsed = detectionTimestamp.minus(Duration.ofDays(daysPastDue));
                    return new Delinquent(i.getLoanId(), lapsed);
                }).collect(Collectors.toSet());
        // load pre-existing delinquents, removing ones that are no longer delinquent
        final Collection<Delinquent> known = get().stream()
                .filter(d -> received.stream().anyMatch(r -> r.getLoanId() == d.getLoanId()))
                .collect(Collectors.toSet());
        // update the list of known delinquents based on the newly received information
        if (known.addAll(received)) {
            PresentDelinquents.saveDelinquents(state, known);
        }
    }

    public Collection<Delinquent> get() {
        return PresentDelinquents.loadDelinquents(state);
    }
}
