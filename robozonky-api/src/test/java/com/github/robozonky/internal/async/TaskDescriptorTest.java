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

import java.time.Duration;
import java.util.concurrent.Future;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TaskDescriptorTest {

    @AfterEach
    void shutdown() {
        Tasks.closeAll();
    }

    @Test
    void timeout() {
        final TaskDescriptor task = new TaskDescriptor(new EndlessRunnable(), Duration.ZERO, Duration.ofMillis(1),
                                                       Duration.ofMillis(10));
        task.schedule(Tasks.INSTANCE.scheduler());
        Assertions.assertTimeout(Duration.ofSeconds(1), () -> {
            while (task.getTimeoutCount() < 1) {
                Thread.sleep(1);
            }
        }, "The task was not killed in time.");
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(task.getSchedulingCount()).isGreaterThan(1); // re-scheduled on timeout
            softly.assertThat(task.getTimeoutCount()).isGreaterThan(0);
            softly.assertThat(task.getFailureCount()).isZero();
            softly.assertThat(task.getSuccessCount()).isZero();
        });
    }

    @Test
    void exceptional() {
        final TaskDescriptor task = new TaskDescriptor(() -> {
            throw new IllegalStateException();
        }, Duration.ZERO, Duration.ofMillis(1), Duration.ZERO);
        task.schedule(Tasks.INSTANCE.scheduler());
        Assertions.assertTimeout(Duration.ofSeconds(1), () -> {
            while (task.getFailureCount() < 1) {
                Thread.sleep(1);
            }
        }, "The task did not fail in time.");
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(task.getSchedulingCount()).isEqualTo(1); // failures are not rescheduled
            softly.assertThat(task.getFailureCount()).isEqualTo(1);
            softly.assertThat(task.getTimeoutCount()).isZero();
            softly.assertThat(task.getSuccessCount()).isZero();
            softly.assertThat((Future<Void>) task.getFuture()).isDone();
        });
    }

    @Test
    void success() {
        final TaskDescriptor task = new TaskDescriptor(() -> { /* NOOP */ }, Duration.ZERO, Duration.ofMillis(1),
                                                       Duration.ZERO);
        task.schedule(Tasks.INSTANCE.scheduler());
        Assertions.assertTimeout(Duration.ofSeconds(1), () -> {
            while (task.getSuccessCount() < 1) {
                Thread.sleep(1);
            }
        }, "The task did not fail in time.");
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(task.getSchedulingCount()).isGreaterThan(1); // re-schedule on success
            softly.assertThat(task.getSuccessCount()).isGreaterThan(0);
            softly.assertThat(task.getFailureCount()).isZero();
            softly.assertThat(task.getTimeoutCount()).isZero();
        });
    }

    private static final class EndlessRunnable implements Runnable {

        @Override
        public void run() {
            try {
                Thread.sleep(Integer.MAX_VALUE);
            } catch (final InterruptedException ex) {
                throw new IllegalStateException(ex);
            }
        }
    }
}
