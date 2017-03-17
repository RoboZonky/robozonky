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

package com.github.triceo.robozonky.notifications;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.temporal.TemporalAmount;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.triceo.robozonky.internal.api.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Counter {

    private static final Logger LOGGER = LoggerFactory.getLogger(Counter.class);
    private static final State.ClassSpecificState STATE = State.INSTANCE.forClass(Counter.class);
    private static final String SEPARATOR = ";";

    private static void store(final String id, final Set<OffsetDateTime> timestamps) {
        final String result = timestamps.stream()
                .map(OffsetDateTime::toString)
                .collect(Collectors.joining(Counter.SEPARATOR));
        Counter.STATE.setValue(id, result);
    }

    private static Collection<OffsetDateTime> load(final String id) {
        return Counter.STATE.getValue(id)
                .map(value -> {
                    final String[] split = value.split(Counter.SEPARATOR);
                    return Stream.of(split)
                            .filter(s -> !s.trim().isEmpty())
                            .map(OffsetDateTime::parse)
                            .collect(Collectors.toSet());
                }).orElse(Collections.emptySet());
    }

    private final String id;
    private final int maxItems;
    private final TemporalAmount period;
    private final Set<OffsetDateTime> timestamps = new LinkedHashSet<>();

    public Counter(final String id, final int maxItemsPerHour) {
        this(id, maxItemsPerHour, Duration.ofHours(1));
    }

    Counter(final String id, final int maxItems, final TemporalAmount period) {
        this.id = id;
        this.maxItems = maxItems;
        this.period = period;
        this.timestamps.addAll(Counter.load(id));
    }

    public synchronized void increase() {
        timestamps.add(OffsetDateTime.now());
        Counter.store(id, timestamps);
    }

    public synchronized boolean allow() {
        final OffsetDateTime now = OffsetDateTime.now();
        final boolean removed = timestamps.removeIf(timestamp -> timestamp.plus(period).isBefore(now));
        if (removed) {
            Counter.LOGGER.trace("Some timestamps removed.");
            Counter.store(id, timestamps);
        }
        return timestamps.size() < maxItems;
    }

}
