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

import java.util.concurrent.ThreadFactory;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public enum Tasks implements AutoCloseable {

    MISSION_CRITICAL(threadGroup("rzCritical", Thread.MAX_PRIORITY)),
    SUPPORTING(threadGroup("rzSupporting", Thread.NORM_PRIORITY)),
    BACKGROUND(threadGroup("rzBackground", Thread.MIN_PRIORITY));

    private static final Logger LOGGER = LogManager.getLogger();
    private final ThreadFactory threadFactory;
    private final Reloadable<? extends Scheduler> scheduler;

    Tasks(final ThreadGroup threadGroup) {
        this.threadFactory = new RoboZonkyThreadFactory(threadGroup);
        this.scheduler = Reloadable.with(() -> newScheduler(threadFactory)).build();
    }

    private static ThreadGroup threadGroup(final String name, final int maxPriority) {
        final ThreadGroup threadGroup = new ThreadGroup(name);
        threadGroup.setMaxPriority(maxPriority);
        return threadGroup;
    }

    public static void closeAll() {
        Stream.of(values()).forEach(s -> {
            try {
                s.close();
            } catch (final Exception ex) { // nothing much to do here
                LOGGER.debug("Failed closing scheduler {}.", s, ex);
            }
        });
    }

    private Scheduler newScheduler(final ThreadFactory threadFactory) {
        LOGGER.debug("Instantiating new background scheduler {}.", this);
        return new SchedulerImpl(Integer.MAX_VALUE, threadFactory, this::cleanBackgroundScheduler);
    }

    private void cleanBackgroundScheduler() {
        scheduler.clear();
        LOGGER.trace("Cleared background scheduler.");
    }

    public Scheduler scheduler() {
        return scheduler.get().getOrElseThrow(() -> new IllegalStateException("Impossible."));
    }

    public Thread newThread(final Runnable r) {
        return threadFactory.newThread(r);
    }

    @Override
    public void close() throws Exception {
        if (!scheduler.hasValue()) {
            return;
        }
        scheduler().close();
    }
}
