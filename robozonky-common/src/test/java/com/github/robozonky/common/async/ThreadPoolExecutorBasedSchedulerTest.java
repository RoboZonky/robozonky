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

package com.github.robozonky.common.async;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.LongAccumulator;

import com.github.robozonky.internal.util.AbstractMinimalRoboZonkyTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class ThreadPoolExecutorBasedSchedulerTest extends AbstractMinimalRoboZonkyTest {

    @Test
    void repeating() throws Exception {
        final ScheduledExecutorService s1 = Executors.newSingleThreadScheduledExecutor();
        final ExecutorService s2 = Executors.newCachedThreadPool();
        final LongAccumulator accumulator = new LongAccumulator((a, b) -> a + b, 0);
        final Runnable r = () -> accumulator.accumulate(1);
        try (final Scheduler s = new ThreadPoolExecutorBasedScheduler(s1, s2, () -> {
            s1.shutdown();
            s2.shutdown();
        })) {
            s.submit(r, Duration.ofMillis(1));
            Thread.sleep(50); // give A LOT OF TIME for the repeat to actually happen
        }
        assertThat(accumulator.longValue()).isGreaterThanOrEqualTo(2);
    }

}
