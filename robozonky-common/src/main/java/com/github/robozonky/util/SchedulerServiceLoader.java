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

import java.util.ServiceLoader;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Loads pluggable {@link SchedulerService}s using {@link ServiceLoader}. If no such are available, will use a default
 * one, which is just a {@link Executors#newScheduledThreadPool(int, ThreadFactory)}.
 * <p>
 * The only actual providers for this service should be coming from robozonky-test module, as the sole purpose of this
 * service is to facilitate testing on RoboZonky's background tasks.
 */
final class SchedulerServiceLoader {

    private static final ServiceLoader<SchedulerService> LOADER = ServiceLoader.load(SchedulerService.class);
    private static final ThreadFactory THREAD_FACTORY = new RoboZonkyThreadFactory(new ThreadGroup("rzBackground"));

    private static final SchedulerService REAL_SCHEDULER =
            parallelism -> Executors.newScheduledThreadPool(parallelism, THREAD_FACTORY);

    public static SchedulerService load() {
        return StreamUtil.toStream(LOADER).findAny().orElse(REAL_SCHEDULER);
    }
}
