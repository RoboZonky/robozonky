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

import java.time.Duration;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.robozonky.api.notifications.Event;
import com.github.robozonky.api.notifications.LoanDelinquent10DaysOrMoreEvent;
import com.github.robozonky.api.notifications.LoanDelinquent30DaysOrMoreEvent;
import com.github.robozonky.api.notifications.LoanDelinquent60DaysOrMoreEvent;
import com.github.robozonky.api.notifications.LoanDelinquent90DaysOrMoreEvent;
import com.github.robozonky.api.notifications.LoanDelinquentEvent;
import com.github.robozonky.api.notifications.LoanNowDelinquentEvent;
import com.github.robozonky.api.remote.entities.Investment;
import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.app.Events;
import com.github.robozonky.internal.api.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Keeps active delinquencies over a given threshold. When a new delinquency over a particular threshold arrives, an
 * event is sent. When a delinquency is no longer among active, it is silently removed.
 */
enum DelinquencyCategory {

    NEW(0),
    MILD(10),
    SEVERE(30),
    CRITICAL(60),
    HOPELESS(90);

    @FunctionalInterface
    private interface EventSupplier {

        LoanDelinquentEvent apply(final Investment i, final Loan l, final LocalDate d);
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(DelinquencyCategory.class);
    private final int thresholdInDays;

    DelinquencyCategory(final int thresholdInDays) {
        this.thresholdInDays = thresholdInDays;
    }

    private static boolean isOverThreshold(final Delinquency d, final int threshold) {
        final Duration target = Duration.ofDays(threshold);
        final Duration actual = d.getDuration();
        return actual.compareTo(target) >= 0;
    }

    private static Stream<Integer> fromIdString(final List<String> idString) {
        return idString.stream().map(String::trim).filter(s -> s.length() > 0).map(Integer::parseInt);
    }

    private static Stream<String> toIdString(final Stream<Integer> stream) {
        return stream.distinct().sorted().map(Object::toString);
    }

    private static EventSupplier getEventSupplier(final int threshold) {
        switch (threshold) {
            case 0:
                return LoanNowDelinquentEvent::new;
            case 10:
                return LoanDelinquent10DaysOrMoreEvent::new;
            case 30:
                return LoanDelinquent30DaysOrMoreEvent::new;
            case 60:
                return LoanDelinquent60DaysOrMoreEvent::new;
            case 90:
                return LoanDelinquent90DaysOrMoreEvent::new;
            default:
                throw new IllegalArgumentException("Wrong delinquency threshold: " + threshold);
        }
    }

    private static LoanDelinquentEvent getEvent(final LocalDate since, final Investment investment, final Loan loan,
                                                final int threshold) {
        return getEventSupplier(threshold).apply(investment, loan, since);
    }

    private static String getFieldName(final int dayThreshold) {
        return "notified" + dayThreshold + "plus";
    }

    private static boolean isLoanRelated(final Delinquency d, final int loanId) {
        return d.getParent().getLoanId() == loanId;
    }

    /**
     * @return Number of days at minimum for a delinquency to belong to this category.
     */
    public int getThresholdInDays() {
        return thresholdInDays;
    }

    /**
     * Update internal state trackers and send events if necessary.
     * @param active Active delinquencies - ie. payments that are, right now, overdue.
     * @param investmentSupplier Retrieves the investment instance for a particular loan ID.
     * @param loanSupplier Retrieves the loan instance for a particular loan ID.
     * @return IDs of loans that are being tracked in this category.
     */
    public Collection<Integer> update(final Collection<Delinquency> active,
                                      final Function<Integer, Investment> investmentSupplier,
                                      final Function<Integer, Loan> loanSupplier) {
        LOGGER.trace("Updating {}.", this);
        final State.ClassSpecificState state = State.forClass(DelinquencyCategory.class);
        final String fieldName = getFieldName(thresholdInDays);
        final Collection<Integer> keepThese = state.getValues(fieldName)
                .map(DelinquencyCategory::fromIdString)
                .orElse(Stream.empty())
                .filter(id -> active.stream().anyMatch(d -> isLoanRelated(d, id)))
                .collect(Collectors.toSet());
        final Stream<Integer> addThese = active.stream()
                .filter(d -> isOverThreshold(d, thresholdInDays))
                .filter(d -> keepThese.stream().noneMatch(id -> isLoanRelated(d, id)))
                .peek(d -> {
                    final int loanId = d.getParent().getLoanId();
                    final Investment i = investmentSupplier.apply(loanId);
                    final Loan l = loanSupplier.apply(loanId);
                    final Event e = getEvent(d.getPaymentMissedDate(), i, l, thresholdInDays);
                    Events.fire(e);
                })
                .map(d -> d.getParent().getLoanId());
        final Collection<Integer> storeThese = Stream.concat(keepThese.stream(), addThese)
                .collect(Collectors.collectingAndThen(Collectors.toSet(), TreeSet::new));
        state.newBatch().set(fieldName, toIdString(storeThese.stream())).call();
        LOGGER.trace("Update over, stored {}.", storeThese);
        return storeThese;
    }

}
