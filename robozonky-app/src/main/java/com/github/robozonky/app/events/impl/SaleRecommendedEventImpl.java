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

package com.github.robozonky.app.events.impl;

import java.util.function.Supplier;

import com.github.robozonky.api.notifications.SaleRecommendedEvent;
import com.github.robozonky.api.remote.entities.Investment;
import com.github.robozonky.api.remote.entities.SellInfo;
import com.github.robozonky.api.strategies.InvestmentDescriptor;
import com.github.robozonky.api.strategies.RecommendedInvestment;

final class SaleRecommendedEventImpl
        extends AbstractRecommendationBasedEventImpl<RecommendedInvestment, InvestmentDescriptor, Investment>
        implements SaleRecommendedEvent {

    private final Supplier<SellInfo> sellInfoSupplier;

    public SaleRecommendedEventImpl(final RecommendedInvestment recommendation,
            final Supplier<SellInfo> sellInfoSupplier) {
        super(recommendation);
        this.sellInfoSupplier = sellInfoSupplier;
    }

    @Override
    public Investment getInvestment() {
        return super.getRecommending();
    }

    @Override
    public SellInfo getSellInfo() {
        return sellInfoSupplier.get();
    }
}
