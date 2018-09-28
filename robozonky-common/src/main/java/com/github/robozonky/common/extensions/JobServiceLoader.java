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

package com.github.robozonky.common.extensions;

import java.util.ServiceLoader;
import java.util.stream.Stream;

import com.github.robozonky.common.jobs.Job;
import com.github.robozonky.common.jobs.JobService;
import com.github.robozonky.internal.util.LazyInitialized;
import com.github.robozonky.util.StreamUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class JobServiceLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobServiceLoader.class);
    private static final LazyInitialized<ServiceLoader<JobService>> LOADER =
            ExtensionsManager.INSTANCE.getServiceLoader(JobService.class);

    private JobServiceLoader() {
        // no instances
    }

    static Stream<Job> load(final Iterable<JobService> loader) {
        JobServiceLoader.LOGGER.debug("Looking up batch jobs.");
        return StreamUtil.toStream(loader)
                .peek(cp -> JobServiceLoader.LOGGER.trace("Evaluating job service '{}'.", cp.getClass()))
                .flatMap(cp -> cp.getJobs().stream());
    }

    public static Stream<Job> load() {
        return JobServiceLoader.load(JobServiceLoader.LOADER.get());
    }
}

