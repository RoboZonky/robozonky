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

package com.github.robozonky.util;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SchedulerTest {

    private static final Runnable RUNNABLE = mock(Runnable.class);

    @Test
    void backgroundRestarts() {
        final Scheduler s = Scheduler.inBackground();
        assertThat(s.getExecutor()).isNotNull();
        s.close();
        assertThat(s.getExecutor().isShutdown()).isTrue();
        try (final Scheduler s2 = Scheduler.inBackground()) {
            assertThat(s).isNotNull().isNotSameAs(s2);
        }
    }

    @Test
    void submit() {
        try (final Scheduler s = Schedulers.INSTANCE.create()) {
            assertThat(s.isSubmitted(RUNNABLE)).isFalse();
            final ScheduledFuture<?> f = s.submit(RUNNABLE);
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(Scheduler.inBackground()).isNotNull();
                softly.assertThat((Future<?>) f).isNotNull();
                softly.assertThat(s.isSubmitted(RUNNABLE)).isTrue();
            });
        }
    }

    @Test
    void run() throws InterruptedException, ExecutionException, TimeoutException {
        try (final Scheduler s = Schedulers.INSTANCE.create()) {
            final Future<?> f = s.run(RUNNABLE);
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat((Future<?>) f).isNotNull();
                softly.assertThat(s.isSubmitted(RUNNABLE)).isFalse();
            });
            f.get(1, TimeUnit.MINUTES); // make sure it was executed
        }
    }

    @Test
    void runWithDelay() throws InterruptedException, ExecutionException, TimeoutException {
        try (final Scheduler s = Schedulers.INSTANCE.create()) {
            final Future<?> f = s.run(RUNNABLE, Duration.ofSeconds(1));
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat((Future<?>) f).isNotNull();
                softly.assertThat(s.isSubmitted(RUNNABLE)).isFalse();
            });
            f.get(1, TimeUnit.MINUTES); // make sure it was executed
        }
    }

    @Test
    void createsNewOnClose() {
        final Scheduler s = Scheduler.inBackground();
        assertThat(s).isNotNull();
        assertThat(s.isClosed()).isFalse();
        s.close();
        final Scheduler s2 = Scheduler.inBackground();
        assertThat(s2).isNotNull();
        assertThat(s2).isNotSameAs(s);
        assertThat(s2.isClosed()).isFalse();
        final Scheduler s3 = Scheduler.inBackground();
        assertThat(s3).isSameAs(s2);
    }
}
