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

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.triceo.robozonky.api.notifications.LoanNoLongerDelinquentEvent;
import com.github.triceo.robozonky.api.remote.entities.Investment;
import com.github.triceo.robozonky.api.remote.enums.PaymentStatus;
import com.github.triceo.robozonky.app.Events;
import com.github.triceo.robozonky.common.remote.Zonky;
import com.github.triceo.robozonky.internal.api.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main entry point to the delinquency API.
 */
public enum DelinquencyTracker {

    INSTANCE; // cheap thread-safe singleton

    private static final Logger LOGGER = LoggerFactory.getLogger(DelinquencyTracker.class);
    private static final String ITEM_SEPARATOR = ";", TIME_SEPARATOR = ":::";
    private static final Pattern ITEM_SPLITTER = Pattern.compile("\\Q" + ITEM_SEPARATOR + "\\E"),
            TIME_SPLITTER = Pattern.compile("\\Q" + TIME_SEPARATOR + "\\E");

    private static String toString(final Delinquency d) {
        return d.getFixedOn()
                .map(fixedOn -> d.getDetectedOn() + TIME_SEPARATOR + fixedOn)
                .orElse(d.getDetectedOn().toString());
    }

    private static String toString(final Delinquent d) {
        return d.getDelinquencies().map(DelinquencyTracker::toString).collect(Collectors.joining(ITEM_SEPARATOR));
    }

    private static Delinquency fromString(final Delinquent d, final String delinquency) {
        final String[] parts = TIME_SPLITTER.split(delinquency);
        if (parts.length == 1) {
            return d.addDelinquency(LocalDate.parse(parts[0]));
        } else if (parts.length == 2) {
            return d.addDelinquency(LocalDate.parse(parts[0]), LocalDate.parse(parts[1]));
        } else {
            throw new IllegalStateException("Unexpected number of dates: " + parts.length);
        }
    }

    private static Delinquent fromString(final int loanId, final String delinquencies) {
        final Delinquent d = new Delinquent(loanId);
        Stream.of(ITEM_SPLITTER.split(delinquencies)).forEach(delinquency -> fromString(d, delinquency));
        return d;
    }

    public void update(final Zonky zonky, final Collection<Investment> presentlyDelinquent) {
        update(zonky, presentlyDelinquent, Collections.emptyList());
    }

    /**
     * Updates delinquency information based on the information about loans that are either currently delinquent or no
     * longer active. Will fire events on new delinquencies and/or on loans no longer delinquent.
     * @param zonky The API that will be used to retrieve the loan instances.
     * @param presentlyDelinquent Loans that currently have overdue instalments. This corresponds to
     * {@link PaymentStatus#getDelinquent()}
     * @param noLongerActive Loans that are no longer relevant. This corresponds to {@link PaymentStatus#getDone()}.
     */
    public void update(final Zonky zonky, final Collection<Investment> presentlyDelinquent,
                       final Collection<Investment> noLongerActive) {
        final LocalDate now = LocalDate.now();
        final Collection<Delinquent> knownDelinquents = this.getDelinquents();
        knownDelinquents.stream()
                .filter(Delinquent::hasActiveDelinquency) // only care about present delinquents
                .filter(d -> presentlyDelinquent.stream().noneMatch(i -> d.getLoanId() == i.getLoanId()))
                .flatMap(d -> d.getActiveDelinquency().map(Stream::of).orElse(Stream.empty()))
                .peek(d -> d.setFixedOn(now.minusDays(1))) // end the delinquency
                .map(Delinquency::getParent)
                .forEach(d -> Events.fire(new LoanNoLongerDelinquentEvent(d.getLoan(zonky)))); // notify
        final Stream<Delinquent> stillDelinquent = knownDelinquents.stream()
                .filter(d -> noLongerActive.stream().noneMatch(i -> d.getLoanId() == i.getLoanId()));
        final Stream<Delinquent> newDelinquents = presentlyDelinquent.stream()
                .filter(i -> knownDelinquents.stream().noneMatch(d -> d.getLoanId() == i.getLoanId()))
                .map(i -> {
                    // no matter when we register delinquency, we can be sure it's delinquent from the last payment day
                    final LocalDate delinquentPayment = (i.getNextPaymentDate() == null) ? now :
                            i.getNextPaymentDate().toLocalDate().minusMonths(1).plusDays(1);
                    return new Delinquent(i.getLoanId(), delinquentPayment);
                });
        synchronized (this) { // store to the state file
            LOGGER.trace("Starting delinquency update.");
            final State.ClassSpecificState state = State.forClass(this.getClass());
            final State.Batch stateUpdate = state.newBatch(true);
            // update state of delinquents
            final Collection<Delinquency> allPresent = Stream.concat(stillDelinquent, newDelinquents)
                    .peek(d -> stateUpdate.set(String.valueOf(d.getLoanId()), toString(d)))
                    .flatMap(d -> d.getActiveDelinquency().map(Stream::of).orElse(Stream.empty()))
                    .collect(Collectors.toSet());
            stateUpdate.call(); // persist state updates
            LOGGER.trace("Delinquency update finished.");
            // and notify of new delinquencies over all known thresholds
            Stream.of(DelinquencyCategory.values()).forEach(c -> c.update(allPresent, zonky));
        }
    }

    /**
     * @return Active loans that are now, or at some point have been, currently tracked as delinquent.
     */
    public Collection<Delinquent> getDelinquents() {
        final State.ClassSpecificState state = State.forClass(this.getClass());
        return state.getKeys().stream()
                .map(key -> {
                    final int loanId = Integer.parseInt(key);
                    final String rawDelinquencies =
                            state.getValue(key).orElseThrow(() -> new IllegalStateException("Impossible."));
                    return fromString(loanId, rawDelinquencies);
                }).collect(Collectors.toSet());
    }

}
