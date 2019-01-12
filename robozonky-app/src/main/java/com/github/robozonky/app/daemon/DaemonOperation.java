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

package com.github.robozonky.app.daemon;

import java.time.Duration;

import com.github.robozonky.app.tenant.PowerTenant;
import com.github.robozonky.common.tenant.Tenant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class DaemonOperation implements Runnable {

    protected final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    private final Duration refreshInterval;
    private final PowerTenant tenant;

    protected DaemonOperation(final PowerTenant auth, final Duration refreshInterval) {
        this.tenant = auth;
        this.refreshInterval = refreshInterval;
    }

    protected abstract boolean isEnabled(final Tenant tenant);

    protected abstract boolean hasStrategy(final Tenant tenant);

    protected abstract void execute(final Tenant tenant);

    public Duration getRefreshInterval() {
        return this.refreshInterval;
    }

    @Override
    public void run() {
        LOGGER.trace("Starting.");
        if (!isEnabled(tenant)) {
            LOGGER.info("Access to marketplace disabled by Zonky.");
        } else if (!hasStrategy(tenant)) {
            LOGGER.info("Asleep as there is no strategy.");
        } else {
            execute(tenant);
        }
        LOGGER.trace("Finished.");
    }
}
