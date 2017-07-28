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
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.triceo.robozonky.api.notifications.Event;
import com.github.triceo.robozonky.api.remote.entities.Investment;
import com.github.triceo.robozonky.api.remote.entities.Loan;
import com.github.triceo.robozonky.app.Events;
import com.github.triceo.robozonky.internal.api.State;

/**
 * Keeps a historical record of which loans were delinquent for a period of time longer than
 * {@link #getThresholdInDays()}.
 */
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

    /**
     * How many days after the due date must a loan be delinquent for in order to count towards this statistic.
     * @return Number of days, inclusive.
     */
    protected abstract int getThresholdInDays();

    protected abstract Event getEventToFire(final Loan delinquent, final OffsetDateTime since);

    private void fireEvent(final Delinquent delinquent, final Function<Integer, Loan> loanProvider) {
        final int loanId = delinquent.getLoanId();
        final Loan l = loanProvider.apply(loanId);
        Events.fire(getEventToFire(l, delinquent.getSince()));
    }

    /**
     * Update the evidence of delinquent loans by the loans which are currently delinquent.
     * @param delinquents Loans delinquent at the moment.
     */
    public void update(final Collection<Delinquent> delinquents, final Function<Integer, Loan> loanProvider) {
        final Collection<Delinquent> known = get();
        final Collection<Delinquent> belong = delinquents.stream()
                .filter(belongs)
                .filter(d -> !known.contains(d))
                .peek(d -> this.fireEvent(d, loanProvider))
                .collect(Collectors.toSet());
        if (known.addAll(belong)) {
            KnownDelinquents.saveDelinquents(state, known);
        }
    }

    /**
     * Get the loans that, at some point in time, were delinquent for a time longer or equal to
     * {@link #getThresholdInDays()}.
     *
     * @return Loans over threshold.
     */
    public Collection<Delinquent> get() {
        return KnownDelinquents.loadDelinquents(state);
    }

    /**
     * Remove loans from the evidence that are no longer active - that is, they were fully paid or written off.
     * @param deadLoans Loans to remove from evidence.
     */
    public void purge(final Collection<Investment> deadLoans) {
        final Collection<Delinquent> known = get();
        final boolean removed = known.removeIf(d -> deadLoans.stream().anyMatch(i -> i.getLoanId() == d.getLoanId()));
        if (removed) {
            KnownDelinquents.saveDelinquents(state, known);
        }
    }
}
