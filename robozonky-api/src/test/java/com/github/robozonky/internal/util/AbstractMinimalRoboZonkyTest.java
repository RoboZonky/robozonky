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

package com.github.robozonky.internal.util;

import java.time.Clock;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractMinimalRoboZonkyTest {

    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());
    private final AtomicReference<Clock> original = new AtomicReference<>();

    protected void setClock(final Clock clock) {
        LOGGER.debug("Setting custom system clock.");
        original.set(DateUtil.getSystemClock());
        DateUtil.setSystemClock(clock);
    }

    @AfterEach
    protected void resetClock() {
        final Clock stored = original.getAndSet(null);
        if (stored != null) {
            DateUtil.setSystemClock(stored);
            LOGGER.debug("Setting original system clock.");
        }
    }

}
