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

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class SchedulerTest {

    private static final Runnable RUNNABLE = mock(Runnable.class);

    private static Scheduler getInstance() {
        return Tasks.BACKGROUND.scheduler();
    }

    @Test
    void backgroundRestarts() throws Exception {
        final Scheduler s = getInstance();
        assertThat(s.getExecutor()).isNotNull();
        s.close();
        assertThat(s.getExecutor().isShutdown()).isTrue();
        try (final Scheduler s2 = getInstance()) {
            assertThat(s).isNotNull().isNotSameAs(s2);
        }
    }

    @Test
    void run() throws Exception {
        try (final Scheduler s = getInstance()) {
            final Future<?> f = s.getExecutor().submit(RUNNABLE);
            assertThat((Future<?>) f).isNotNull();
            f.get(1, TimeUnit.MINUTES); // make sure it was executed
        }
    }

    @Test
    void createsNewOnClose() throws Exception {
        final Scheduler s = getInstance();
        assertThat(s).isNotNull();
        assertThat(s.isClosed()).isFalse();
        s.close();
        final Scheduler s2 = getInstance();
        assertThat(s2).isNotNull();
        assertThat(s2).isNotSameAs(s);
        assertThat(s2.isClosed()).isFalse();
        final Scheduler s3 = getInstance();
        assertThat(s3).isSameAs(s2);
    }

    @AfterEach
    void closeAll() {
        Tasks.closeAll();
    }
}
