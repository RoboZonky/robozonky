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

package com.github.robozonky.internal.async;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.ThrowingSupplier;

import java.time.Duration;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.LongAccumulator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

class ForkJoinPoolExecutorBasedSchedulerTest {

    @Test
    void repeating() {
        final LongAccumulator accumulator = new LongAccumulator(Long::sum, 0);
        final Runnable r = () -> accumulator.accumulate(1);
        final Scheduler s = new ForkJoinPoolBasedScheduler();
        final ScheduledFuture<?> f = s.submit(r, Duration.ofMillis(1));
        assertThat((Future)f).isNotNull();
        assertTimeoutPreemptively(Duration.ofSeconds(5), (ThrowingSupplier<?>)f::get);
        assertThat(accumulator.longValue()).isGreaterThanOrEqualTo(1);
    }

}
