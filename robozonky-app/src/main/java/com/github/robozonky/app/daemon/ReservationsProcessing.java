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

import java.util.Collection;
import java.util.stream.Collectors;

import com.github.robozonky.api.remote.entities.ReservationPreferences;
import com.github.robozonky.api.strategies.ReservationDescriptor;
import com.github.robozonky.api.strategies.ReservationStrategy;
import com.github.robozonky.app.tenant.PowerTenant;
import com.github.robozonky.common.jobs.TenantPayload;
import com.github.robozonky.common.remote.Zonky;
import com.github.robozonky.common.tenant.Tenant;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

final class ReservationsProcessing implements TenantPayload {

    private static final Logger LOGGER = LogManager.getLogger();

    private static void process(final PowerTenant tenant, final ReservationStrategy strategy) {
        final ReservationPreferences preferences = tenant.call(Zonky::getReservationPreferences);
        if (!ReservationPreferences.isEnabled(preferences)) {
            LOGGER.info("Reservation system is disabled or there are no active categories.");
            return;
        }
        final Collection<ReservationDescriptor> reservations = tenant.call(Zonky::getPendingReservations)
                .filter(r -> !tenant.getLoan(r.getId()).getMyInvestment().isPresent())
                .map(r -> new ReservationDescriptor(r, () -> tenant.getLoan(r.getId())))
                .collect(Collectors.toList());
        ReservationSession.process(tenant, reservations, strategy);
    }

    @Override
    public void accept(final Tenant tenant) {
        tenant.getReservationStrategy().ifPresent(s -> process((PowerTenant) tenant, s));
    }
}
