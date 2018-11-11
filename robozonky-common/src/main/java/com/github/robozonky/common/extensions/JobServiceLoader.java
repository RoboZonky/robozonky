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

import java.util.Collection;
import java.util.ServiceLoader;
import java.util.function.Function;
import java.util.stream.Stream;

import com.github.robozonky.common.jobs.JobService;
import com.github.robozonky.common.jobs.SimpleJob;
import com.github.robozonky.common.jobs.TenantJob;
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

    static <T> Stream<T> load(final Iterable<JobService> loader, Function<JobService, Collection<T>> jobProvider) {
        JobServiceLoader.LOGGER.debug("Looking up batch jobs.");
        return StreamUtil.toStream(loader)
                .peek(cp -> JobServiceLoader.LOGGER.trace("Evaluating job service '{}'.", cp.getClass()))
                .flatMap(cp -> jobProvider.apply(cp).stream());
    }

    public static Stream<SimpleJob> loadSimpleJobs() {
        return JobServiceLoader.load(JobServiceLoader.LOADER.get(), JobService::getSimpleJobs);
    }

    public static Stream<TenantJob> loadTenantJobs() {
        return JobServiceLoader.load(JobServiceLoader.LOADER.get(), JobService::getTenantJobs);
    }
}

