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

package com.github.robozonky.api.strategies;

import java.util.function.Supplier;

import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.api.remote.entities.Reservation;

/**
 * Determines which {@link Reservation}s will be invested into out of those coming through the reservation system,
 * and how much will be invested.
 * What the strategy does or does not allow depends on the {@link StrategyService} implementation.
 */
public interface ReservationStrategy {

    ReservationMode getMode();

    /**
     * Determine whether a participation is acceptable by the strategy.
     * 
     * @param reservationDescriptor     Reservation to be evaluated for acceptability.
     * @param portfolioOverviewSupplier Retrieves the latest available information on-demand.
     * @param sessionInfo               Information about the current session.
     * @return True if acceptable.
     */
    boolean recommend(ReservationDescriptor reservationDescriptor,
            Supplier<PortfolioOverview> portfolioOverviewSupplier, SessionInfo sessionInfo);
}
