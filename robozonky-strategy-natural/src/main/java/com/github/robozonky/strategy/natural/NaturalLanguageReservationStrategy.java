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

package com.github.robozonky.strategy.natural;

import static com.github.robozonky.strategy.natural.Audit.LOGGER;

import java.util.function.Supplier;

import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.api.strategies.ReservationDescriptor;
import com.github.robozonky.api.strategies.ReservationMode;
import com.github.robozonky.api.strategies.ReservationStrategy;

class NaturalLanguageReservationStrategy implements ReservationStrategy {

    private final ParsedStrategy strategy;

    public NaturalLanguageReservationStrategy(final ParsedStrategy p) {
        this.strategy = p;
    }

    @Override
    public ReservationMode getMode() {
        return strategy.getReservationMode()
            .orElseThrow(() -> new IllegalStateException("Reservations are not enabled, yet strategy exists."));
    }

    @Override
    public boolean recommend(final ReservationDescriptor reservationDescriptor,
            final Supplier<PortfolioOverview> portfolioOverviewSupplier,
            final SessionInfo sessionInfo) {
        var portfolio = portfolioOverviewSupplier.get();
        if (!Util.isAcceptable(strategy, portfolio)) {
            return false;
        }
        var reservation = reservationDescriptor.item();
        LOGGER.trace("Evaluating {}.", reservation);
        var preferences = Preferences.get(strategy, portfolio);
        var isAcceptable = preferences.isDesirable(reservation.getRating());
        if (!isAcceptable) {
            LOGGER.debug("Reservation #{} skipped due to an undesirable rating.", reservation.getId());
            return false;
        }
        return strategy.isApplicable(reservationDescriptor, portfolio);
    }

}
