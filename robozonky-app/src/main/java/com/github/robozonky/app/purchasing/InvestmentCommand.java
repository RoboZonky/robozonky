/*
 * Copyright 2017 The RoboZonky Project
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

package com.github.robozonky.app.purchasing;

import java.util.function.Consumer;

import com.github.robozonky.api.notifications.PurchaseRecommendedEvent;
import com.github.robozonky.api.strategies.PurchaseStrategy;
import com.github.robozonky.app.Events;

final class InvestmentCommand implements Consumer<Session> {

    private final PurchaseStrategy strategy;

    public InvestmentCommand(final PurchaseStrategy strategy) {
        this.strategy = strategy;
    }

    public void accept(final Session s) {
        boolean invested;
        do {
            invested = strategy.recommend(s.getAvailable(), s.getPortfolioOverview())
                    .peek(r -> Events.fire(new PurchaseRecommendedEvent(r)))
                    .anyMatch(s::purchase); // keep trying until investment opportunities are exhausted
        } while (invested);
    }
}
