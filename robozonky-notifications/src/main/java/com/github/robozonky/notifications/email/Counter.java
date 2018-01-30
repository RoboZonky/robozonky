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

package com.github.robozonky.notifications.email;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.temporal.TemporalAmount;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.robozonky.internal.api.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class Counter {

    private static final Logger LOGGER = LoggerFactory.getLogger(Counter.class);
    private final String id;
    private final long maxItems;
    private final TemporalAmount period;
    private final Set<OffsetDateTime> timestamps;

    public Counter(final String id, final int maxItems) {
        this(id, maxItems, Duration.ofHours(1));
    }

    public Counter(final String id, final int maxItems, final TemporalAmount period) {
        this.id = id;
        this.maxItems = maxItems;
        this.period = period;
        this.timestamps = new CopyOnWriteArraySet<>(Counter.load(id));
    }

    private static Collection<OffsetDateTime> load(final String id) {
        return State.forClass(Counter.class).getValues(id)
                .map(value -> value.stream()
                        .map(String::trim)
                        .map(OffsetDateTime::parse)
                        .collect(Collectors.toSet()))
                .orElse(Collections.emptySet());
    }

    private boolean store(final String id, final Set<OffsetDateTime> timestamps) {
        final Stream<String> result = getValidTimestamps().map(OffsetDateTime::toString);
        LOGGER.trace("Storing timestamps: {}.", timestamps);
        return State.forClass(Counter.class).newBatch().set(id, result).call();
    }

    private Stream<OffsetDateTime> getValidTimestamps() {
        final OffsetDateTime now = OffsetDateTime.now();
        return timestamps.stream()
                .filter(timestamp -> timestamp.plus(period).isAfter(now));
    }

    /**
     * @return True when the counter increase was properly persisted.
     */
    public boolean increase() {
        timestamps.add(OffsetDateTime.now());
        return store(id, timestamps);
    }

    public boolean allow() {
        return getValidTimestamps().count() < maxItems;
    }
}
