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

import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class TasksTest {

    @Test
    void closing() {
        final Set<ExecutorService> e = Stream.of(Tasks.values())
                .map(t -> t.scheduler().getExecutor())
                .collect(Collectors.toSet());
        final ScheduledExecutorService se = Tasks.schedulingExecutor();
        Tasks.closeAll();
        assertThat(e)
                .extracting(ExecutorService::isShutdown)
                .doesNotContain(false); // all are closed
        assertThat(se.isShutdown()).isTrue();
        final Set<ExecutorService> e2 = Stream.of(Tasks.values())
                .map(t -> t.scheduler().getExecutor())
                .collect(Collectors.toSet());
        final ScheduledExecutorService se2 = Tasks.schedulingExecutor();
        assertThat(e2)
                .extracting(ExecutorService::isShutdown)
                .doesNotContain(true); // all are recreated
        assertThat(se2.isShutdown()).isFalse();
    }

    @AfterEach
    void closeAll() {
        Tasks.closeAll();
    }
}
