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

package com.github.robozonky.internal.util;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.concurrent.atomic.AtomicReference;

import com.github.robozonky.internal.api.Defaults;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * All RoboZonky code should use this class to retrieve its now. This will ensure that tests will be able to inject
 * their own {@link Clock} implementation.
 */
public final class DateUtil {

    private static final Logger LOGGER = LogManager.getLogger(DateUtil.class);
    private static final Clock DEFAULT = Clock.system(Defaults.ZONE_ID);
    private static final AtomicReference<Clock> CLOCK = new AtomicReference<>(DEFAULT);

    private DateUtil() {
        // no instances
    }

    private static Clock getSystemClock() {
        return CLOCK.get();
    }

    static void setSystemClock(final Clock clock) {
        CLOCK.set(clock);
        LOGGER.debug("Setting system clock: {}.", clock);
    }

    static void resetSystemClock() {
        CLOCK.set(DEFAULT);
        LOGGER.debug("Reset to original system clock.");
    }

    public static ZonedDateTime zonedNow() {
        return ZonedDateTime.now(getSystemClock());
    }

    public static OffsetDateTime offsetNow() {
        return OffsetDateTime.now(getSystemClock());
    }

    public static LocalDateTime localNow() {
        return LocalDateTime.now(getSystemClock());
    }

    public static Instant now() {
        return Instant.now(getSystemClock());
    }
}
