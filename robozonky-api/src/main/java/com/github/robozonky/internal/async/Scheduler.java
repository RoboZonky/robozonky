/*
 * Copyright 2020 The RoboZonky Project
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

package com.github.robozonky.internal.async;

import java.time.Duration;

public interface Scheduler extends AutoCloseable {

    static Scheduler create() {
        return new CachedThreadPoolBasedScheduler();
    }

    /**
     *
     * @param toSchedule     Task to schedule.
     * @param delayInBetween Delay between the first task's end and second task's start.
     * @param firstDelay     The delay before the first instance of the task is scheduled.
     * @param timeout        Maximum run time for a single instance of the scheduled task. Only used when greater than
     *                       0.
     * @return never null
     */
    TaskDescriptor submit(final Runnable toSchedule, final Duration delayInBetween, final Duration firstDelay,
            final Duration timeout);

    boolean isClosed();

}
