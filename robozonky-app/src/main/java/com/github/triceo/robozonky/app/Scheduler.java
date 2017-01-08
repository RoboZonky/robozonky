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

package com.github.triceo.robozonky.app;

import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAmount;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import com.github.triceo.robozonky.api.ReturnCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Scheduler implements ShutdownHook.Handler {

    private static final Logger LOGGER = LoggerFactory.getLogger(Scheduler.class);

    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);

    private final Collection<ScheduledFuture<?>> futures = new LinkedHashSet<>();

    public void submit(final Runnable toSchedule, final TemporalAmount delayInBetween) {
        final long delayInSeconds = delayInBetween.get(ChronoUnit.SECONDS);
        Scheduler.LOGGER.debug("Scheduling {} every {} seconds.", toSchedule, delayInSeconds);
        final ScheduledFuture<?> future =
                executor.scheduleWithFixedDelay(toSchedule, 0, delayInSeconds, TimeUnit.SECONDS);
        this.futures.add(future);
    }

    @Override
    public Optional<Consumer<ReturnCode>> get() {
        return Optional.of(returnCode -> {
            Scheduler.LOGGER.debug("Shutting down.");
            futures.forEach(future -> future.cancel(true));
            executor.shutdownNow();
        });
    }
}
