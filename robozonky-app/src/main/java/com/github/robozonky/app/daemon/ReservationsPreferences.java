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

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.robozonky.api.remote.entities.ReservationPreference;
import com.github.robozonky.api.remote.entities.ReservationPreferences;
import com.github.robozonky.api.remote.enums.LoanTermInterval;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.ReservationMode;
import com.github.robozonky.api.strategies.ReservationStrategy;
import com.github.robozonky.app.tenant.PowerTenant;
import com.github.robozonky.common.jobs.TenantPayload;
import com.github.robozonky.common.remote.Zonky;
import com.github.robozonky.common.tenant.Tenant;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

final class ReservationsPreferences implements TenantPayload {

    private static final Logger LOGGER = LogManager.getLogger();

    private static Stream<Tuple2<Rating, LoanTermInterval>> enumerateAllCategories() {
        return Arrays.stream(Rating.values())
                .flatMap(r -> Arrays.stream(LoanTermInterval.values()).map(i -> Tuple.of(r, i)));
    }

    private static boolean matches(final ReservationPreference preference,
                                   final Tuple2<Rating, LoanTermInterval> properties) {
        if (preference.isInsuredOnly()) {
            return false;
        } else if (preference.getLoanTermInterval() != properties._2) {
            return false;
        } else if (preference.getRatingType() != properties._1) {
            return false;
        } else {
            return true;
        }
    }

    private static boolean isEnabled(final Collection<ReservationPreference> preferences,
                                     final Tuple2<Rating, LoanTermInterval> properties) {
        return preferences.stream().anyMatch(p -> matches(p, properties));
    }

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
        final Collection<Tuple2<Rating, LoanTermInterval>> missing = enumerateAllCategories()
                .filter(t -> !isEnabled(preferences.getReservationPreferences(), t))
                .collect(Collectors.toList());
        if (missing.isEmpty()) {
            LOGGER.debug("Keeping existing reservation preferences on account of them being complete.");
        }
        LOGGER.debug("Reservation preferences missing: {}.", missing);
        enableReservationSystem(tenant);
    }

    private static void enableReservationSystem(final PowerTenant tenant) {
        LOGGER.info("Enabling reservation system and overriding existing settings.");
        final ReservationPreference[] allCategories = enumerateAllCategories()
                .map(c -> new ReservationPreference(c._2, c._1, false))
                .toArray(ReservationPreference[]::new);
        final ReservationPreferences p = new ReservationPreferences(allCategories);
        tenant.run(z -> z.setReservationPreferences(p));
    }

    @Override
    public void accept(final Tenant tenant) {
        tenant.getReservationStrategy().ifPresent(s -> process((PowerTenant) tenant, s));
    }
}
