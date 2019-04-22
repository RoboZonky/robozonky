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
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
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
 * <li>Task scheduling is requested via {@link #submit(Runnable, Duration, Duration)}. The task has a delay in which it
 * should be started, as well as a delay between the finish of one tasks and the start of the next consecutive
 * task.</li>
 * <li>The task itself ("original task") will be executed on a given {@link ExecutorService}, but in order to implement
 * the other properties, we need to implement a level of indirection via {@link ScheduledExecutorService}.</li>
 * <li>First, a task is scheduled in the {@link ScheduledExecutorService}, which only tells the {@link ExecutorService}
 * to schedule the original task after the delay has passed.</li>
 * <li>At the end of original task's execution, another request is sent to the {@link ScheduledExecutorService} to again
 * schedule the original task with the {@link ExecutorService}, this time with the delay equal to the delay between the
 * tasks, as initially specified.</li>
 * <li>The {@link ScheduledFuture} returned by {@link #submit(Runnable, Duration)} will always refer to the active
 * request on the {@link ScheduledExecutorService} - that is, if a new request has already been sent, the
 * {@link ScheduledFuture} will be referring to that new request. This is only logical, since at that point, the old
 * request is already finished.</li>
 * </ul>
 */
final class ThreadPoolExecutorBasedScheduler implements Scheduler {

    private static final Logger LOGGER = LogManager.getLogger(ThreadPoolExecutorBasedScheduler.class);
    private final ScheduledExecutorService schedulingExecutor;
    private final ExecutorService executor;
    private final Runnable onClose;

    public ThreadPoolExecutorBasedScheduler(final ScheduledExecutorService schedulingExecutor,
                                            final ExecutorService actualExecutor, final Runnable onClose) {
        this.schedulingExecutor = schedulingExecutor;
        this.executor = actualExecutor;
        this.onClose = onClose;
    }

    @Override
    public ScheduledFuture<?> submit(final Runnable toSchedule, final Duration delayInBetween,
                                     final Duration firstDelay, final Duration timeout) {
        LOGGER.debug("Scheduling {} every {} ns, starting in {} ns.", toSchedule, delayInBetween.toNanos(),
                     firstDelay.toNanos());
        final TaskDescriptor task = new TaskDescriptor(toSchedule, firstDelay, delayInBetween, timeout);
        task.schedule(this);
        return task.getFuture();
    }

    @Override
    public boolean isClosed() {
        return executor.isShutdown();
    }

    @Override
    public ExecutorService getExecutor() {
        return executor;
    }

    @Override
    public ScheduledExecutorService getSchedulingExecutor() {
        return schedulingExecutor;
    }

    @Override
    public void close() {
        LOGGER.trace("Shutting down {}.", this);
        executor.shutdown();
        try {
            LOGGER.debug("Waiting until {} shutdown.", this);
            executor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (final InterruptedException ex) {
            Thread.currentThread().interrupt();
            LOGGER.debug("Wait failed.", ex);
        } finally {
            LOGGER.trace("Wait over.");
            if (onClose != null) {
                onClose.run();
            }
        }
    }
}
