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
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.robozonky.api.notifications.Event;
import com.github.robozonky.api.notifications.LoanDefaultedEvent;
import com.github.robozonky.api.notifications.LoanDelinquentEvent;
import com.github.robozonky.api.notifications.LoanNoLongerDelinquentEvent;
import com.github.robozonky.api.remote.entities.Investment;
import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.api.remote.enums.PaymentStatus;
import com.github.robozonky.api.remote.enums.PaymentStatuses;
import com.github.robozonky.app.Events;
import com.github.robozonky.app.authentication.Authenticated;
import com.github.robozonky.app.configuration.daemon.PortfolioDependant;
import com.github.robozonky.internal.api.Defaults;
import com.github.robozonky.internal.api.State;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Historical record of which {@link Investment}s have been delinquent and when. Instances of this class (obtained via
 * {@link #Delinquents()} update shared state (implemented via {@link State}) which can then be
 * retrieved through static methods, such as {@link #getDelinquents()} and {@link #getLastUpdateTimestamp()}.
 */
public class Delinquents implements PortfolioDependant {

    private static final Logger LOGGER = LoggerFactory.getLogger(Delinquents.class);
    private static final String LAST_UPDATE_PROPERTY_NAME = "lastUpdate";
    private static final String TIME_SEPARATOR = ":::";
    private static final Pattern TIME_SPLITTER = Pattern.compile("\\Q" + TIME_SEPARATOR + "\\E");

    private final LoanProvider loanProvider;

    /**
     * Creates an instance of this class which will use {@link PortfolioLoanProvider} to retrieve {@link Loan}s.
     */
    public Delinquents() {
        this(null);
    }

    Delinquents(final LoanProvider loanProvider) {
        this.loanProvider = loanProvider;
    }

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

    private static Collection<Investment> getWithPaymentStatus(final Portfolio portfolio,
                                                               final PaymentStatuses target) {
        return portfolio.getActiveWithPaymentStatus(target).collect(Collectors.toList());
    }

    private static boolean related(final Delinquent d, final Investment i) {
        return d.getLoanId() == i.getLoanId();
    }

    private static State.ClassSpecificState getState() {
        return State.forClass(Delinquents.class);
    }

    /**
     * Retrieve the time when the internal state of this class was modified.
     * @return {@link Instant#EPOCH} when no internal state.
     */
    public static OffsetDateTime getLastUpdateTimestamp() {
        return getState().getValue(LAST_UPDATE_PROPERTY_NAME)
                .map(OffsetDateTime::parse)
                .orElse(OffsetDateTime.ofInstant(Instant.EPOCH, Defaults.ZONE_ID));
    }

    /**
     * @return Active loans that are now, or at some point have been, tracked as delinquent.
     */
    public static Collection<Delinquent> getDelinquents() {
        final State.ClassSpecificState state = getState();
        return state.getKeys().stream()
                .filter(StringUtils::isNumeric) // skip any non-loan metadata
                .map(key -> {
                    final int loanId = Integer.parseInt(key);
                    final List<String> rawDelinquencies =
                            state.getValues(key).orElseThrow(() -> new IllegalStateException("Impossible."));
                    return add(loanId, rawDelinquencies);
                }).collect(Collectors.toSet());
    }

    private static Collection<Delinquency> store(final Stream<Delinquent> newDelinquents,
                                                 final Stream<Delinquent> stillDelinquent) {
        LOGGER.trace("Starting delinquency update.");
        final State.ClassSpecificState state = getState();
        final State.Batch stateUpdate = state.newBatch(true);
        // update state of delinquents
        final Collection<Delinquency> allPresent = Stream.concat(stillDelinquent, newDelinquents)
                .peek(d -> stateUpdate.set(String.valueOf(d.getLoanId()), toString(d)))
                .flatMap(d -> d.getActiveDelinquency().map(Stream::of).orElse(Stream.empty()))
                .collect(Collectors.toSet());
        stateUpdate.set(LAST_UPDATE_PROPERTY_NAME, OffsetDateTime.now().toString());
        stateUpdate.call(); // persist state updates
        LOGGER.trace("Delinquency update finished.");
        return allPresent;
    }

    void update(final Authenticated auth, final Collection<Investment> presentlyDelinquent,
                final LoanProvider loanProvider) {
        update(auth, presentlyDelinquent, Collections.emptyList(), loanProvider);
    }

    /**
     * Updates delinquency information based on the information about loans that are either currently delinquent or no
     * longer active. Will fire events on new delinquencies and/or on loans no longer delinquent.
     * @param auth The API that will be used to retrieve the loan instances.
     * @param presentlyDelinquent Loans that currently have overdue instalments. This corresponds to
     * {@link PaymentStatus#getDelinquent()}
     * @param noLongerActive Loans that are no longer relevant. This corresponds to {@link PaymentStatus#getDone()}.
     * @param loanProvider Used to retrieve {@link Loan} instances from Zonky.
     */
    static void update(final Authenticated auth, final Collection<Investment> presentlyDelinquent,
                       final Collection<Investment> noLongerActive, final LoanProvider loanProvider) {
        LOGGER.debug("Updating delinquent loans.");
        final LocalDate now = LocalDate.now();
        final Collection<Delinquent> knownDelinquents = getDelinquents();
        knownDelinquents.stream()
                .filter(Delinquent::hasActiveDelinquency) // only care about present delinquents
                .filter(d -> presentlyDelinquent.stream().noneMatch(i -> related(d, i)))
                .flatMap(d -> d.getActiveDelinquency().map(Stream::of).orElse(Stream.empty()))
                .peek(d -> d.setFixedOn(now.minusDays(1))) // end the delinquency
                .map(Delinquency::getParent)
                .forEach(d -> {  // notify
                    final Loan loan = auth.call(z -> loanProvider.apply(d.getLoanId(), z));
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
        final Collection<Delinquency> presentDelinquents = store(newDelinquents, stillDelinquent);
        // and notify of new delinquencies over all known thresholds
        Stream.of(DelinquencyCategory.values()).forEach(c -> c.update(presentDelinquents, auth, loanProvider));
        LOGGER.trace("Done.");
    }

    /**
     * Will use the remote server to update information about the current delinquency status of {@link Investment}s of
     * the current user. It will register {@link Investment}s that are now delinquent and were not before, and also
     * detect {@link Investment}s which were delinquent before and are now back to normal. It will also fire the
     * appropriate {@link Event}s, such as {@link LoanDelinquentEvent}, {@link LoanNoLongerDelinquentEvent} etc.
     * @param portfolio Will be used to load information about existing investments.
     * @param auth Will be used to query additional information about {@link Loan}s, using the method supplied in the
     * constructor.
     */
    @Override
    public void accept(final Portfolio portfolio, final Authenticated auth) {
        final LoanProvider p = loanProvider == null ? new PortfolioLoanProvider(portfolio) : loanProvider;
        update(auth, getWithPaymentStatus(portfolio, PaymentStatus.getDelinquent()),
               getWithPaymentStatus(portfolio, PaymentStatus.getDone()), p);
    }
}
