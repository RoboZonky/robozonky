/*
 * Copyright 2018 The RoboZonky Project
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

import java.math.BigDecimal;

import com.github.robozonky.api.notifications.InvestmentRequestedEvent;
import com.github.robozonky.api.remote.entities.sanitized.MarketplaceLoan;
import com.github.robozonky.api.strategies.RecommendedLoan;

final class InvestmentRequestedEventImpl extends AbstractEventImpl implements InvestmentRequestedEvent {

    private final MarketplaceLoan loan;
    private final BigDecimal recommendation;

    public InvestmentRequestedEventImpl(final RecommendedLoan recommendation) {
        this.loan = recommendation.descriptor().item();
        this.recommendation = recommendation.amount();
    }

    @Override
    public MarketplaceLoan getLoan() {
        return loan;
    }

    @Override
    public BigDecimal getRecommendation() {
        return recommendation;
    }
}
