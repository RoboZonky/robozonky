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

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ThreadFactory;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Provides different {@link Scheduler} instances based on the needs of a particular task. There are three categories
 * of tasks:
 *
 * <ul>
 * <li>{@link #MISSION_CRITICAL} tasks are essentially just the primary and secondary marketplace checks, which
 * are absolutely necessary, must be scheduled immediately and processed as soon as possible. There will be a fixed
 * pool of 2 threads always available to execute these tasks, one for each marketplace. These tasks will get
 * {@link Thread#MAX_PRIORITY}.</li>
 * <li>{@link #SUPPORTING} tasks carry functionality which is still very important for the robot or for the user,
 * but can be postponed momentarily. These would be tasks such as selling participations or updating strategies from
 * a remote server. There will be 1 thread always available to execute these tasks, and it will be given
 * {@link Thread#NORM_PRIORITY}.</li>
 * <li>{@link #BACKGROUND} tasks carry functionality which is either unimportant or can be easily postponed. There will
 * be up to 1 thread available to execute these tasks, and it will be given {@link Thread#MIN_PRIORITY}.</li>
 * </ul>
 * <p>
 * None of these tasks are restricted from spinning up their own threads. In fact, many of the {@link #MISSION_CRITICAL}
 * and {@link #SUPPORTING} tasks may start entire {@link ForkJoinPool}s as a side-effect of using
 * {@link Stream#parallel()}.
 */
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
        Stream.of(values()).forEach(Tasks::close);
    }

    private Scheduler newScheduler(final ThreadFactory threadFactory) {
        LOGGER.debug("Instantiating new background scheduler {}.", this);
        return new SchedulerImpl(Integer.MAX_VALUE, threadFactory, this::clear);
    }

    private void clear() {
        scheduler.clear();
        LOGGER.debug("Cleared background scheduler: {}.", this);
    }

    public Scheduler scheduler() {
        return scheduler.get().getOrElseThrow(() -> new IllegalStateException("Impossible."));
    }

    @Override
    public void close() {
        if (!scheduler.hasValue()) {
            LOGGER.debug("No scheduler to close: {}.", this);
            return;
        }
        try {
            scheduler().close();
        } catch (final Exception ex) { // nothing we can actually do here
            LOGGER.debug("Failed closing scheduler {}.", this, ex);
        }
    }
}
