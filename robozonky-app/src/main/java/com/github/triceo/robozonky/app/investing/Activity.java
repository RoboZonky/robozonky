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

package com.github.triceo.robozonky.app.investing;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAmount;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.github.triceo.robozonky.api.strategies.LoanDescriptor;
import com.github.triceo.robozonky.internal.api.Defaults;
import com.github.triceo.robozonky.internal.api.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Decides whether or not the application should fall asleep because of general marketplace inactivity. Uses two sources
 * of data to make the decision: the marketplace, and the app's internal state concerning the last time the marketplace
 * was checked.
 *
 * In order for the state to be persisted, the App needs to eventually call {@link #settle()} after calling
 * {@link #shouldSleep()}.
 */
class Activity {


    private static final Logger LOGGER = LoggerFactory.getLogger(Activity.class);
    private static final OffsetDateTime EPOCH = OffsetDateTime.ofInstant(Instant.EPOCH, Defaults.ZONE_ID);
    static final State.ClassSpecificState STATE = State.INSTANCE.forClass(Activity.class);
    static final String LAST_MARKETPLACE_CHECK_STATE_ID = "lastMarketplaceCheck";

    private final TemporalAmount sleepInterval;
    private final Collection<LoanDescriptor> recentLoansDescending;
    private Runnable settler = null;

    Activity(final Collection<LoanDescriptor> loans) {
        this(loans, Duration.ofMinutes(60));
    }

    public Activity(final Collection<LoanDescriptor> loans, final TemporalAmount maximumSleepPeriod) {
        this.sleepInterval = maximumSleepPeriod;
        this.recentLoansDescending = new ArrayList<>(loans);
    }

    static OffsetDateTime getLatestMarketplaceAction() {
        return Activity.STATE.getValue(Activity.LAST_MARKETPLACE_CHECK_STATE_ID).map(s -> {
            try {
                return OffsetDateTime.parse(s);
            } catch (final DateTimeParseException ex) {
                Activity.LOGGER.debug("Failed read marketplace timestamp.", ex);
                return Activity.EPOCH;
            }
        }).orElse(Activity.EPOCH);
    }

    /**
     * Retrieve all loans in the marketplace which have not yet been fully funded and which have been published past
     * a certain point in time.
     * @param instant The earliest point in time for the loans to published on.
     * @return Ordered by publishing time descending.
     */
    private Collection<LoanDescriptor> getLoansNewerThan(final OffsetDateTime instant) {
        return this.recentLoansDescending.stream()
                .filter(l -> l.getLoan().getDatePublished().isAfter(instant))
                .collect(Collectors.toList());
    }

    private List<LoanDescriptor> getUnactionableLoans() {
        return this.recentLoansDescending.stream()
                .filter(l -> l.getLoanCaptchaProtectionEndDateTime()
                        .map(captchaEnds -> captchaEnds.isAfter(OffsetDateTime.now()))
                        .orElse(false))
                .collect(Collectors.toList());
    }

    /**
     * Whether or not the application should fall asleep and not make any further contact with API.
     *
     * @return True if no further contact should be made during this run of the app.
     */
    public boolean shouldSleep() {
        final OffsetDateTime lastKnownAction = Activity.getLatestMarketplaceAction();
        final boolean hasUnactionableLoans = !this.getUnactionableLoans().isEmpty();
        boolean shouldSleep = true;
        final Collection<LoanDescriptor> newLoans = this.getLoansNewerThan(lastKnownAction);
        if (!newLoans.isEmpty()) {
            // try investing since there are loans we haven't seen yet
            Activity.LOGGER.debug("Will not sleep due to new loans: {}.", newLoans.stream()
                    .map(l -> String.valueOf(l.getLoan().getId()))
                    .sorted()
                    .collect(Collectors.joining(",")));
            shouldSleep = false;
        } else if (lastKnownAction.plus(this.sleepInterval).isBefore(OffsetDateTime.now())) {
            // try investing since we haven't tried in a while; maybe we have some more funds now
            Activity.LOGGER.debug("Will not sleep due to already sleeping too much.");
            shouldSleep = false;
        }
        synchronized (this) { // do not allow concurrent modification of the settler variable
            if (!shouldSleep || hasUnactionableLoans) {
                /*
                 * only persist (= change marketplace check timestamp) when we're intending to execute some actual
                 * investing.
                 */
                this.settler = () -> this.persist(hasUnactionableLoans);
            } else {
                this.settler = null;
            }
        }
        return shouldSleep;
    }

    /**
     * Persists the new marketplace state following a {@link #shouldSleep()} call.
     */
    public synchronized void settle() {
        if (this.settler == null) {
            return;
        }
        this.settler.run();
        this.settler = null;
    }

    private void persist(final boolean hasUnactionableLoans) {
        // make sure the unactionable loans are never included in the time the marketplace was last checked
        final OffsetDateTime result = hasUnactionableLoans ?
                OffsetDateTime.now().minus(Duration.from(ResultTracker.CAPTCHA_DELAY).plus(Duration.ofSeconds(30)))
                : OffsetDateTime.now();
        if (hasUnactionableLoans) {
            Activity.LOGGER.debug("New marketplace last checked time placed before beginning of closed season: {}.",
                    result);
        } else {
            Activity.LOGGER.debug("New marketplace last checked time is {}.", result);
        }
        Activity.STATE.setValue(Activity.LAST_MARKETPLACE_CHECK_STATE_ID, result.toString());
    }
}
