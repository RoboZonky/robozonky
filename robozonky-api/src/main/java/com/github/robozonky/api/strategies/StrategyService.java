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

import java.util.Optional;
import java.util.ServiceLoader;

/**
 * Use Java's {@link ServiceLoader} to load different strategy implementations.
 */
public interface StrategyService {

    /**
     * Prepare investing strategy for being used by the app.
     * @param strategy Investment strategy in question.
     * @return Processed instance of the strategy provided by the user, if the input format is supported.
     */
    Optional<InvestmentStrategy> toInvest(String strategy);

    /**
     * Prepare selling strategy for being used by the app.
     * @param strategy Investment strategy in question.
     * @return Processed instance of the strategy provided by the user, if the input format is supported.
     */
    Optional<SellStrategy> toSell(String strategy);

    /**
     * Prepare purchasing strategy for being used by the app.
     * @param strategy Investment strategy in question.
     * @return Processed instance of the strategy provided by the user, if the input format is supported.
     */
    Optional<PurchaseStrategy> toPurchase(String strategy);

    /**
     * Prepare investing strategy for being used by the app. The investments in this case will be coming from the
     * reservations susystem.
     * @param strategy Investment strategy in question.
     * @return Processed instance of the strategy provided by the user, if the input format is supported.
     */
    Optional<ReservationStrategy> forReservations(String strategy);
}
