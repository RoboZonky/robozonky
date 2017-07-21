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

package com.github.triceo.robozonky.app.investing;

import java.time.OffsetDateTime;
import java.time.temporal.TemporalAmount;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * For whatever reason, if the marketplace stops being repeatedly checked, the robot is effectively dead. And we can
 * not get any reasonable logs, since we usually find out days after this has happened.
 * <p>
 * This little piece of code is designed to detect such a situation. Surrounding code will then take this and kill
 * the robot, sending a warning e-mail. When that happens, we will have up-to-date logs and will be able to identify
 * the culprit.
 */
class SuddenDeathDetection implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(SuddenDeathDetection.class);

    private final CountDownLatch daemonStopsIfReleased;
    private final AtomicReference<OffsetDateTime> lastMarketplaceCheck = new AtomicReference<>(OffsetDateTime.now());
    private final AtomicBoolean suddenDeath = new AtomicBoolean(false);
    private final TemporalAmount inactivityThreshold;

    public SuddenDeathDetection(final CountDownLatch daemonStopsIfReleased, final TemporalAmount inactivityThreshold) {
        this.daemonStopsIfReleased = daemonStopsIfReleased;
        this.inactivityThreshold = inactivityThreshold;
    }

    public void registerMarketplaceCheck() {
        lastMarketplaceCheck.set(OffsetDateTime.now());
    }

    public boolean isSuddenDeath() {
        return suddenDeath.get();
    }

    @Override
    public void run() {
        try {
            final OffsetDateTime now = OffsetDateTime.now();
            if (lastMarketplaceCheck.get().plus(inactivityThreshold).isBefore(now)) {
                SuddenDeathDetection.LOGGER.error("Sudden death is here.");
                suddenDeath.set(true); // make the rest of the code notice the sudden death
                daemonStopsIfReleased.countDown(); // kill daemon and report
            }
        } catch (final Throwable t) { // not catching here would stop the thread, disabling sudden death detection
            SuddenDeathDetection.LOGGER.warn("Sudden death workaround error.", t);
        }
    }
}
