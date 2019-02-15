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

package com.github.robozonky.api.strategies;

import java.util.Collection;
import java.util.stream.Stream;

import com.github.robozonky.api.remote.entities.Restrictions;
import com.github.robozonky.api.remote.entities.sanitized.Reservation;

/**
 * Determines which {@link Reservation}s will be invested into out of those coming through the reservation system,
 * and how much will be invested. What the strategy does or does not allow depends on the {@link StrategyService}
 * implementation.
 */
public interface ReservationStrategy {

    ReservationMode getMode();

    /**
     * Retrieve reservations that are acceptable by the strategy, in the order in which they are to be evaluated. After
     * an investment has been made into any single one of these reservations, the strategy should be called again to
     * re-evaluate the resulting situation.
     * @param available Reservations to be evaluated for acceptability.
     * @param portfolio Aggregation of information as to the user's current portfolio.
     * @param restrictions Restrictions imposed by Zonky on the current user.
     * @return Acceptable reservations, in the order of their decreasing priority, mapped to the recommended investment
     * amounts.
     */
    Stream<RecommendedReservation> recommend(Collection<ReservationDescriptor> available, PortfolioOverview portfolio,
                                             Restrictions restrictions);
}
