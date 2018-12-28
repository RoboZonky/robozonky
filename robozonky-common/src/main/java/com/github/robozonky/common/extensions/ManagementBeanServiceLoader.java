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

import com.github.robozonky.common.management.ManagementBean;
import com.github.robozonky.common.management.ManagementBeanService;
import com.github.robozonky.util.StreamUtil;
import io.vavr.Lazy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ManagementBeanServiceLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManagementBeanServiceLoader.class);
    private static final Lazy<ServiceLoader<ManagementBeanService>> LOADER =
            ExtensionsManager.INSTANCE.getServiceLoader(ManagementBeanService.class);

    private ManagementBeanServiceLoader() {
        // no instances
    }

    public static Stream<ManagementBean<?>> loadManagementBeans() {
        ManagementBeanServiceLoader.LOGGER.debug("Looking up management beans.");
        return StreamUtil.toStream(LOADER.get())
                .peek(cp -> ManagementBeanServiceLoader.LOGGER.trace("Evaluating management bean '{}'.", cp.getClass()))
                .flatMap(cp -> cp.getManagementBeans().stream());
    }

}

