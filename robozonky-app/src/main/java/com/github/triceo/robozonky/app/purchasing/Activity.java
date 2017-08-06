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

package com.github.triceo.robozonky.app.purchasing;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAmount;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.triceo.robozonky.api.strategies.ParticipationDescriptor;
import com.github.triceo.robozonky.internal.api.Defaults;
import com.github.triceo.robozonky.internal.api.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Decides whether or not the application should fall asleep because of general marketplace inactivity. Uses two sources
 * of data to make the decision: the marketplace, and the app's internal state concerning the last time the marketplace
 * was checked.
 * <p>
 * In order for the state to be persisted, the App needs to eventually call {@link #settle()} after calling
 * {@link #shouldSleep()}.
 */
class Activity {

    private static final String COMMA = ",";
    private static final Pattern COMMA_PATTERN = Pattern.compile("\\Q" + COMMA + "\\E");

    private static SortedSet<Integer> serialize(final Collection<ParticipationDescriptor> items) {
        final Set<Integer> result = items.stream().map(i -> i.item().getInvestmentId()).collect(Collectors.toSet());
        return new TreeSet<>(result);
    }

    private static String prepare(final Collection<ParticipationDescriptor> items) {
        return serialize(items).stream()
                .map(String::valueOf)
                .collect(Collectors.joining(COMMA));
    }

    private static SortedSet<Integer> read() {
        final String source = STATE.getValue(LAST_MARKETPLACE_STATE_ID).orElse("");
        final Set<Integer> result = Stream.of(COMMA_PATTERN.split(source))
                .map(s -> Integer.parseInt(s.trim()))
                .collect(Collectors.toSet());
        return new TreeSet<>(result);
    }

    private static final Runnable DO_NOTHING = () -> {
    };
    private static final Logger LOGGER = LoggerFactory.getLogger(Activity.class);
    private static final OffsetDateTime EPOCH = OffsetDateTime.ofInstant(Instant.EPOCH, Defaults.ZONE_ID);
    static final State.ClassSpecificState STATE = State.forClass(Activity.class);
    static final String LAST_MARKETPLACE_CHECK_STATE_ID = "lastMarketplaceCheck",
            LAST_MARKETPLACE_STATE_ID = "lastMarketplace";

    private final TemporalAmount sleepInterval;
    private final Collection<ParticipationDescriptor> recentDescending;
    private final AtomicReference<Runnable> settler = new AtomicReference<>(Activity.DO_NOTHING);

    Activity(final Collection<ParticipationDescriptor> items) {
        this(items, Duration.ofMinutes(60));
    }

    public Activity(final Collection<ParticipationDescriptor> items, final TemporalAmount maximumSleepPeriod) {
        this.sleepInterval = maximumSleepPeriod;
        this.recentDescending = new ArrayList<>(items);
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
        final SortedSet<Integer> now = serialize(recentDescending);
        return !Objects.equals(state, now);
    }

    /**
     * Whether or not the application should fall asleep and not make any further contact with API.
     * @return True if no further contact should be made during this run of the app.
     */
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

    /**
     * Persists the new marketplace state following a {@link #shouldSleep()} call.
     */
    public void settle() {
        this.settler.getAndSet(Activity.DO_NOTHING).run();
    }

    private void persist() {
        final OffsetDateTime result = OffsetDateTime.now();
        STATE.newBatch()
                .set(LAST_MARKETPLACE_CHECK_STATE_ID, result.toString())
                .set(LAST_MARKETPLACE_STATE_ID, prepare(recentDescending))
                .call();
        LOGGER.debug("New marketplace last checked time is {}.", result);
    }
}
