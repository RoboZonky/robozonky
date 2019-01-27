/*
 * Copyright 2019 The RoboZonky Project
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

package com.github.robozonky.app.delinquencies;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.github.robozonky.api.notifications.LoanDefaultedEvent;
import com.github.robozonky.api.notifications.LoanDelinquent10DaysOrMoreEvent;
import com.github.robozonky.api.notifications.LoanDelinquent30DaysOrMoreEvent;
import com.github.robozonky.api.notifications.LoanDelinquent60DaysOrMoreEvent;
import com.github.robozonky.api.notifications.LoanDelinquent90DaysOrMoreEvent;
import com.github.robozonky.api.notifications.LoanNowDelinquentEvent;
import com.github.robozonky.api.notifications.SessionEvent;
import com.github.robozonky.api.remote.entities.sanitized.Development;
import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.app.events.impl.EventFactory;
import com.github.robozonky.app.tenant.PowerTenant;
import com.github.robozonky.common.tenant.LazyEvent;
import com.github.robozonky.common.tenant.Tenant;
import com.github.robozonky.internal.util.DateUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static java.util.stream.Collectors.toList;

/**
 * Keeps active delinquencies over a given threshold. When a new delinquency over a particular threshold arrives, an
 * event is sent. When a delinquency is no longer among active, it is silently removed.
 */
enum Category {

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

    private static final Logger LOGGER = LogManager.getLogger(Category.class);
    private final int thresholdInDays;

    Category(final int thresholdInDays) {
        this.thresholdInDays = thresholdInDays;
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
    private static LazyEvent<? extends SessionEvent> getLazyEventSupplier(final int threshold,
                                                                          final Supplier<? extends SessionEvent> e) {
        switch (threshold) {
            case -1:
                return EventFactory.loanDefaultedLazy((Supplier<LoanDefaultedEvent>) e);
            case 0:
                return EventFactory.loanNowDelinquentLazy((Supplier<LoanNowDelinquentEvent>) e);
            case 10:
                return EventFactory.loanDelinquent10plusLazy((Supplier<LoanDelinquent10DaysOrMoreEvent>) e);
            case 30:
                return EventFactory.loanDelinquent30plusLazy((Supplier<LoanDelinquent30DaysOrMoreEvent>) e);
            case 60:
                return EventFactory.loanDelinquent60plusLazy((Supplier<LoanDelinquent60DaysOrMoreEvent>) e);
            case 90:
                return EventFactory.loanDelinquent90plusLazy((Supplier<LoanDelinquent90DaysOrMoreEvent>) e);
            default:
                throw new IllegalArgumentException("Wrong delinquency threshold: " + threshold);
        }
    }

    private static SessionEvent supplyEvent(final Tenant tenant, final Investment investment, final int threshold) {
        LOGGER.trace("Retrieving event for investment #{}.", investment.getId());
        final LocalDate since = DateUtil.localNow().toLocalDate().minusDays(investment.getDaysPastDue());
        final int loanId = investment.getLoanId();
        final Collection<Development> developments = getDevelopments(tenant, loanId, since);
        final Loan loan = tenant.getLoan(loanId);
        final SessionEvent e = getEventSupplierConstructor(threshold)
                .apply(investment, loan, since, developments);
        LOGGER.trace("Done.");
        return e;
    }

    private static LazyEvent<? extends SessionEvent> getEvent(final Tenant tenant, final Investment investment,
                                                              final int threshold) {
        return getLazyEventSupplier(threshold, () -> supplyEvent(tenant, investment, threshold));
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

    public int getThresholdInDays() {
        return thresholdInDays;
    }

    public Stream<Category> getLesser() {
        return Arrays.stream(Category.values())
                .filter(category -> category.thresholdInDays < this.thresholdInDays && category.thresholdInDays >= 0);
    }

    /**
     * Update internal state trackers and send events if necessary.
     * @param tenant Tenant to execute over.
     * @param investment Investment to process.
     */
    public void process(final PowerTenant tenant, final Investment investment) {
        LOGGER.debug("Updating {}.", this);
        tenant.fire(getEvent(tenant, investment, thresholdInDays));
    }

    @FunctionalInterface
    private interface EventSupplier {

        SessionEvent apply(final Investment i, final Loan l, final LocalDate d,
                           final Collection<Development> collections);
    }

}
