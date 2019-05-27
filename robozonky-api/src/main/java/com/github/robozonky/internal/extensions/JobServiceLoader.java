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

package com.github.robozonky.internal.extensions;

import java.util.Collection;
import java.util.ServiceLoader;
import java.util.function.Function;
import java.util.stream.Stream;

import com.github.robozonky.internal.jobs.JobService;
import com.github.robozonky.internal.jobs.SimpleJob;
import com.github.robozonky.internal.jobs.TenantJob;
import com.github.robozonky.internal.util.StreamUtil;
import io.vavr.Lazy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class JobServiceLoader {

    private static final Logger LOGGER = LogManager.getLogger(JobServiceLoader.class);
    private static final Lazy<ServiceLoader<JobService>> LOADER =
            ExtensionsManager.INSTANCE.getServiceLoader(JobService.class);

    private JobServiceLoader() {
        // no instances
    }

    static <T> Stream<T> load(final Iterable<JobService> loader,
                              final Function<JobService, Collection<T>> jobProvider) {
        LOGGER.debug("Looking up batch jobs.");
        return StreamUtil.toStream(loader)
                .peek(cp -> LOGGER.trace("Evaluating job service '{}'.", cp.getClass()))
                .flatMap(cp -> jobProvider.apply(cp).stream());
    }

    public static Stream<SimpleJob> loadSimpleJobs() {
        return load(LOADER.get(), JobService::getSimpleJobs);
    }

    public static Stream<TenantJob> loadTenantJobs() {
        return load(LOADER.get(), JobService::getTenantJobs);
    }
}

