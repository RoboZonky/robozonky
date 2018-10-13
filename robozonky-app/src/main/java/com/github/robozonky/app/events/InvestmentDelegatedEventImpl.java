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

package com.github.robozonky.app.events;

import java.math.BigDecimal;

import com.github.robozonky.api.notifications.InvestmentDelegatedEvent;
import com.github.robozonky.api.remote.entities.sanitized.MarketplaceLoan;
import com.github.robozonky.api.strategies.RecommendedLoan;

final class InvestmentDelegatedEventImpl extends AbstractEventImpl implements InvestmentDelegatedEvent {

    private final MarketplaceLoan loan;
    private final BigDecimal recommendation;
    private final String confirmationProviderId;

    public InvestmentDelegatedEventImpl(final RecommendedLoan recommendation, final String confirmationProviderId) {
        this.loan = recommendation.descriptor().item();
        this.recommendation = recommendation.amount();
        this.confirmationProviderId = confirmationProviderId;
    }

    @Override
    public String getConfirmationProviderId() {
        return confirmationProviderId;
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
