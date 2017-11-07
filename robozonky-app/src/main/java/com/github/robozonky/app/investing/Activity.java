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

package com.github.robozonky.app.investing;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAmount;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;

import com.github.robozonky.api.strategies.LoanDescriptor;
import com.github.robozonky.app.configuration.daemon.MarketplaceActivity;
import com.github.robozonky.internal.api.Defaults;
import com.github.robozonky.internal.api.Settings;
import com.github.robozonky.internal.api.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class Activity implements MarketplaceActivity {

    static final State.ClassSpecificState STATE = State.forClass(Activity.class);
    static final String LAST_MARKETPLACE_CHECK_STATE_ID = "lastMarketplaceCheck";
    private static final Runnable DO_NOTHING = () -> {
    };
    private static final Logger LOGGER = LoggerFactory.getLogger(Activity.class);
    private static final OffsetDateTime EPOCH = OffsetDateTime.ofInstant(Instant.EPOCH, Defaults.ZONE_ID);
    /**
     * @see Settings#getCaptchaDelay().
     */
    private static final TemporalAmount CAPTCHA_DELAY = Settings.INSTANCE.getCaptchaDelay();
    private final TemporalAmount sleepInterval;
    private final Collection<LoanDescriptor> recentLoansDescending;
    private final AtomicReference<Runnable> settler = new AtomicReference<>(Activity.DO_NOTHING);

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

    private boolean hasLoansNewerThan(final OffsetDateTime instant) {
        return this.recentLoansDescending.stream()
                .anyMatch(l -> l.item().getDatePublished().isAfter(instant));
    }

    private boolean hasUnactionableLoans() {
        final OffsetDateTime now = OffsetDateTime.now();
        return this.recentLoansDescending.stream()
                .anyMatch(l -> l.getLoanCaptchaProtectionEndDateTime()
                        .map(captchaEnds -> captchaEnds.isAfter(now))
                        .orElse(false));
    }

    @Override
    public boolean shouldSleep() {
        final OffsetDateTime lastKnownAction = Activity.getLatestMarketplaceAction();
        final boolean hasUnactionableLoans = this.hasUnactionableLoans();
        final boolean shouldSleep;
        if (lastKnownAction.plus(this.sleepInterval).isBefore(OffsetDateTime.now())) {
            // try investing since we haven't tried in a while; maybe we have some more funds now
            shouldSleep = false;
        } else {
            // try investing if we have some unseen loans
            final boolean hasUnseenLoans = this.hasLoansNewerThan(lastKnownAction);
            shouldSleep = !(hasUnseenLoans || hasUnactionableLoans);
        }
        // only change marketplace check timestamp when we're intending to call some actual investing.
        this.settler.set(shouldSleep ? null : () -> this.persist(hasUnactionableLoans));
        return shouldSleep;
    }

    @Override
    public void settle() {
        this.settler.getAndSet(Activity.DO_NOTHING).run();
    }

    private void persist(final boolean hasUnactionableLoans) {
        // make sure the unactionable marketplace are never included in the time the marketplace was last checked
        final OffsetDateTime result = hasUnactionableLoans ?
                OffsetDateTime.now().minus(Duration.from(CAPTCHA_DELAY).plus(Duration.ofSeconds(30)))
                : OffsetDateTime.now();
        Activity.STATE.newBatch().set(Activity.LAST_MARKETPLACE_CHECK_STATE_ID, result.toString()).call();
        Activity.LOGGER.debug("New marketplace last checked time is {}.", result);
    }
}
