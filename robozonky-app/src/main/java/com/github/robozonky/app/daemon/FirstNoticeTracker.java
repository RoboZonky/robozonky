/*
 * Copyright 2021 The RoboZonky Project
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

package com.github.robozonky.app.daemon;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.internal.Defaults;
import com.github.robozonky.internal.test.DateUtil;

import io.micrometer.core.instrument.Timer;

/**
 * Used to debug how long it takes for a loan to be invested into, or a participation to be purchased.
 * The times are in nanosecond from the moment the robot was first notified of the item, to the moment that the robot
 * triggered the API request to invest or purchase.
 */
final class FirstNoticeTracker {

    static final FirstNoticeTracker INSTANCE = new FirstNoticeTracker();
    private static final Logger LOGGER = LogManager.getLogger(FirstNoticeTracker.class);
    private final Instant firstAcceptedPublication = DateUtil.now();
    private final Timer timer;
    private final Map<Long, OffsetDateTime> deadlines = new ConcurrentHashMap<>(0);

    private FirstNoticeTracker() {
        this.timer = Timer.builder("robozonky.first.notice")
            .tag("marketplace", "primary")
            .register(Defaults.METER_REGISTRY);
    }

    public static CompletableFuture<Void> executeAsync(final BiConsumer<FirstNoticeTracker, Instant> operation) {
        var firstNotice = DateUtil.now();
        return CompletableFuture.runAsync(() -> operation.accept(INSTANCE, firstNotice));
    }

    public void register(final Instant notice, final long loanId, final OffsetDateTime datePublished) {
        register(notice, loanId, datePublished.toInstant(), datePublished.plusDays(7)); // Guess deadline, adjust later.
    }

    public void register(final Instant notice, final Loan loan) {
        register(notice, loan.getId(), loan.getDatePublished()
            .toInstant(), loan.getDeadline());
    }

    private void register(final Instant notice, final long loanId, final Instant datePublished,
            final OffsetDateTime deadline) {
        if (datePublished.isBefore(firstAcceptedPublication)) {
            // Loans from before the robot was started would be skewing the metric.
            return;
        }
        var previousDeadline = deadlines.put(loanId, deadline); // Always adjust deadline.
        if (previousDeadline == null) { // Only record time if this is the first time we're seeing the loan.
            var sincePublished = Duration.between(notice, datePublished)
                .abs();
            timer.record(sincePublished);
        } else {
            LOGGER.trace("Loan #{} already noticed.", loanId);
        }
    }

    public void cleanup() { // Prevent unrestricted growth of the map.
        var now = DateUtil.zonedNow()
            .toOffsetDateTime();
        for (var entry : deadlines.entrySet()) {
            var deadline = entry.getValue();
            if (deadline.isAfter(now)) {
                continue;
            }
            deadlines.remove(entry.getKey());
        }
    }

}
