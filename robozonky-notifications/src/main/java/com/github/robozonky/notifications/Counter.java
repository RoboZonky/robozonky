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

package com.github.robozonky.notifications;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.temporal.TemporalAmount;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.common.async.Reloadable;
import com.github.robozonky.common.state.TenantState;
import com.github.robozonky.internal.util.DateUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

final class Counter {

    private static final Logger LOGGER = LogManager.getLogger(Counter.class);
    private final String id;
    private final long maxItems;
    private final TemporalAmount period;
    private final SessionInfo sessionInfo;
    private final Reloadable<Set<OffsetDateTime>> timestamps;

    public Counter(final SessionInfo sessionInfo, final String id, final int maxItems) {
        this(sessionInfo, id, maxItems, Duration.ofHours(1));
    }

    public Counter(final SessionInfo sessionInfo, final String id, final int maxItems, final Duration period) {
        this.sessionInfo = sessionInfo;
        this.id = id;
        this.maxItems = maxItems;
        this.period = period;
        this.timestamps = Reloadable.with(() -> {
            final Set<OffsetDateTime> result = load(sessionInfo, id);
            LOGGER.debug("Loaded timestamps: {}.", result);
            return result;
        })
                .reloadAfter(period)
                .build();
    }

    private static Set<OffsetDateTime> load(final SessionInfo sessionInfo, final String id) {
        return TenantState.of(sessionInfo).in(Counter.class).getValues(id)
                .map(value -> value
                        .map(String::trim)
                        .map(OffsetDateTime::parse)
                        .collect(Collectors.toSet()))
                .orElse(new HashSet<>(0));
    }

    private void store(final SessionInfo sessionInfo, final String id, final Set<OffsetDateTime> timestamps) {
        LOGGER.trace("Storing timestamps: {}.", timestamps);
        TenantState.of(sessionInfo)
                .in(Counter.class)
                .reset(b -> b.put(id, filterValidTimestamps(timestamps).map(OffsetDateTime::toString)));
    }

    private Set<OffsetDateTime> getTimestamps() {
        return timestamps.get().getOrElse(Collections.emptySet());
    }

    private Stream<OffsetDateTime> filterValidTimestamps(final Set<OffsetDateTime> timestamps) {
        final OffsetDateTime now = DateUtil.offsetNow();
        return timestamps.stream().filter(timestamp -> timestamp.plus(period).isAfter(now));
    }

    public void increase() {
        final Set<OffsetDateTime> stamps = getTimestamps();
        stamps.add(DateUtil.offsetNow());
        store(sessionInfo, id, stamps);
    }

    public boolean allow() {
        return filterValidTimestamps(getTimestamps()).count() < maxItems;
    }
}
