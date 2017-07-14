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

package com.github.triceo.robozonky.notifications;

import java.time.Duration;
import java.util.UUID;

import com.github.triceo.robozonky.common.AbstractStateLeveragingTest;
import org.assertj.core.api.Assertions;
import org.junit.Test;

public class CounterTest extends AbstractStateLeveragingTest {

    @Test
    public void testTiming() throws InterruptedException {
        final int seconds = 1;
        final Counter c = new Counter(UUID.randomUUID().toString(), 1, Duration.ofSeconds(seconds));
        Assertions.assertThat(c.allow()).isTrue();
        Assertions.assertThat(c.increase()).isTrue();
        Assertions.assertThat(c.allow()).isFalse();
        int millis = 0;
        boolean isAllowed = false;
        while (millis < seconds * 5 * 1000) { // spend the absolute minimum time waiting
            Thread.sleep(1);
            millis += 1;
            isAllowed = c.allow();
            if (isAllowed) {
                break;
            }
        }
        if (!isAllowed) {
            Assertions.fail("Did not reset counter in time.");
        }
    }

}
