/*
 * Copyright 2020 The RoboZonky Project
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

package com.github.robozonky.notifications;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAmount;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.internal.Defaults;
import com.github.robozonky.internal.async.Reloadable;
import com.github.robozonky.internal.state.TenantState;
import com.github.robozonky.internal.test.DateUtil;

final class Counter {

    private static final Logger LOGGER = LogManager.getLogger(Counter.class);
    private final String id;
    private final long maxItems;
    private final TemporalAmount period;
    private final SessionInfo sessionInfo;
    private final Reloadable<Set<ZonedDateTime>> timestamps;

    public Counter(final SessionInfo sessionInfo, final String id, final int maxItems) {
        this(sessionInfo, id, maxItems, Duration.ofHours(1));
    }

    public Counter(final SessionInfo sessionInfo, final String id, final int maxItems, final Duration period) {
        this.sessionInfo = sessionInfo;
        this.id = id;
        this.maxItems = maxItems;
        this.period = period;
        this.timestamps = Reloadable.with(() -> {
            var result = load(sessionInfo, id);
            LOGGER.debug(() -> {
                var toString = result.stream()
                    .map(DateUtil::toString)
                    .collect(Collectors.joining("; ", "[", "]"));
                return "Loaded timestamps: " + toString + ".";
            });
            return result;
        })
            .reloadAfter(period)
            .build();
    }

    private static Set<ZonedDateTime> load(final SessionInfo sessionInfo, final String id) {
        return TenantState.of(sessionInfo)
            .in(Counter.class)
            .getValues(id)
            .map(value -> value
                .map(String::trim)
                .map(OffsetDateTime::parse)
                .map(t -> t.atZoneSameInstant(Defaults.ZONKYCZ_ZONE_ID))
                .collect(Collectors.toSet()))
            .orElse(new HashSet<>(0));
    }

    private void store(final SessionInfo sessionInfo, final String id, final Set<ZonedDateTime> timestamps) {
        LOGGER.trace("Storing timestamps: {}.", timestamps);
        TenantState.of(sessionInfo)
            .in(Counter.class)
            .reset(b -> b.put(id, filterValidTimestamps(timestamps)
                .map(ZonedDateTime::toOffsetDateTime)
                .map(OffsetDateTime::toString)));
    }

    private Set<ZonedDateTime> getTimestamps() {
        return timestamps.get()
            .getOrElse(Collections.emptySet());
    }

    private Stream<ZonedDateTime> filterValidTimestamps(final Set<ZonedDateTime> timestamps) {
        var now = DateUtil.zonedNow();
        return timestamps.stream()
            .filter(timestamp -> timestamp.plus(period)
                .isAfter(now));
    }

    public void increase() {
        var stamps = getTimestamps();
        stamps.add(DateUtil.zonedNow());
        store(sessionInfo, id, stamps);
    }

    public boolean allow() {
        return filterValidTimestamps(getTimestamps()).count() < maxItems;
    }
}
