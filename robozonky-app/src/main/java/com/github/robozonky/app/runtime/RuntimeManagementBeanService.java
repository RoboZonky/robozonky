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

package com.github.robozonky.app.runtime;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

import com.github.robozonky.common.management.ManagementBean;
import com.github.robozonky.common.management.ManagementBeanService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RuntimeManagementBeanService implements ManagementBeanService {

    private static final AtomicReference<ManagementBean<?>> BEAN = new AtomicReference<>();
    private static final Logger LOGGER = LoggerFactory.getLogger(RuntimeManagementBeanService.class);

    static boolean setManagementBean(final ManagementBean<?> mBean) {
        return BEAN.compareAndSet(null, mBean);
    }

    @Override
    public Collection<ManagementBean<?>> getManagementBeans() {
        final ManagementBean<?> stored = BEAN.get();
        if (stored == null) {
            LOGGER.debug("No runtime management bean found.");
            return Collections.emptySet();
        } else {
            LOGGER.debug("Runtime management bean found: {}.", stored);
            return Collections.singleton(stored);
        }
    }
}
