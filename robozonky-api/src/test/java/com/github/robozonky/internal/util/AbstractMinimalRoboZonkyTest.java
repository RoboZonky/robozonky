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
import java.time.Duration;
import java.time.Instant;
import java.util.Random;

import com.github.robozonky.internal.api.Defaults;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterEach;

public abstract class AbstractMinimalRoboZonkyTest {

    protected final Logger logger = LogManager.getLogger(getClass());

    protected void setClock(final Clock clock) {
        DateUtil.setSystemClock(clock);
    }

    protected void skipAheadBy(final Duration duration) {
        final Instant next = DateUtil.now().plus(duration);
        setClock(Clock.fixed(next, Defaults.ZONE_ID));
    }

    protected void setRandom(final Random random) {
        RandomUtil.setRandom(random);
    }

    @AfterEach
    protected void resetClock() {
        DateUtil.resetSystemClock();
    }

    @AfterEach
    protected void resetRandom() {
        RandomUtil.resetRandom();
    }
}
