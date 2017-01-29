/*
 * Copyright 2017 Lukáš Petrovický
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

package com.github.triceo.robozonky.app.util;

import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAmount;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import com.github.triceo.robozonky.api.Refreshable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Scheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(Scheduler.class);
    public static final Scheduler BACKGROUND_SCHEDULER = new Scheduler(1);

    private final Supplier<ScheduledExecutorService> executorProvider;
    private ScheduledExecutorService executor;
    private final Set<Refreshable<?>> submitted = new LinkedHashSet<>();

    public Scheduler() {
        this(1);
    }

    public Scheduler(final int poolSize) {
        this.executorProvider = () -> Executors.newScheduledThreadPool(poolSize);
        this.executor = executorProvider.get();
    }

    private void actuallySubmit(final Refreshable<?> toSchedule, final TemporalAmount delayInBetween) {
        final long delayInSeconds = delayInBetween.get(ChronoUnit.SECONDS);
        Scheduler.LOGGER.debug("Scheduling {} every {} seconds.", toSchedule, delayInSeconds);
        this.submitted.add(toSchedule);
        executor.scheduleWithFixedDelay(toSchedule, 0, delayInSeconds, TimeUnit.SECONDS);
    }

    public void submit(final Refreshable<?> toSchedule, final TemporalAmount delayInBetween) {
        final Optional<Refreshable<?>> maybeDependedOn = toSchedule.getDependedOn();
        maybeDependedOn.ifPresent(dependedOn -> {
            this.submit(dependedOn, delayInBetween); // make sure the parent's parent is also submitted
        });
        if (!isSubmitted(toSchedule)) {
            this.actuallySubmit(toSchedule, delayInBetween);
        }
    }

    public boolean isSubmitted(final Refreshable<?> refreshable) {
        return submitted.contains(refreshable);
    }

    public void shutdown() {
        Scheduler.LOGGER.debug("Shutting down.");
        executor.shutdownNow();
    }

    public boolean isShutdown() {
        return executor.isShutdown();
    }

    /**
     * Re-start the scheduler following {@link #shutdown()}. This is only to help with testing.
     */
    public void reinit() {
        if (!isShutdown()) {
            Scheduler.LOGGER.debug("Not reinitializing scheduler as it is not yet shut down.");
            return;
        }
        this.submitted.clear();
        this.executor = executorProvider.get();
    }
}
