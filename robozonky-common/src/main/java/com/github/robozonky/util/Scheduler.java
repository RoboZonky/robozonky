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

package com.github.robozonky.util;

import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.github.robozonky.internal.api.Settings;
import com.github.robozonky.internal.util.LazyInitialized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("rawtypes")
public class Scheduler implements AutoCloseable {

    private static final ThreadFactory THREAD_FACTORY = new RoboZonkyThreadFactory(newThreadGroup("rzBackground"));
    private static final Logger LOGGER = LoggerFactory.getLogger(Scheduler.class);
    private static final LazyInitialized<Scheduler> BACKGROUND_SCHEDULER = LazyInitialized.create(() -> {
        LOGGER.debug("Instantiating new background scheduler.");
        /*
         * Pool size > 1 speeds up RoboZonky startup. Strategy loading will block until all other preceding tasks will
         * have finished on the executor and if some of them are long-running, this will hurt robot's startup time.
         */
        return Schedulers.INSTANCE.create(2, THREAD_FACTORY);
    });
    private static final Duration REFRESH = Settings.INSTANCE.getRemoteResourceRefreshInterval();
    private final Collection<Runnable> submitted = new CopyOnWriteArraySet<>();
    private final AtomicInteger pauseRequests = new AtomicInteger(0);
    private final PausableScheduledExecutorService executor;

    Scheduler(final int poolSize, final ThreadFactory threadFactory) {
        this.executor = SchedulerServiceLoader.load().newScheduledExecutorService(poolSize, threadFactory);
    }

    private static ThreadGroup newThreadGroup(final String name) {
        final ThreadGroup threadGroup = new ThreadGroup(name);
        threadGroup.setMaxPriority(Thread.NORM_PRIORITY - 1); // these threads are supposed to be less important
        threadGroup.setDaemon(true); // all of these threads are daemons (won't block shutdown)
        return threadGroup;
    }

    public static Scheduler inBackground() {
        final Scheduler s = BACKGROUND_SCHEDULER.get();
        if (s.isClosed()) {
            BACKGROUND_SCHEDULER.reset();
            return BACKGROUND_SCHEDULER.get();
        }
        return s;
    }

    public ScheduledFuture submit(final Runnable toSchedule) {
        return this.submit(toSchedule, Scheduler.REFRESH);
    }

    public ScheduledFuture submit(final Runnable toSchedule, final Duration delayInBetween) {
        return submit(toSchedule, delayInBetween, Duration.ZERO);
    }

    public ScheduledFuture submit(final Runnable toSchedule, final Duration delayInBetween,
                                  final Duration firstDelay) {
        Scheduler.LOGGER.debug("Scheduling {} every {} ms, starting in {} ms.", toSchedule, delayInBetween.toMillis(),
                               firstDelay.toMillis());
        /*
         * it is imperative that tasks be scheduled with fixed delay. if scheduled at fixed rate instead, pausing the
         * executor would result in tasks queuing up. and since we use this class to schedule tasks as frequently as
         * every second, such behavior is not acceptable.
         */
        final ScheduledFuture f = executor.scheduleWithFixedDelay(toSchedule, firstDelay.toNanos(),
                                                                  delayInBetween.toNanos(), TimeUnit.NANOSECONDS);
        this.submitted.add(toSchedule);
        return f;
    }

    public Future run(final Runnable toRun) {
        Scheduler.LOGGER.debug("Submitting {} immediately.", toRun);
        return executor.submit(toRun);
    }

    public Future run(final Runnable toRun, final Duration delay) {
        final long millis = delay.toMillis();
        Scheduler.LOGGER.debug("Submitting {} to run in {} ms.", toRun, millis);
        return executor.schedule(toRun, millis, TimeUnit.MILLISECONDS);
    }

    public boolean isSubmitted(final Runnable refreshable) {
        return submitted.contains(refreshable);
    }

    /**
     * Pause the scheduler, queuing all scheduled executions until {@link #resume()} is called. Calling this method
     * several times in a row will require equal amount of calls to {@link #resume()} to resume the scheduler later.
     */
    void pause() {
        pauseRequests.updateAndGet(old -> {
            if (old == 0) {
                executor.pause();
            }
            return old + 1;
        });
        LOGGER.trace("Incrementing pause counter for {}.", this);
    }

    public boolean isPaused() {
        return pauseRequests.get() > 0;
    }

    public boolean isClosed() {
        return executor.isShutdown();
    }

    public ScheduledExecutorService getExecutor() {
        return executor;
    }

    /**
     * Decrements the counter of times that {@link #pause()} was called. If counter reaches zero, scheduler is resumed
     * and all queued tasks are executed.
     */
    void resume() {
        LOGGER.trace("Decrementing pause counter for {}.", this);
        pauseRequests.updateAndGet(old -> {
            if (old == 1) {
                executor.resume();
            }
            return Math.max(0, old - 1);
        });
    }

    @Override
    public void close() {
        Scheduler.LOGGER.trace("Shutting down {}.", this);
        executor.shutdownNow();
        Schedulers.INSTANCE.destroy(this);
    }
}
