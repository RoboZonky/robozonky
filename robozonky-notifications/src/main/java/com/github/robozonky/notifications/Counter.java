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

package com.github.robozonky.notifications;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.temporal.TemporalAmount;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.common.state.TenantState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class Counter {

    private static final Logger LOGGER = LoggerFactory.getLogger(Counter.class);
    private final String id;
    private final long maxItems;
    private final TemporalAmount period;
    private final Map<SessionInfo, Set<OffsetDateTime>> timestamps = new HashMap<>(0);

    public Counter(final String id, final int maxItems) {
        this(id, maxItems, Duration.ofHours(1));
    }

    public Counter(final String id, final int maxItems, final TemporalAmount period) {
        this.id = id;
        this.maxItems = maxItems;
        this.period = period;
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

    private Set<OffsetDateTime> getTimestamps(final SessionInfo sessionInfo) {
        return timestamps.computeIfAbsent(sessionInfo, sessionInfo1 -> Counter.load(sessionInfo1, id));
    }

    private Stream<OffsetDateTime> filterValidTimestamps(final Set<OffsetDateTime> timestamps) {
        final OffsetDateTime now = OffsetDateTime.now();
        return timestamps.stream().filter(timestamp -> timestamp.plus(period).isAfter(now));
    }

    public void increase(final SessionInfo sessionInfo) {
        final Set<OffsetDateTime> timestamps = getTimestamps(sessionInfo);
        getTimestamps(sessionInfo).add(OffsetDateTime.now());
        store(sessionInfo, id, timestamps);
    }

    public boolean allow(final SessionInfo sessionInfo) {
        return filterValidTimestamps(getTimestamps(sessionInfo)).count() < maxItems;
    }
}
