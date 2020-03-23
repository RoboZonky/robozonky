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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This attempts to provide functionality similar to {@link ScheduledExecutorService}, but with a fully configurable
 * underlying {@link ThreadPoolExecutor}. ({@link ScheduledThreadPoolExecutor} does not really allow for scaling the
 * pool size as cached thread pool does.)
 * <p>
 * This is how it works:
 * <ul>
 * <li>Task scheduling is requested via {@link #submit(Runnable, Duration, Duration, Duration)}. The task has a delay
 * in which it should be started, as well as a delay between the finish of one tasks and the start of the next
 * consecutive task.</li>
 * <li>The task itself ("original task") will be executed on a given {@link ExecutorService}, but in order to implement
 * the other properties, we need to implement a level of indirection.</li>
 * <li>First, a task is scheduled with a delayed executor, which only tells the {@link ExecutorService} to schedule the
 * original task after the delay has passed.</li>
 * <li>At the end of original task's execution, another request is sent to the delayed executor to again schedule the
 * original task with the {@link ExecutorService}, this time with the delay equal to the delay between the tasks, as
 * initially specified.</li>
 * </ul>
 */
final class CachedThreadPoolBasedScheduler implements Scheduler {

    private static final Logger LOGGER = LogManager.getLogger(CachedThreadPoolBasedScheduler.class);
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    @Override
    public TaskDescriptor submit(final Runnable toSchedule, final Duration delayInBetween, final Duration firstDelay,
            final Duration timeout) {
        final TaskDescriptor task = new TaskDescriptor(executorService, toSchedule, firstDelay, delayInBetween,
                timeout);
        LOGGER.debug("Scheduling {} every {} ns, starting in {} ns.", task, delayInBetween.toNanos(),
                firstDelay.toNanos());
        task.schedule();
        return task;
    }

    @Override
    public boolean isClosed() {
        return executorService.isShutdown();
    }

    @Override
    public void close() throws Exception {
        executorService.shutdownNow();
        LOGGER.debug("Shutting down {}.", executorService);
        executorService.awaitTermination(1, TimeUnit.SECONDS);
        LOGGER.debug("Shut down {}.", executorService);
    }
}
