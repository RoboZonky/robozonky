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

import java.time.Duration;
import java.util.concurrent.*;

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
final class ForkJoinPoolBasedScheduler implements Scheduler {

    private static final Logger LOGGER = LogManager.getLogger(ForkJoinPoolBasedScheduler.class);

    @Override
    public ScheduledFuture<?> submit(final Runnable toSchedule, final Duration delayInBetween,
                                     final Duration firstDelay, final Duration timeout) {
        LOGGER.debug("Scheduling {} every {} ns, starting in {} ns.", toSchedule, delayInBetween.toNanos(),
                     firstDelay.toNanos());
        final TaskDescriptor task = new TaskDescriptor(toSchedule, firstDelay, delayInBetween, timeout);
        task.schedule(this);
        return task.getFuture();
    }

}
