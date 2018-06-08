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
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.github.robozonky.api.notifications.Event;
import com.github.robozonky.api.notifications.LoanDelinquent10DaysOrMoreEvent;
import com.github.robozonky.api.notifications.LoanDelinquent30DaysOrMoreEvent;
import com.github.robozonky.api.notifications.LoanDelinquent60DaysOrMoreEvent;
import com.github.robozonky.api.notifications.LoanDelinquent90DaysOrMoreEvent;
import com.github.robozonky.api.notifications.LoanDelinquentEvent;
import com.github.robozonky.api.notifications.LoanNowDelinquentEvent;
import com.github.robozonky.api.remote.entities.sanitized.Development;
import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.app.Events;
import com.github.robozonky.app.authentication.Tenant;
import com.github.robozonky.common.state.InstanceState;
import com.github.robozonky.internal.api.Defaults;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(DelinquencyCategory.class);
    private final int thresholdInDays;

    DelinquencyCategory(final int thresholdInDays) {
        this.thresholdInDays = thresholdInDays;
    }

    private static boolean isOverThreshold(final Investment d, final int threshold) {
        final Duration target = Duration.ofDays(threshold);
        final OffsetDateTime since = getPaymentMissedDate(d).atStartOfDay(Defaults.ZONE_ID).toOffsetDateTime();
        final Duration actual = Duration.between(since, OffsetDateTime.now()).abs();
        return actual.compareTo(target) >= 0;
    }

    private static IntStream fromIdString(final Stream<String> idString) {
        return idString.map(String::trim).filter(s -> s.length() > 0).mapToInt(Integer::parseInt);
    }

    private static Stream<String> toIdString(final int[] ids) {
        return IntStream.of(ids).mapToObj(Integer::toString);
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
                                                final int threshold, final Collection<Development> collections) {
        return getEventSupplier(threshold).apply(investment, loan, since, collections);
    }

    private static String getFieldName(final int dayThreshold) {
        return "notified" + dayThreshold + "plus";
    }

    private static boolean isRelated(final Investment i, final int loanId) {
        return i.getLoanId() == loanId;
    }

    private static LocalDate getPaymentMissedDate(final Investment i) {
        return i.getNextPaymentDate()
                .orElseThrow(() -> new IllegalStateException("Unexpected missing date: " + i.getId()))
                .toLocalDate();
    }

    /**
     * @return Number of days at minimum for a delinquency to belong to this category.
     */
    public int getThresholdInDays() {
        return thresholdInDays;
    }

    /**
     * Update internal state trackers and send events if necessary.
     * @param tenant Session identifier.
     * @param active Active delinquencies - ie. payments that are, right now, overdue.
     * @param loanSupplier Retrieves the loan instance for a particular loan ID.
     * @return IDs of loans that are being tracked in this category.
     */
    public int[] update(final Tenant tenant, final Collection<Investment> active,
                        final Function<Integer, Loan> loanSupplier,
                        final BiFunction<Loan, LocalDate, Collection<Development>> collectionsSupplier) {
        LOGGER.trace("Updating {}.", this);
        final InstanceState<DelinquencyCategory> state = tenant.getState(DelinquencyCategory.class);
        final String fieldName = getFieldName(thresholdInDays);
        final int[] keepThese = state.getValues(fieldName)
                .map(DelinquencyCategory::fromIdString)
                .orElse(IntStream.empty())
                .filter(id -> active.stream().anyMatch(d -> isRelated(d, id)))
                .toArray();
        LOGGER.trace("Keeping {}.", keepThese);
        final IntStream addThese = active.stream()
                .filter(d -> isOverThreshold(d, thresholdInDays))
                .filter(d -> IntStream.of(keepThese).noneMatch(id -> isRelated(d, id)))
                .peek(d -> {
                    final int loanId = d.getLoanId();
                    final Loan l = loanSupplier.apply(loanId);
                    final LocalDate since = getPaymentMissedDate(d);
                    final Event e = getEvent(since, d, l, thresholdInDays, collectionsSupplier.apply(l, since));
                    Events.fire(e);
                })
                .mapToInt(Investment::getLoanId);
        final int[] storeThese = IntStream.concat(IntStream.of(keepThese), addThese).distinct().sorted().toArray();
        state.update(b -> b.put(fieldName, toIdString(storeThese)));
        LOGGER.trace("Update over, stored {}.", storeThese);
        return storeThese;
    }

    @FunctionalInterface
    private interface EventSupplier {

        LoanDelinquentEvent apply(final Investment i, final Loan l, final LocalDate d,
                                  final Collection<Development> collections);
    }

}
