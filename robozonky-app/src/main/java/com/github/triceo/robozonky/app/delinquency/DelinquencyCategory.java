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

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAmount;
import java.util.Collection;
import java.util.function.BiFunction;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.triceo.robozonky.api.notifications.LoanDelinquent10DaysOrMoreEvent;
import com.github.triceo.robozonky.api.notifications.LoanDelinquent30DaysOrMoreEvent;
import com.github.triceo.robozonky.api.notifications.LoanDelinquent60DaysOrMoreEvent;
import com.github.triceo.robozonky.api.notifications.LoanDelinquent90DaysOrMoreEvent;
import com.github.triceo.robozonky.api.notifications.LoanDelinquentEvent;
import com.github.triceo.robozonky.api.notifications.LoanNowDelinquentEvent;
import com.github.triceo.robozonky.api.remote.entities.Loan;
import com.github.triceo.robozonky.app.Events;
import com.github.triceo.robozonky.common.remote.Zonky;
import com.github.triceo.robozonky.internal.api.State;

/**
 * Keeps active delinquencies over a given threshold. When a new delinquency over a particular threshold arrives, an
 * event is sent. When a delinquency is no longer among active, it is silently removed.
 */
enum DelinquencyCategory {

    NEW(0),
    MILD(10),
    SEVERE(30),
    CRITICAL(60),
    DEFAULTED(90);

    private static final String COMMA = ",";
    private static final Pattern SPLIT_BY_COMMA = Pattern.compile("\\Q" + COMMA + "\\E");
    private final int thresholdInDays;

    DelinquencyCategory(final int thresholdInDays) {
        this.thresholdInDays = thresholdInDays;
    }

    private static boolean isOverThreshold(final Delinquency d, final int threshold) {
        final TemporalAmount target = Duration.ofDays(threshold);
        final TemporalAmount actual = d.getDuration();
        return actual.get(ChronoUnit.SECONDS) >= target.get(ChronoUnit.SECONDS);
    }

    private static Stream<Integer> fromIdString(final String idString) {
        return Stream.of(SPLIT_BY_COMMA.split(idString))
                .map(String::trim)
                .filter(s -> s.length() > 0)
                .map(Integer::parseInt);
    }

    private static String toIdString(final Stream<Integer> stream) {
        return stream.distinct().sorted().map(Object::toString).collect(Collectors.joining(COMMA));
    }

    private static BiFunction<Loan, OffsetDateTime, LoanDelinquentEvent> getEventSupplier(final int threshold) {
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

    private static LoanDelinquentEvent getEvent(final Delinquency d, final int threshold, final Zonky z) {
        final Loan loan = d.getParent().getLoan(z);
        final OffsetDateTime since = d.getDetectedOn();
        return getEventSupplier(threshold).apply(loan, since);
    }

    private static String getFieldName(final int dayThreshold) {
        return "notified" + dayThreshold + "plus";
    }

    /**
     * @return Number of days at minimum for a delinquency to belong to this category.
     */
    public int getThresholdInDays() {
        return thresholdInDays;
    }

    /**
     * Update internal state trackers and send events if necessary.
     *
     * @param delinquencies Active delinquencies - ie. payments that are, right now, overdue.
     * @param zonky Authenticated API to be used for retrieving loan data.
     * @return IDs of loans that are being tracked in this category.
     */
    public Collection<Integer> update(final Collection<Delinquency> delinquencies, final Zonky zonky) {
        final Collection<Delinquency> activeAndPresnet = delinquencies.stream()
                .filter(d -> !d.getFixedOn().isPresent())
                .collect(Collectors.toSet());
        final State.ClassSpecificState state = State.INSTANCE.forClass(this.getClass());
        final String fieldName = getFieldName(thresholdInDays);
        final Collection<Integer> activeHistorical = state.getValue(fieldName)
                .map(idString -> fromIdString(idString))
                .orElse(Stream.empty())
                .filter(id -> activeAndPresnet.stream().anyMatch(d -> d.getParent().getLoanId() == id))
                .collect(Collectors.toSet());
        final Stream<Integer> newFound = activeAndPresnet.stream()
                .filter(d -> isOverThreshold(d, thresholdInDays))
                .filter(d -> activeHistorical.stream().noneMatch(i -> d.getParent().getLoanId() == i))
                .peek(d -> Events.fire(getEvent(d, thresholdInDays, zonky)))
                .map(d -> d.getParent().getLoanId());
        final Collection<Integer> result = Stream.concat(activeHistorical.stream(), newFound)
                .sorted()
                .collect(Collectors.toSet());
        state.setValue(fieldName, toIdString(result.stream()));
        return result;
    }

}
