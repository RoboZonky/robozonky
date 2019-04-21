package com.github.robozonky.common.async;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

final class SchedulerUtil {

    private static final Logger LOGGER = LogManager.getLogger();

    private SchedulerUtil() {
        // no instances
    }

    public static void schedule(final TaskDescriptor task, final Scheduler scheduler) {
        schedule(task, scheduler, true);
    }

    static void schedule(final TaskDescriptor task, final Scheduler scheduler, final boolean firstCall) {
        final Runnable toSchedule = task.getToSchedule();
        if (scheduler.getExecutor().isShutdown()) {
            LOGGER.debug("Not scheduling {} as {} shutdown.", toSchedule, scheduler);
            return;
        }
        final Runnable toSubmit = () -> submit(task, scheduler);
        final Duration delay = firstCall ? task.getInitialDelay() : task.getDelayInBetween();
        final long totalNanos = delay.toNanos();
        LOGGER.debug("Scheduling {} to happen after {} ns.", toSchedule, totalNanos);
        final ScheduledFuture<?> f =
                scheduler.getSchedulingExecutor().schedule(toSubmit, totalNanos, TimeUnit.NANOSECONDS);
        task.getFuture().setCurrent(f);
    }

    private static void submit(final TaskDescriptor task, final Scheduler scheduler) {
        final Runnable toSchedule = task.getToSchedule();
        final Runnable runnable = () -> {
            LOGGER.trace("Running {}.", toSchedule);
            toSchedule.run();
        };
        final long totalNanos = task.getTimeout().toNanos();
        LOGGER.debug("Submitting {} for actual execution.", toSchedule);
        CompletableFuture<Void> cf = CompletableFuture.runAsync(runnable, scheduler.getExecutor());
        if (totalNanos > 0) {
            LOGGER.debug("Will be killed in {} ns.", totalNanos);
            cf = cf.orTimeout(totalNanos, TimeUnit.NANOSECONDS);
        }
        cf.handleAsync((r, t) -> rescheduleOrFail(scheduler, task, r, t), scheduler.getExecutor());
    }

    private static Void rescheduleOrFail(final Scheduler scheduler, final TaskDescriptor task, final Void result,
                                         final Throwable failure) {
        final Runnable toSchedule = task.getToSchedule();
        if (failure == null) { // reschedule
            LOGGER.debug("Completed {} successfully.", toSchedule);
            schedule(task, scheduler, false);
        } else {
            /*
             * mimic behavior of ScheduledExecutorService and don't reschedule on failure; timeout is not
             * considered
             * a failure and will cause the task to be rescheduled.
             */
            LOGGER.debug("Failed executing task {}.", toSchedule, failure);
            if (failure instanceof TimeoutException) {
                schedule(task, scheduler, false);
            }
            task.getFuture().setCurrent(new FailedScheduledFuture<Void>(failure));
        }
        return result;
    }
}
