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
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.triceo.robozonky.api.remote.entities.Investment;
import com.github.triceo.robozonky.internal.api.State;

abstract class KnownDelinquents {

    private static final String TIMESTAMP_SEPARATOR = ";";
    private static final Pattern SPLIT_BY_TIMESTAMP_SEPARATOR =
            Pattern.compile("\\Q" + KnownDelinquents.TIMESTAMP_SEPARATOR + "\\E");
    private final State.ClassSpecificState state = State.INSTANCE.forClass(this.getClass());
    private final Predicate<Delinquent> belongs = delinquent -> delinquent.getSince()
            .plus(Duration.ofDays(this.getThresholdInDays()))
            .isBefore(OffsetDateTime.now());

    private static Collection<Delinquent> loadDelinquents(final State.ClassSpecificState state) {
        final Map<Integer, String> read = state.getKeys().stream()
                .collect(Collectors.toMap(Integer::parseInt, key ->
                        state.getValue(key).orElseThrow(() -> new IllegalStateException("Impossible."))));
        return read.entrySet().stream()
                .flatMap(e -> {
                    final int loanId = e.getKey();
                    final String[] parts = KnownDelinquents.SPLIT_BY_TIMESTAMP_SEPARATOR.split(e.getValue());
                    return Stream.of(parts)
                            .map(OffsetDateTime::parse)
                            .map(p -> new Delinquent(loanId, p));
                }).collect(Collectors.toSet());
    }

    private static void saveDelinquents(final State.ClassSpecificState state,
                                        final Collection<Delinquent> delinquents) {
        state.reset();
        delinquents.stream()
                .collect(Collectors.groupingBy(Delinquent::getLoanId))
                .forEach((id, delinquent) -> {
                    final String joined = delinquent.stream()
                            .map(d -> d.getSince().toString())
                            .collect(Collectors.joining(KnownDelinquents.TIMESTAMP_SEPARATOR));
                    state.setValue(id.toString(), joined);
                });
    }

    protected abstract int getThresholdInDays();

    public void update(final Collection<Delinquent> delinquents) {
        final Collection<Delinquent> known = get();
        final Collection<Delinquent> belong = delinquents.stream().filter(belongs).collect(Collectors.toSet());
        if (known.addAll(belong)) {
            KnownDelinquents.saveDelinquents(state, known);
        }
    }

    public Collection<Delinquent> get() {
        return KnownDelinquents.loadDelinquents(state);
    }

    public void purge(final Collection<Investment> toRemove) {
        final Collection<Delinquent> known = get();
        final boolean removed = known.removeIf(d -> toRemove.stream().anyMatch(i -> i.getLoanId() == d.getLoanId()));
        if (removed) {
            KnownDelinquents.saveDelinquents(state, known);
        }
    }
}
