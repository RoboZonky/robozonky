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

package com.github.triceo.robozonky.notifications.email;

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

final class EmailCounter {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailCounter.class);
    private static final State.ClassSpecificState STATE = State.INSTANCE.forClass(EmailCounter.class);
    private static final String SEPARATOR = ";";

    private static void store(final String id, final Set<OffsetDateTime> timestamps) {
        final String result = timestamps.stream()
                .map(OffsetDateTime::toString)
                .collect(Collectors.joining(EmailCounter.SEPARATOR));
        EmailCounter.STATE.setValue(id, result);
    }

    private static Collection<OffsetDateTime> load(final String id) {
        return EmailCounter.STATE.getValue(id)
                .map(value -> {
                    final String[] split = value.split(EmailCounter.SEPARATOR);
                    return Stream.of(split)
                            .filter(s -> !s.trim().isEmpty())
                            .map(OffsetDateTime::parse)
                            .collect(Collectors.toSet());
                }).orElse(Collections.emptySet());
    }

    private final String id;
    private final int maxEmails;
    private final TemporalAmount period;
    private final Set<OffsetDateTime> timestamps = new LinkedHashSet<>();

    public EmailCounter(final String id, final int maxEmailsPerHour) {
        this(id, maxEmailsPerHour, Duration.ofHours(1));
    }

    EmailCounter(final String id, final int maxEmails, final TemporalAmount period) {
        this.id = id;
        this.maxEmails = maxEmails;
        this.period = period;
        this.timestamps.addAll(EmailCounter.load(id));
    }

    public synchronized void emailSent() {
        timestamps.add(OffsetDateTime.now());
        EmailCounter.store(id, timestamps);
    }

    public synchronized boolean allowEmail() {
        final OffsetDateTime now = OffsetDateTime.now();
        final boolean removed = timestamps.removeIf(timestamp -> timestamp.plus(period).isBefore(now));
        if (removed) {
            EmailCounter.LOGGER.trace("Some timestamps removed.");
            EmailCounter.store(id, timestamps);
        }
        return timestamps.size() < maxEmails;
    }

}
