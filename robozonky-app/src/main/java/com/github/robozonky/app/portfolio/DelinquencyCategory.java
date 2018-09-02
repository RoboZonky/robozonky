/*
 * Copyright 2018 The RoboZonky Project
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

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.github.robozonky.api.notifications.Event;
import com.github.robozonky.api.notifications.LoanDefaultedEvent;
import com.github.robozonky.api.notifications.LoanDelinquent10DaysOrMoreEvent;
import com.github.robozonky.api.notifications.LoanDelinquent30DaysOrMoreEvent;
import com.github.robozonky.api.notifications.LoanDelinquent60DaysOrMoreEvent;
import com.github.robozonky.api.notifications.LoanDelinquent90DaysOrMoreEvent;
import com.github.robozonky.api.notifications.LoanNowDelinquentEvent;
import com.github.robozonky.api.remote.entities.sanitized.Development;
import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.app.authentication.Tenant;
import com.github.robozonky.app.configuration.daemon.Transactional;
import com.github.robozonky.app.util.LoanCache;
import com.github.robozonky.common.state.InstanceState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.stream.Collectors.toList;

/**
 * Keeps active delinquencies over a given threshold. When a new delinquency over a particular threshold arrives, an
 * event is sent. When a delinquency is no longer among active, it is silently removed.
 */
enum DelinquencyCategory {

    NEW(0),
    MILD(10),
    SEVERE(30),
    CRITICAL(60),
    HOPELESS(90),
    /**
     * This has a bit of a special meaning. Any delinquent can be both defaulted and either new, mild, severe, critical
     * or hopeless.
     */
    DEFAULTED(-1);

    private static final Logger LOGGER = LoggerFactory.getLogger(DelinquencyCategory.class);
    private final int thresholdInDays;

    DelinquencyCategory(final int thresholdInDays) {
        this.thresholdInDays = thresholdInDays;
    }

    private static boolean isOverThreshold(final Investment d, final int threshold) {
        return d.getDaysPastDue() >= threshold;
    }

    private static IntStream fromIdString(final Stream<String> idString) {
        return idString.mapToInt(Integer::parseInt);
    }

    private static Stream<String> toIdString(final int... ids) {
        return IntStream.of(ids).distinct().sorted().mapToObj(Integer::toString);
    }

    private static EventSupplier getEventSupplier(final int threshold) {
        switch (threshold) {
            case -1:
                return LoanDefaultedEvent::new;
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

    private static Event getEvent(final Tenant tenant, final Investment investment, final int threshold) {
        final Loan loan = LoanCache.INSTANCE.getLoan(investment.getLoanId(), tenant);
        final LocalDate since = LocalDate.now().minusDays(investment.getDaysPastDue());
        return getEventSupplier(threshold).apply(investment, loan, since, getDevelopments(tenant, loan, since));
    }

    private static String getFieldName(final int dayThreshold) {
        return "notified" + dayThreshold + "plus";
    }

    private static List<Development> getDevelopments(final Tenant auth, final Loan loan,
                                                     final LocalDate delinquentSince) {
        final LocalDate lastNonDelinquentDay = delinquentSince.minusDays(1);
        final List<Development> developments = auth.call(z -> z.getDevelopments(loan))
                .filter(d -> d.getDateFrom().toLocalDate().isAfter(lastNonDelinquentDay))
                .collect(toList());
        Collections.reverse(developments);
        return developments;
    }

    /**
     * @return Number of days at minimum for a delinquency to belong to this category.
     */
    public int getThresholdInDays() {
        return thresholdInDays;
    }

    /**
     * Update internal state trackers and send events if necessary.
     * @param transactional Portfolio to update.
     * @param active Active delinquencies - ie. payments that are, right now, overdue.
     * @return IDs of loans that are being tracked in this category.
     */
    public int[] update(final Transactional transactional, final Collection<Investment> active) {
        LOGGER.trace("Updating {}.", this);
        final Tenant tenant = transactional.getTenant();
        final InstanceState<DelinquencyCategory> transactionalState = tenant.getState(DelinquencyCategory.class);
        final String fieldName = getFieldName(thresholdInDays);
        final Set<Integer> keepThese = transactionalState.getValues(fieldName)
                .map(DelinquencyCategory::fromIdString)
                .orElse(IntStream.empty())
                .filter(id -> active.stream().anyMatch(d -> d.getLoanId() == id)) // remove no longer delinquent
                .boxed()
                .collect(Collectors.toSet());
        LOGGER.trace("Keeping {}.", keepThese);
        final IntStream addThese = active.stream()
                .filter(d -> !keepThese.contains(d.getLoanId())) // this delinquency is newly over the threshold
                .filter(d -> isOverThreshold(d, thresholdInDays))
                .peek(d -> transactional.fire(getEvent(tenant, d, thresholdInDays)))
                .mapToInt(Investment::getLoanId);
        final int[] storeThese = IntStream.concat(keepThese.stream().mapToInt(i -> i), addThese).toArray();
        transactionalState.update(b -> b.put(fieldName, toIdString(storeThese)));
        LOGGER.trace("Update over, stored {}.", storeThese);
        return storeThese;
    }

    @FunctionalInterface
    private interface EventSupplier {

        Event apply(final Investment i, final Loan l, final LocalDate d, final Collection<Development> collections);
    }

}
