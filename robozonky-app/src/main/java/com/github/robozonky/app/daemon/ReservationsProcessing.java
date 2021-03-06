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

package com.github.robozonky.app.daemon;

import org.apache.logging.log4j.Logger;

import com.github.robozonky.api.strategies.ReservationDescriptor;
import com.github.robozonky.api.strategies.ReservationStrategy;
import com.github.robozonky.app.tenant.PowerTenant;
import com.github.robozonky.internal.jobs.TenantPayload;
import com.github.robozonky.internal.remote.Zonky;
import com.github.robozonky.internal.remote.entities.ReservationPreferencesImpl;
import com.github.robozonky.internal.tenant.Tenant;

final class ReservationsProcessing implements TenantPayload {

    private static final Logger LOGGER = Audit.reservations();

    private static void process(final PowerTenant tenant, final ReservationStrategy strategy) {
        var preferences = tenant.call(Zonky::getReservationPreferences);
        if (!ReservationPreferencesImpl.isEnabled(preferences)) {
            LOGGER.info("Reservation system is disabled or there are no active categories.");
            return;
        }
        /*
         * Do not make this parallel.
         * Each reservation will be processed individually and if done in parallel, the portfolio structure could go
         * haywire.
         * The code is designed to invest, rebuild the portfolio structure, and then invest again.
         */
        var reservations = tenant.call(Zonky::getPendingReservations)
            .map(r -> new ReservationDescriptor(r, () -> tenant.getLoan(r.getId())));
        ReservationSession.process(tenant, reservations, strategy);
    }

    @Override
    public void accept(final Tenant tenant) {
        tenant.getReservationStrategy()
            .ifPresent(s -> process((PowerTenant) tenant, s));
    }
}
