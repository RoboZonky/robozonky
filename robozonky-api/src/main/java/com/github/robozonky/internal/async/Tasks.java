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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public enum Tasks implements AutoCloseable {

    INSTANCE(Executors::newCachedThreadPool);

    private static final Logger LOGGER = LogManager.getLogger(Tasks.class);
    private static final Reloadable<ScheduledExecutorService> SCHEDULING_EXECUTOR =
            Reloadable.with(Tasks::createSchedulingExecutor).build();
    private final Reloadable<? extends Scheduler> scheduler;

    Tasks(final Supplier<ExecutorService> service) {
        this.scheduler = Reloadable.with(() -> createScheduler(service.get())).build();
    }

    private static ScheduledExecutorService createSchedulingExecutor() {
        return Executors.newSingleThreadScheduledExecutor();
    }

    public static void closeAll() {
        if (SCHEDULING_EXECUTOR.hasValue()) {
            schedulingExecutor().shutdown();
            SCHEDULING_EXECUTOR.clear();
        }
        Stream.of(values()).forEach(Tasks::close);
    }

    static ScheduledExecutorService schedulingExecutor() {
        return SCHEDULING_EXECUTOR.get().getOrElseThrow(() -> new IllegalStateException("Impossible."));
    }

    private Scheduler createScheduler(final ExecutorService actualExecutor) {
        LOGGER.debug("Instantiating new background scheduler {}.", this);
        return new ThreadPoolExecutorBasedScheduler(schedulingExecutor(), actualExecutor, this::clear);
    }

    private void clear() {
        scheduler.clear();
        LOGGER.debug("Cleared background scheduler: {}.", this);
    }

    public Scheduler scheduler() {
        return scheduler.get().getOrElseThrow(() -> new IllegalStateException("Impossible."));
    }

    @Override
    public void close() {
        if (!scheduler.hasValue()) {
            LOGGER.debug("No scheduler to close: {}.", this);
            return;
        }
        try {
            scheduler().close();
        } catch (final Exception ex) { // nothing we can actually do here
            LOGGER.debug("Failed closing scheduler {}.", this, ex);
        }
    }
}
