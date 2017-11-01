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

package com.github.robozonky.app.portfolio;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.robozonky.api.notifications.LoanDefaultedEvent;
import com.github.robozonky.api.notifications.LoanNoLongerDelinquentEvent;
import com.github.robozonky.api.remote.entities.Investment;
import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.api.remote.enums.PaymentStatus;
import com.github.robozonky.api.remote.enums.PaymentStatuses;
import com.github.robozonky.app.Events;
import com.github.robozonky.common.remote.Zonky;
import com.github.robozonky.internal.api.Defaults;
import com.github.robozonky.internal.api.State;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main entry point to the delinquency API.
 */
public class Delinquents implements Consumer<Zonky> {

    public static final Delinquents INSTANCE = new Delinquents();

    private static final Logger LOGGER = LoggerFactory.getLogger(Delinquents.class);
    private static final String LAST_UPDATE_PROPERTY_NAME = "lastUpdate";
    private static final String TIME_SEPARATOR = ":::";
    private static final Pattern TIME_SPLITTER = Pattern.compile("\\Q" + TIME_SEPARATOR + "\\E");

    private static String toString(final Delinquency d) {
        return d.getFixedOn()
                .map(fixedOn -> d.getPaymentMissedDate() + TIME_SEPARATOR + fixedOn)
                .orElse(d.getPaymentMissedDate().toString());
    }

    private static Stream<String> toString(final Delinquent d) {
        return d.getDelinquencies().map(Delinquents::toString);
    }

    private static void add(final Delinquent d, final String delinquency) {
        final String[] parts = TIME_SPLITTER.split(delinquency);
        if (parts.length == 1) {
            d.addDelinquency(LocalDate.parse(parts[0]));
        } else if (parts.length == 2) {
            d.addDelinquency(LocalDate.parse(parts[0]), LocalDate.parse(parts[1]));
        } else {
            throw new IllegalStateException("Unexpected number of dates: " + parts.length);
        }
    }

    private static Delinquent add(final int loanId, final List<String> delinquencies) {
        final Delinquent d = new Delinquent(loanId);
        delinquencies.forEach(delinquency -> add(d, delinquency));
        return d;
    }

    private static Collection<Investment> getWithPaymentStatus(final PaymentStatuses target) {
        return Portfolio.INSTANCE.getActiveWithPaymentStatus(target).collect(Collectors.toList());
    }

    private static boolean related(final Delinquent d, final Investment i) {
        return d.getLoanId() == i.getLoanId();
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
    void update(final Zonky zonky, final Collection<Investment> presentlyDelinquent,
                final Collection<Investment> noLongerActive) {
        LOGGER.debug("Updating delinquent loans.");
        final LocalDate now = LocalDate.now();
        final Collection<Delinquent> knownDelinquents = this.getDelinquents();
        knownDelinquents.stream()
                .filter(Delinquent::hasActiveDelinquency) // only care about present delinquents
                .filter(d -> presentlyDelinquent.stream().noneMatch(i -> related(d, i)))
                .flatMap(d -> d.getActiveDelinquency().map(Stream::of).orElse(Stream.empty()))
                .peek(d -> d.setFixedOn(now.minusDays(1))) // end the delinquency
                .map(Delinquency::getParent)
                .forEach(d -> {  // notify
                    final Loan loan = d.getLoan(zonky);
                    final LocalDate since = d.getLatestDelinquency().get().getPaymentMissedDate();
                    if (noLongerActive.stream().anyMatch(i -> related(d, i))) {
                        Events.fire(new LoanDefaultedEvent(loan, since));
                    } else {
                        Events.fire(new LoanNoLongerDelinquentEvent(loan, since));
                    }
                });
        final Stream<Delinquent> stillDelinquent = knownDelinquents.stream()
                .filter(d -> noLongerActive.stream().noneMatch(i -> related(d, i)));
        final Stream<Delinquent> newDelinquents = presentlyDelinquent.stream()
                .filter(i -> knownDelinquents.stream().noneMatch(d -> related(d, i)))
                .map(i -> new Delinquent(i.getLoanId(), i.getNextPaymentDate().toLocalDate()));
        synchronized (this) { // store to the state file
            LOGGER.trace("Starting delinquency update.");
            final State.ClassSpecificState state = State.forClass(this.getClass());
            final State.Batch stateUpdate = state.newBatch(true);
            // update state of delinquents
            final Collection<Delinquency> allPresent = Stream.concat(stillDelinquent, newDelinquents)
                    .peek(d -> stateUpdate.set(String.valueOf(d.getLoanId()), toString(d)))
                    .flatMap(d -> d.getActiveDelinquency().map(Stream::of).orElse(Stream.empty()))
                    .collect(Collectors.toSet());
            stateUpdate.set(LAST_UPDATE_PROPERTY_NAME, OffsetDateTime.now().toString());
            stateUpdate.call(); // persist state updates
            LOGGER.trace("Delinquency update finished.");
            // and notify of new delinquencies over all known thresholds
            Stream.of(DelinquencyCategory.values()).forEach(c -> c.update(allPresent, zonky));
        }
        LOGGER.trace("Done.");
    }

    public OffsetDateTime getLastUpdateTimestamp() {
        return State.forClass(this.getClass())
                .getValue(LAST_UPDATE_PROPERTY_NAME)
                .map(OffsetDateTime::parse)
                .orElse(OffsetDateTime.ofInstant(Instant.EPOCH, Defaults.ZONE_ID));
    }

    /**
     * @return Active loans that are now, or at some point have been, currently tracked as delinquent.
     */
    public Collection<Delinquent> getDelinquents() {
        final State.ClassSpecificState state = State.forClass(this.getClass());
        return state.getKeys().stream()
                .filter(StringUtils::isNumeric) // skip any non-loan metadata
                .map(key -> {
                    final int loanId = Integer.parseInt(key);
                    final List<String> rawDelinquencies =
                            state.getValues(key).orElseThrow(() -> new IllegalStateException("Impossible."));
                    return add(loanId, rawDelinquencies);
                }).collect(Collectors.toSet());
    }

    @Override
    public void accept(final Zonky zonky) {
        update(zonky, getWithPaymentStatus(PaymentStatus.getDelinquent()),
               getWithPaymentStatus(PaymentStatus.getDone()));
    }
}
