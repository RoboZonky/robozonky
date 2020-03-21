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

package com.github.robozonky.internal.async;

import java.time.Duration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SchedulerTest {

    @Test
    void simple() throws Exception {
        try (Scheduler scheduler = Scheduler.create()) {
            TaskDescriptor task = scheduler.submit(() -> {
                // NOOP
            }, Duration.ofMillis(1), Duration.ofMillis(2), Duration.ofMillis(10));
            Assertions.assertTimeoutPreemptively(Duration.ofSeconds(1), () -> {
                while (task.getSuccessCount() < 1) {
                    Thread.sleep(1);
                }
            }, "Timed out while waiting for operation to complete.");
        }
    }

}
