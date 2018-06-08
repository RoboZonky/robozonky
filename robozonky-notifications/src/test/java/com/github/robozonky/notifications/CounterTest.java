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

package com.github.robozonky.notifications;

import java.time.Duration;
import java.util.UUID;

import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.test.AbstractRoboZonkyTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class CounterTest extends AbstractRoboZonkyTest {

    private static final SessionInfo SESSION = new SessionInfo("someone@robozonky.cz");

    @Test
    void testTiming() throws InterruptedException {
        final int seconds = 1;
        final Counter c = new Counter(UUID.randomUUID().toString(), 1, Duration.ofSeconds(seconds));
        assertThat(c.allow(SESSION)).isTrue();
        c.increase(SESSION);
        assertThat(c.allow(SESSION)).isFalse();
        int millis = 0;
        boolean isAllowed = false;
        while (millis < seconds * 5 * 1000) { // spend the absolute minimum time waiting
            Thread.sleep(1);
            millis += 1;
            isAllowed = c.allow(SESSION);
            if (isAllowed) {
                break;
            }
        }
        if (!isAllowed) {
            fail("Did not reset counter in time.");
        }
    }
}
