/*
 * Copyright 2020 The RoboZonky Project
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

package com.github.robozonky.test;

import java.time.Clock;
import java.time.Duration;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.mockito.Mockito;

import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.internal.Defaults;
import com.github.robozonky.internal.SessionInfoImpl;
import com.github.robozonky.internal.test.DateUtil;
import com.github.robozonky.internal.test.RandomUtil;

public abstract class AbstractMinimalRoboZonkyTest {

    protected static final String USERNAME = "someone@robozonky.cz";
    protected final Logger logger = LogManager.getLogger(getClass());

    protected static SessionInfo mockSessionInfo() {
        return mockSessionInfo(false);
    }

    protected static SessionInfo mockSessionInfo(boolean isDryRun) {
        return Mockito.spy(new SessionInfoImpl(USERNAME, "Testing", isDryRun));
    }

    protected void setClock(final Clock clock) {
        DateUtil.setSystemClock(clock);
    }

    protected void skipAheadBy(final Duration duration) {
        var next = DateUtil.zonedNow()
            .plus(duration)
            .toInstant();
        setClock(Clock.fixed(next, Defaults.ZONKYCZ_ZONE_ID));
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
