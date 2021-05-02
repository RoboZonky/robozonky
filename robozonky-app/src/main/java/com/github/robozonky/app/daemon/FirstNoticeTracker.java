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

    private static final Logger LOGGER = LogManager.getLogger(FirstNoticeTracker.class);
    static final FirstNoticeTracker INSTANCE = new FirstNoticeTracker();

    private final Instant firstAcceptedPublication = DateUtil.now();
    private final Timer timer;
    private final Map<Integer, OffsetDateTime> registrations = new ConcurrentHashMap<>(0);

    private FirstNoticeTracker() {
        this.timer = Timer.builder("robozonky.first.notice")
            .tag("marketplace", "primary")
            .register(Defaults.METER_REGISTRY);
    }

    public static CompletableFuture<Void> executeAsync(final BiConsumer<FirstNoticeTracker, Instant> operation) {
        var firstNotice = DateUtil.now();
        return CompletableFuture.runAsync(() -> operation.accept(INSTANCE, firstNotice));
    }

    public void register(final Instant firstNotice, final Loan loan) {
        if (loan.getDatePublished()
            .toInstant()
            .isBefore(firstAcceptedPublication)) {
            // Loans from before the robot was started would be skewing the metric.
            return;
        }
        var previousDeadline = registrations.putIfAbsent(loan.getId(), loan.getDeadline());
        if (previousDeadline == null) {
            var sincePublished = Duration.between(firstNotice, loan.getDatePublished()
                .toInstant())
                .abs();
            timer.record(sincePublished);
        } else {
            LOGGER.trace("Loan #{} already noticed.", loan.getId());
        }
    }

    public void cleanup() { // Prevent unrestricted growth of the map.
        for (var entry : registrations.entrySet()) {
            if (entry.getValue()
                .isAfter(OffsetDateTime.now())) {
                continue;
            }
            registrations.remove(entry.getKey());
        }
    }

}
