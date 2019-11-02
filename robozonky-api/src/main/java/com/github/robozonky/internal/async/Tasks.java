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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public enum Tasks {

    INSTANCE;

    private final Logger logger = LogManager.getLogger(Tasks.class);
    private final ScheduledExecutorService schedulingExecutor = Executors.newSingleThreadScheduledExecutor();
    private final ForkJoinPoolBasedScheduler scheduler = new ForkJoinPoolBasedScheduler();

    Tasks() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.debug("Requesting scheduling executor shutdown.");
            schedulingExecutor.shutdownNow();
        }));
    }

    public boolean isShuttingDown() {
        return schedulingExecutor.isShutdown();
    }

    ScheduledExecutorService schedulingExecutor() {
        return schedulingExecutor;
    }

    public Scheduler scheduler() {
        return scheduler;
    }
}
