package com.github.robozonky.common.async;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.LongAdder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

final class TaskDescriptor {

    private static final Logger LOGGER = LogManager.getLogger();
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

    public void schedule(final Scheduler scheduler) {
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

    private void schedule(final Scheduler scheduler, final boolean firstCall) {
        if (scheduler.getExecutor().isShutdown()) {
            LOGGER.debug("Not scheduling {} as {} shutdown.", toSchedule, scheduler);
            return;
        }
        final Runnable toSubmit = () -> submit(scheduler);
        final Duration delay = firstCall ? initialDelay : delayInBetween;
        final long totalNanos = delay.toNanos();
        LOGGER.debug("Scheduling {} to happen after {} ns.", toSchedule, totalNanos);
        final ScheduledFuture<?> f =
                scheduler.getSchedulingExecutor().schedule(toSubmit, totalNanos, TimeUnit.NANOSECONDS);
        schedulingCount.increment();
        future.setCurrent(f);
    }

    private void submit(final Scheduler scheduler) {
        final Runnable runnable = () -> {
            LOGGER.trace("Running {}.", toSchedule);
            toSchedule.run();
        };
        final long totalNanos = timeout.toNanos();
        LOGGER.debug("Submitting {} for actual execution.", toSchedule);
        CompletableFuture<Void> cf = CompletableFuture.runAsync(runnable, scheduler.getExecutor());
        if (totalNanos > 0) {
            LOGGER.debug("Will be killed in {} ns.", totalNanos);
            cf = cf.orTimeout(totalNanos, TimeUnit.NANOSECONDS);
        }
        cf.handleAsync((r, t) -> rescheduleOrFail(scheduler, r, t), scheduler.getExecutor());
    }

    private Void rescheduleOrFail(final Scheduler scheduler, final Void result, final Throwable failure) {
        if (failure == null) { // reschedule
            LOGGER.debug("Completed {} successfully.", toSchedule);
            schedule(scheduler, false);
            successCount.increment();
        } else {
            /*
             * mimic behavior of ScheduledExecutorService and don't reschedule on failure; timeout is not
             * considered a failure and will cause the task to be rescheduled.
             */
            LOGGER.debug("Failed executing task {}.", toSchedule, failure);
            if (failure instanceof TimeoutException) {
                schedule(scheduler, false);
                timeoutCount.increment();
            } else {
                future.setCurrent(new FailedScheduledFuture<Void>(failure));
                failureCount.increment();
            }
        }
        return result;
    }
}
