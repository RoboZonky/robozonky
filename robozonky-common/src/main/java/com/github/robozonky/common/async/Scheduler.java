/*
 * Copyright 2018 The RoboZonky Project
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
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import com.github.robozonky.internal.api.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("rawtypes")
public class Scheduler implements AutoCloseable {

    public static final ThreadFactory THREAD_FACTORY = new RoboZonkyThreadFactory(new ThreadGroup("rzBackground"));
    private static final Logger LOGGER = LoggerFactory.getLogger(Scheduler.class);
    private static final Reloadable<Scheduler> BACKGROUND_SCHEDULER = Reloadable.with(() -> {
        LOGGER.debug("Instantiating new background scheduler.");
        // give as many threads as is necessary; the scheduler will start with 1 and scale back to 1 if no more needed
        return new Scheduler(Integer.MAX_VALUE, THREAD_FACTORY, Scheduler::cleanBackgroundScheduler);
    }).build();
    private static final Duration REFRESH = Settings.INSTANCE.getRemoteResourceRefreshInterval();
    private final ScheduledExecutorService executor;
    private final Runnable onClose;

    /**
     * @param poolSize The maximum amount of threads that the scheduler will have available. Minimum is 1 and the
     * scheduler may scale down to that number when it has no need for more threads.
     * @param threadFactory
     * @param onClose This exists mostly so that the background scheduler can reload itself after its {@link #close()}
     * method had been called.
     */
    private Scheduler(final int poolSize, final ThreadFactory threadFactory, final Runnable onClose) {
        this.executor = SchedulerServiceLoader.load().newScheduledExecutorService(poolSize, threadFactory);
        this.onClose = onClose;
    }

    public Scheduler(final int poolSize, final ThreadFactory threadFactory) {
        this(poolSize, threadFactory, null);
    }

    Scheduler() {
        this(1, Executors.defaultThreadFactory());
    }

    private static void cleanBackgroundScheduler() {
        BACKGROUND_SCHEDULER.clear();
        LOGGER.trace("Cleared background scheduler.");
    }

    public static Scheduler inBackground() {
        return BACKGROUND_SCHEDULER.get().getOrElseThrow(() -> new IllegalStateException("Impossible."));
    }

    public ScheduledFuture submit(final Runnable toSchedule) {
        return this.submit(toSchedule, REFRESH);
    }

    public ScheduledFuture submit(final Runnable toSchedule, final Duration delayInBetween) {
        return submit(toSchedule, delayInBetween, Duration.ZERO);
    }

    public ScheduledFuture submit(final Runnable toSchedule, final Duration delayInBetween,
                                  final Duration firstDelay) {
        LOGGER.debug("Scheduling {} every {} ms, starting in {} ms.", toSchedule, delayInBetween.toMillis(),
                     firstDelay.toMillis());
        /*
         * it is imperative that tasks be scheduled with fixed delay. if scheduled at fixed rate instead, pausing the
         * executor would result in tasks queuing up. and since we use this class to schedule tasks as frequently as
         * every second, such behavior is not acceptable.
         */
        return executor.scheduleWithFixedDelay(toSchedule, firstDelay.toNanos(), delayInBetween.toNanos(),
                                               TimeUnit.NANOSECONDS);
    }

    public Future run(final Runnable toRun) {
        LOGGER.debug("Submitting {} immediately.", toRun);
        return executor.submit(toRun);
    }

    public Future run(final Runnable toRun, final Duration delay) {
        final long millis = delay.toMillis();
        LOGGER.debug("Submitting {} to run in {} ms.", toRun, millis);
        return executor.schedule(toRun, millis, TimeUnit.MILLISECONDS);
    }

    public boolean isClosed() {
        return executor.isShutdown();
    }

    public ScheduledExecutorService getExecutor() {
        return executor;
    }

    @Override
    public void close() {
        LOGGER.trace("Shutting down {}.", this);
        executor.shutdownNow();
        if (onClose != null) {
            onClose.run();
        }
    }
}
