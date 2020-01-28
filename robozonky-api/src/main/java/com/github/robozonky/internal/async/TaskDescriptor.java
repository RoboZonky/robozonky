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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.LongAdder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

final class TaskDescriptor {

    private static final Logger LOGGER = LogManager.getLogger(TaskDescriptor.class);
    private final DelegatingScheduledFuture<?> future;
    private final Runnable toSchedule;
    private final Duration initialDelay;
    private final Duration delayInBetween;
    private final Duration timeout;

    private final LongAdder schedulingCount = new LongAdder();
    private final LongAdder successCount = new LongAdder();
    private final LongAdder failureCount = new LongAdder();
    private final LongAdder timeoutCount = new LongAdder();

    public TaskDescriptor(final Runnable toSchedule, final Duration initialDelay, final Duration delayInBetween,
                          final Duration timeout) {
        this.future = new DelegatingScheduledFuture<>();
        this.toSchedule = toSchedule;
        this.initialDelay = initialDelay;
        this.delayInBetween = delayInBetween;
        this.timeout = timeout;
    }

    public void schedule(final ForkJoinPoolBasedScheduler scheduler) {
        schedule(scheduler, true);
    }

    public ScheduledFuture<?> getFuture() {
        return future;
    }

    long getSchedulingCount() {
        return schedulingCount.sum();
    }

    long getSuccessCount() {
        return successCount.sum();
    }

    long getFailureCount() {
        return failureCount.sum();
    }

    long getTimeoutCount() {
        return timeoutCount.sum();
    }

    private void schedule(final ForkJoinPoolBasedScheduler scheduler, final boolean firstCall) {
        if (Tasks.INSTANCE.isShuttingDown()) {
            LOGGER.debug("Not scheduling {} as {} shutdown.", toSchedule, scheduler);
            return;
        }
        final Runnable toSubmit = () -> submit(scheduler);
        final Duration delay = firstCall ? initialDelay : delayInBetween;
        final long totalNanos = delay.toNanos();
        LOGGER.trace("Scheduling {} to happen after {} ns.", toSchedule, totalNanos);
        final ScheduledFuture<?> f = Tasks.INSTANCE.schedulingExecutor().schedule(toSubmit, totalNanos, TimeUnit.NANOSECONDS);
        schedulingCount.increment();
        future.setCurrent(f);
    }

    private void submit(final ForkJoinPoolBasedScheduler scheduler) {
        final Runnable runnable = () -> {
            LOGGER.trace("Running {}.", toSchedule);
            toSchedule.run();
        };
        final long totalNanos = timeout.toNanos();
        LOGGER.debug("Submitting {} for actual execution.", toSchedule);
        CompletableFuture<Void> cf = CompletableFuture.runAsync(runnable);
        if (totalNanos > 0) {
            LOGGER.debug("Will be killed in {} ns.", totalNanos);
            cf = cf.orTimeout(totalNanos, TimeUnit.NANOSECONDS);
        }
        cf.handleAsync((r, t) -> rescheduleOrFail(scheduler, r, t));
    }

    private Void rescheduleOrFail(final ForkJoinPoolBasedScheduler scheduler, final Void result,
                                  final Throwable failure) {
        if (failure == null) { // reschedule
            LOGGER.trace("Completed {} successfully.", toSchedule);
            schedule(scheduler, false);
            successCount.increment();
        } else {
            /*
             * mimic behavior of ScheduledExecutorService and don't reschedule on failure; timeout is not
             * considered a failure and will cause the task to be rescheduled.
             */
            if (failure instanceof TimeoutException) {
                LOGGER.debug("Failed executing task {}, rescheduling.", toSchedule, failure);
                schedule(scheduler, false);
                timeoutCount.increment();
            } else {
                LOGGER.warn("No longer scheduling {}.", toSchedule, failure);
                future.setCurrent(new FailedScheduledFuture<Void>(failure));
                failureCount.increment();
            }
        }
        return result;
    }
}
