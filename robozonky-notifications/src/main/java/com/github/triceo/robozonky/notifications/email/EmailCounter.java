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
import java.util.LinkedHashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class EmailCounter {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailCounter.class);

    private final int maxEmails;
    private final TemporalAmount period;
    private final Set<OffsetDateTime> timestamps = new LinkedHashSet<>();

    public EmailCounter(final int maxEmailsPerHour) {
        this(maxEmailsPerHour, Duration.ofHours(1));
    }

    EmailCounter(final int maxEmails, final TemporalAmount period) {
        this.maxEmails = maxEmails;
        this.period = period;
    }

    public synchronized void emailSent() {
        timestamps.add(OffsetDateTime.now());
    }

    public synchronized boolean allowEmail() {
        final OffsetDateTime now = OffsetDateTime.now();
        final boolean removed = timestamps.removeIf(timestamp -> timestamp.plus(period).isBefore(now));
        if (removed) {
            EmailCounter.LOGGER.trace("Some timestamps removed.");
        }
        return timestamps.size() < maxEmails;
    }

}
