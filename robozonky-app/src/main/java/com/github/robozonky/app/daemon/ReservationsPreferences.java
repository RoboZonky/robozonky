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

package com.github.robozonky.app.daemon;

import com.github.robozonky.api.remote.entities.ReservationPreferences;
import com.github.robozonky.api.strategies.ReservationMode;
import com.github.robozonky.api.strategies.ReservationStrategy;
import com.github.robozonky.app.tenant.PowerTenant;
import com.github.robozonky.common.jobs.TenantPayload;
import com.github.robozonky.common.remote.Zonky;
import com.github.robozonky.common.tenant.Tenant;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

final class ReservationsPreferences implements TenantPayload {

    private static final Logger LOGGER = LogManager.getLogger();

    private static void process(final PowerTenant tenant, final ReservationStrategy strategy) {
        if (strategy.getMode() != ReservationMode.FULL_OWNERSHIP) {
            LOGGER.debug("Keeping existing reservation preferences on account of strategy configuration.");
            return;
        }
        final ReservationPreferences preferences = tenant.call(Zonky::getReservationPreferences);
        if (!ReservationPreferences.isEnabled(preferences)) {
            LOGGER.info("Reservation system is disabled and will be enabled.");
            enableReservationSystem(tenant);
            return;
        }
        LOGGER.trace("Retrieved reservation preferences: {}.", preferences);
        if (preferences.equals(ReservationPreferences.TOTAL.get())) {
            LOGGER.debug("Keeping existing reservation preferences on account of them being complete.");
            return;
        }
        enableReservationSystem(tenant);
    }

    private static void enableReservationSystem(final PowerTenant tenant) {
        LOGGER.info("Enabling reservation system and overriding existing settings.");
        tenant.run(z -> z.setReservationPreferences(ReservationPreferences.TOTAL.get()));
    }

    @Override
    public void accept(final Tenant tenant) {
        tenant.getReservationStrategy().ifPresent(s -> process((PowerTenant) tenant, s));
    }
}
