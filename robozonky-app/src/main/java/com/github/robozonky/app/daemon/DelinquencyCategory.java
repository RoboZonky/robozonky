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

package com.github.robozonky.app.daemon;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
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
import com.github.robozonky.app.events.EventFactory;
import com.github.robozonky.app.events.LazyEvent;
import com.github.robozonky.common.Tenant;
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

    private static EventSupplier getEventSupplierConstructor(final int threshold) {
        switch (threshold) {
            case -1:
                return EventFactory::loanDefaulted;
            case 0:
                return EventFactory::loanNowDelinquent;
            case 10:
                return EventFactory::loanDelinquent10plus;
            case 30:
                return EventFactory::loanDelinquent30plus;
            case 60:
                return EventFactory::loanDelinquent60plus;
            case 90:
                return EventFactory::loanDelinquent90plus;
            default:
                throw new IllegalArgumentException("Wrong delinquency threshold: " + threshold);
        }
    }

    @SuppressWarnings("unchecked")
    private static LazyEvent<? extends Event> getLazyEventSupplier(final int threshold,
                                                                   final Supplier<? extends Event> event) {
        switch (threshold) {
            case -1:
                return EventFactory.loanDefaultedLazy((Supplier<LoanDefaultedEvent>) event);
            case 0:
                return EventFactory.loanNowDelinquentLazy((Supplier<LoanNowDelinquentEvent>) event);
            case 10:
                return EventFactory.loanDelinquent10plusLazy((Supplier<LoanDelinquent10DaysOrMoreEvent>) event);
            case 30:
                return EventFactory.loanDelinquent30plusLazy((Supplier<LoanDelinquent30DaysOrMoreEvent>) event);
            case 60:
                return EventFactory.loanDelinquent60plusLazy((Supplier<LoanDelinquent60DaysOrMoreEvent>) event);
            case 90:
                return EventFactory.loanDelinquent90plusLazy((Supplier<LoanDelinquent90DaysOrMoreEvent>) event);
            default:
                throw new IllegalArgumentException("Wrong delinquency threshold: " + threshold);
        }
    }

    private static LazyEvent<? extends Event> getEvent(final Tenant tenant, final Investment investment,
                                                       final int threshold) {
        LOGGER.trace("Retrieving event for investment #{}.", investment.getId());
        final LocalDate since = LocalDate.now().minusDays(investment.getDaysPastDue());
        final int loanId = investment.getLoanId();
        final Collection<Development> developments = getDevelopments(tenant, loanId, since);
        final Loan loan = LoanCache.get().getLoan(loanId, tenant);
        final Supplier<Event> s = () -> getEventSupplierConstructor(threshold).apply(investment, loan, since,
                                                                                     developments);
        final LazyEvent<? extends Event> e = getLazyEventSupplier(threshold, s);
        LOGGER.trace("Done.");
        return e;
    }

    private static String getFieldName(final int dayThreshold) {
        return "notified" + dayThreshold + "plus";
    }

    private static List<Development> getDevelopments(final Tenant auth, final int loanId,
                                                     final LocalDate delinquentSince) {
        final LocalDate lastNonDelinquentDay = delinquentSince.minusDays(1);
        final List<Development> developments = auth.call(z -> z.getDevelopments(loanId))
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
     * @param delinquents Active delinquencies - ie. payments that are, right now, overdue.
     * @return IDs of loans that are being tracked in this category.
     */
    public int[] update(final Transactional transactional, final Collection<Investment> delinquents) {
        LOGGER.debug("Updating {}.", this);
        final Tenant tenant = transactional.getTenant();
        final InstanceState<DelinquencyCategory> transactionalState = tenant.getState(DelinquencyCategory.class);
        final String fieldName = getFieldName(thresholdInDays);
        final Set<Integer> keepThese = transactionalState.getValues(fieldName)
                .map(DelinquencyCategory::fromIdString)
                .orElse(IntStream.empty())
                .filter(id -> delinquents.stream().anyMatch(d -> d.getLoanId() == id)) // remove no longer delinquent
                .boxed()
                .collect(Collectors.toSet());
        LOGGER.trace("Keeping {}.", keepThese);
        final IntStream addThese = delinquents.stream()
                .parallel() // there is potentially a lot of these, and each involves several REST requests
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
