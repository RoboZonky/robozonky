/*
 * Copyright 2017 The RoboZonky Project
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
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAmount;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import com.github.robozonky.internal.api.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Scheduler {

    /*
     * Pool size > 1 speeds up RoboZonky startup. Strategy loading will block until all other preceding tasks
     * will have finished on the executor and if some of them are long-running, this will hurt robot's startup
     * time.
     */
    private static final Scheduler BACKGROUND_SCHEDULER = new Scheduler(2);
    private static final Logger LOGGER = LoggerFactory.getLogger(Scheduler.class);
    private static final TemporalAmount REFRESH = Settings.INSTANCE.getRemoteResourceRefreshInterval();
    private final Supplier<ScheduledExecutorService> executorProvider;
    private final Set<Runnable> submitted = new LinkedHashSet<>(0);
    private ScheduledExecutorService executor;

    public Scheduler() {
        this(1);
    }

    public Scheduler(final int poolSize) {
        this.executorProvider = () -> SchedulerServiceLoader.load().newScheduledExecutorService(poolSize);
        this.executor = executorProvider.get();
    }

    public static Scheduler inBackground() {
        return BACKGROUND_SCHEDULER;
    }

    public void submit(final Runnable toSchedule) {
        this.submit(toSchedule, Scheduler.REFRESH);
    }

    public Future<?> run(final Runnable toRun) {
        Scheduler.LOGGER.debug("Scheduling {} immediately.", toRun);
        return executor.submit(toRun);
    }

    public void submit(final Runnable toSchedule, final TemporalAmount delayInBetween) {
        submit(toSchedule, delayInBetween, Duration.ZERO);
    }

    public void submit(final Runnable toSchedule, final TemporalAmount delayInBetween,
                       final TemporalAmount firstDelay) {
        final long firstDelayInSeconds = firstDelay.get(ChronoUnit.SECONDS);
        final long delayInSeconds = delayInBetween.get(ChronoUnit.SECONDS);
        Scheduler.LOGGER.debug("Scheduling {} every {} seconds, starting in {} seconds.", toSchedule, delayInSeconds,
                               firstDelayInSeconds);
        executor.scheduleWithFixedDelay(toSchedule, firstDelayInSeconds, delayInSeconds, TimeUnit.SECONDS);
        this.submitted.add(toSchedule);
    }

    public boolean isSubmitted(final Runnable refreshable) {
        return submitted.contains(refreshable);
    }

    public void shutdown() {
        Scheduler.LOGGER.debug("Shutting down.");
        executor.shutdownNow();
    }

}
