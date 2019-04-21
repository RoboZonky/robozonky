package com.github.robozonky.common.async;

import java.time.Duration;

final class TaskDescriptor {

    private final DelegatingScheduledFuture<?> future;
    private final Runnable toSchedule;
    private final Duration initialDelay;
    private final Duration delayInBetween;
    private final Duration timeout;

    public TaskDescriptor(final DelegatingScheduledFuture<?> future, final Runnable toSchedule,
                          final Duration initialDelay, final Duration delayInBetween, final Duration timeout) {
        this.future = future;
        this.toSchedule = toSchedule;
        this.initialDelay = initialDelay;
        this.delayInBetween = delayInBetween;
        this.timeout = timeout;
    }

    public DelegatingScheduledFuture<?> getFuture() {
        return future;
    }

    public Runnable getToSchedule() {
        return toSchedule;
    }

    public Duration getInitialDelay() {
        return initialDelay;
    }

    public Duration getDelayInBetween() {
        return delayInBetween;
    }

    public Duration getTimeout() {
        return timeout;
    }
}
