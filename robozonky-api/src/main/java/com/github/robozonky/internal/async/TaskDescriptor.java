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
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.LongAdder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

final class TaskDescriptor {

    private static final Logger LOGGER = LogManager.getLogger(TaskDescriptor.class);
    private final Runnable toSchedule;
    private final Duration initialDelay;
    private final Duration delayInBetween;
    private final Duration timeout;
    private boolean cancelled = false;

    private final LongAdder schedulingCount = new LongAdder();
    private final LongAdder successCount = new LongAdder();
    private final LongAdder failureCount = new LongAdder();
    private final LongAdder timeoutCount = new LongAdder();

    TaskDescriptor(final Runnable toSchedule, final Duration initialDelay, final Duration delayInBetween,
                   final Duration timeout) {
        this.toSchedule = () -> {
            LOGGER.trace("Running {} from within {}.", toSchedule, this);
            try {
                toSchedule.run();
            } finally {
                LOGGER.trace("Finished {}.", toSchedule);
            }
        };
        this.initialDelay = initialDelay;
        this.delayInBetween = delayInBetween;
        this.timeout = timeout;
    }

    public void cancel() {
        cancelled = true;
    }

    void schedule() {
        schedule(null);
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

    private void schedule(final Executor executor) {
        if (cancelled || ForkJoinPool.commonPool().isTerminating()) {
            LOGGER.debug("Not scheduling {} as the common pool is terminating.", this);
            return;
        }
        LOGGER.trace("Scheduling {} to happen after {}.", this, initialDelay);
        Executor futureDelayedExecutor = executor == null ?
                CompletableFuture.delayedExecutor(delayInBetween.toNanos(), TimeUnit.NANOSECONDS) :
                executor;
        final Runnable toSubmit = () -> submit(futureDelayedExecutor);
        Executor delayedExecutor = executor == null ?
                CompletableFuture.delayedExecutor(initialDelay.toNanos(), TimeUnit.NANOSECONDS) :
                executor;
        delayedExecutor.execute(toSubmit);
        schedulingCount.increment();
    }

    private void submit(final Executor executor) {
        final long totalNanos = timeout.toNanos();
        LOGGER.debug("Submitting {} for actual execution.", this);
        CompletableFuture<Void> cf = CompletableFuture.runAsync(toSchedule);
        if (totalNanos > 0) {
            LOGGER.debug("Will be killed in {} ns.", totalNanos);
            cf = cf.orTimeout(totalNanos, TimeUnit.NANOSECONDS);
        }
        cf.whenCompleteAsync((r, t) -> rescheduleOrFail(executor, t));
    }

    private void rescheduleOrFail(final Executor executor, final Throwable failure) {
        if (failure == null) { // reschedule
            LOGGER.trace("Completed {} successfully.", this);
            schedule(executor);
            successCount.increment();
        } else if (failure instanceof TimeoutException) {
            LOGGER.debug("Failed executing task {}, rescheduling.", this, failure);
            schedule(executor);
            timeoutCount.increment();
        } else {
            LOGGER.warn("No longer scheduling {}.", this, failure);
            failureCount.increment();
        }
    }
}
