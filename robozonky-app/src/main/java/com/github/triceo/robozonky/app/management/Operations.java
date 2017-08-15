/*
 * Copyright 2017 Lukáš Petrovický
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

package com.github.triceo.robozonky.app.management;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import com.github.triceo.robozonky.api.notifications.Event;
import com.github.triceo.robozonky.api.notifications.InvestmentDelegatedEvent;
import com.github.triceo.robozonky.api.notifications.InvestmentMadeEvent;
import com.github.triceo.robozonky.api.notifications.InvestmentPurchasedEvent;
import com.github.triceo.robozonky.api.notifications.InvestmentRejectedEvent;
import com.github.triceo.robozonky.api.notifications.SaleOfferedEvent;
import com.github.triceo.robozonky.api.strategies.RecommendedLoan;

class Operations implements OperationsMBean {

    private OffsetDateTime lastInvestmentRunTimestamp;
    private final Map<Integer, Integer> successfulInvestments = new LinkedHashMap<>(),
            delegatedInvestments = new LinkedHashMap<>(), rejectedInvestments = new LinkedHashMap<>(),
            purchasedInvestments = new LinkedHashMap<>(), offeredInvestments = new LinkedHashMap<>();

    @Override
    public Map<Integer, Integer> getSuccessfulInvestments() {
        return this.successfulInvestments;
    }

    @Override
    public Map<Integer, Integer> getDelegatedInvestments() {
        return this.delegatedInvestments;
    }

    @Override
    public Map<Integer, Integer> getRejectedInvestments() {
        return this.rejectedInvestments;
    }

    @Override
    public Map<Integer, Integer> getPurchasedInvestments() {
        return this.purchasedInvestments;
    }

    @Override
    public Map<Integer, Integer> getOfferedInvestments() {
        return this.offeredInvestments;
    }

    @Override
    public OffsetDateTime getLatestUpdatedDateTime() {
        return this.lastInvestmentRunTimestamp;
    }

    void handle(final InvestmentMadeEvent event) {
        this.successfulInvestments.put(event.getInvestment().getLoanId(), event.getInvestment().getAmount());
        registerInvestmentRun(event);
    }

    void handle(final InvestmentDelegatedEvent event) {
        final RecommendedLoan r = event.getRecommendation();
        this.delegatedInvestments.put(r.descriptor().item().getId(), r.amount().intValue());
        registerInvestmentRun(event);
    }

    void handle(final InvestmentRejectedEvent event) {
        final RecommendedLoan r = event.getRecommendation();
        this.rejectedInvestments.put(r.descriptor().item().getId(), r.amount().intValue());
        registerInvestmentRun(event);
    }

    void handle(final SaleOfferedEvent event) {
        this.offeredInvestments.put(event.getInvestment().getLoanId(), event.getInvestment().getAmount());
        registerInvestmentRun(event);
    }

    void handle(final InvestmentPurchasedEvent event) {
        this.purchasedInvestments.put(event.getInvestment().getLoanId(), event.getInvestment().getAmount());
        registerInvestmentRun(event);
    }

    private void registerInvestmentRun(final Event event) {
        this.lastInvestmentRunTimestamp = event.getCreatedOn();
    }
}
