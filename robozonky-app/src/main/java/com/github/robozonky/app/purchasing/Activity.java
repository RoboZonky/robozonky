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

package com.github.robozonky.app.purchasing;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAmount;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import com.github.robozonky.api.remote.entities.Participation;
import com.github.robozonky.app.configuration.daemon.MarketplaceActivity;
import com.github.robozonky.internal.api.Defaults;
import com.github.robozonky.internal.api.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class Activity implements MarketplaceActivity {

    private static SortedSet<Integer> serialize(final Collection<Participation> items) {
        final Set<Integer> result = items.stream().map(Participation::getInvestmentId).collect(Collectors.toSet());
        return new TreeSet<>(result);
    }

    private static SortedSet<Integer> read() {
        return STATE.getValues(LAST_MARKETPLACE_STATE_ID)
                .map(source -> {
                    final Set<Integer> values = source.stream()
                            .map(s -> Integer.parseInt(s.trim()))
                            .collect(Collectors.toSet());
                    return (SortedSet<Integer>) new TreeSet<>(values);
                })
                .orElse(Collections.emptySortedSet());
    }

    private static final Runnable DO_NOTHING = () -> {
    };
    private static final Logger LOGGER = LoggerFactory.getLogger(Activity.class);
    private static final OffsetDateTime EPOCH = OffsetDateTime.ofInstant(Instant.EPOCH, Defaults.ZONE_ID);
    static final State.ClassSpecificState STATE = State.forClass(Activity.class);
    static final String LAST_MARKETPLACE_CHECK_STATE_ID = "lastMarketplaceCheck",
            LAST_MARKETPLACE_STATE_ID = "lastMarketplace";

    private final TemporalAmount sleepInterval;
    private final Collection<Participation> currentMarketplace;
    private final AtomicReference<Runnable> settler = new AtomicReference<>(Activity.DO_NOTHING);

    Activity(final Collection<Participation> items) {
        this(items, Duration.ofMinutes(60));
    }

    public Activity(final Collection<Participation> items, final TemporalAmount maximumSleepPeriod) {
        this.sleepInterval = maximumSleepPeriod;
        this.currentMarketplace = new ArrayList<>(items);
    }

    static OffsetDateTime getLatestMarketplaceAction() {
        return STATE.getValue(LAST_MARKETPLACE_CHECK_STATE_ID).map(s -> {
            try {
                return OffsetDateTime.parse(s);
            } catch (final DateTimeParseException ex) {
                LOGGER.debug("Failed read marketplace timestamp.", ex);
                return EPOCH;
            }
        }).orElse(EPOCH);
    }

    private boolean hasNewLoans() {
        final SortedSet<Integer> state = read();
        final SortedSet<Integer> now = serialize(currentMarketplace);
        now.removeAll(state);
        return !now.isEmpty();
    }

    @Override
    public boolean shouldSleep() {
        final OffsetDateTime lastKnownAction = Activity.getLatestMarketplaceAction();
        final boolean shouldSleep;
        if (lastKnownAction.plus(sleepInterval).isBefore(OffsetDateTime.now())) {
            // try investing since we haven't tried in a while; maybe we have some more funds now
            shouldSleep = false;
        } else {
            // try investing if we have some unseen participations
            shouldSleep = !this.hasNewLoans();
        }
        // only change marketplace check timestamp when we're intending to call some actual investing.
        settler.set(shouldSleep ? null : this::persist);
        return shouldSleep;
    }

    @Override
    public void settle() {
        this.settler.getAndSet(Activity.DO_NOTHING).run();
    }

    private void persist() {
        final OffsetDateTime result = OffsetDateTime.now();
        final State.Batch b = STATE.newBatch();
        b.set(LAST_MARKETPLACE_CHECK_STATE_ID, result.toString());
        if (currentMarketplace.size() == 0) {
            b.unset(LAST_MARKETPLACE_STATE_ID);
        } else {
            b.set(LAST_MARKETPLACE_STATE_ID, serialize(currentMarketplace).stream().map(String::valueOf));
        }
        b.call();
        LOGGER.debug("New marketplace last checked time is {}.", result);
    }
}
