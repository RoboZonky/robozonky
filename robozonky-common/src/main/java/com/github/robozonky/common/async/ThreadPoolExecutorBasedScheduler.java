/*
 * Copyright 2019 The RoboZonky Project
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.github.robozonky.internal.api.Settings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

final class ThreadPoolExecutorBasedScheduler implements Scheduler {

    private static final Logger LOGGER = LogManager.getLogger(SchedulerImpl.class);
    private static final Duration REFRESH = Settings.INSTANCE.getRemoteResourceRefreshInterval();
    private final ScheduledExecutorService schedulingExecutor;
    private final ExecutorService executor;
    private final Runnable onClose;

    public ThreadPoolExecutorBasedScheduler(final ScheduledExecutorService schedulingExecutor,
                                            final ExecutorService actualExecutor, final Runnable onClose) {
        this.schedulingExecutor = schedulingExecutor;
        this.executor = actualExecutor;
        this.onClose = onClose;
    }

    private Future<?> submitToExecutor(final Runnable toSchedule) {
        LOGGER.debug("Submitting {} for actual execution.", toSchedule);
        return executor.submit(toSchedule);
    }

    @Override
    public ScheduledFuture<?> submit(final Runnable toSchedule) {
        return submit(toSchedule, REFRESH);
    }

    @Override
    public ScheduledFuture<?> submit(final Runnable toSchedule, final Duration delayInBetween) {
        return submit(toSchedule, REFRESH, Duration.ZERO);
    }

    @Override
    public ScheduledFuture<?> submit(final Runnable toSchedule, final Duration delayInBetween,
                                     final Duration firstDelay) {
        LOGGER.debug("Scheduling {} every {} ms, starting in {} ms.", toSchedule, delayInBetween.toMillis(),
                     firstDelay.toMillis());
        final Runnable toSubmit = () -> submitToExecutor(toSchedule);
        return schedulingExecutor.scheduleWithFixedDelay(toSubmit, firstDelay.toNanos(), delayInBetween.toNanos(),
                                                         TimeUnit.NANOSECONDS);
    }

    @Override
    public boolean isClosed() {
        return executor.isShutdown();
    }

    @Override
    public ExecutorService getExecutor() {
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
