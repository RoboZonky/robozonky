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

import com.github.robozonky.util.Refreshable;
import com.github.robozonky.util.Schedulers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Will pause and/or resume all background tasks, effectively pausing the application, based on the availability of
 * Zonky servers as notified by {@link LivenessCheck}.
 */
class SchedulerControl implements Refreshable.RefreshListener<ApiVersion> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SchedulerControl.class);

    @Override
    public void valueSet(final ApiVersion newValue) {
        Schedulers.INSTANCE.resume();
        LOGGER.info("Zonky servers are available, API version {} detected.", newValue.getBuildVersion());
    }

    @Override
    public void valueUnset(final ApiVersion oldValue) {
        LOGGER.info("Pausing RoboZonky on account of Zonky servers not being available.");
        Schedulers.INSTANCE.pause();
    }
}
