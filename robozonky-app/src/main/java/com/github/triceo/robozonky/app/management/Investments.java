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

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import com.github.triceo.robozonky.api.notifications.ExecutionStartedEvent;
import com.github.triceo.robozonky.api.notifications.InvestmentDelegatedEvent;
import com.github.triceo.robozonky.api.notifications.InvestmentMadeEvent;
import com.github.triceo.robozonky.api.notifications.InvestmentRejectedEvent;
import com.github.triceo.robozonky.internal.api.Defaults;

class Investments implements InvestmentsMBean {

    private OffsetDateTime lastInvestmentRunTimestamp;
    private final Map<Integer, Integer> successfulInvestments = new LinkedHashMap<>(),
            delegatedInvestments = new LinkedHashMap<>(), rejectedInvestments = new LinkedHashMap<>();

    public Investments() {
        this.clear();
    }

    @Override
    public Map<Integer, Integer> getSuccessfulInvestments() {
        return this.successfulInvestments;
    }

    void addSuccessfulInvestment(final InvestmentMadeEvent event) {
        this.successfulInvestments.put(event.getInvestment().getLoanId(), event.getInvestment().getAmount());
    }

    @Override
    public Map<Integer, Integer> getDelegatedInvestments() {
        return this.delegatedInvestments;
    }

    void addDelegatedInvestment(final InvestmentDelegatedEvent event) {
        this.delegatedInvestments.put(
                event.getRecommendation().getLoanDescriptor().getLoan().getId(),
                event.getRecommendation().getRecommendedInvestmentAmount());
    }

    @Override
    public Map<Integer, Integer> getRejectedInvestments() {
        return this.rejectedInvestments;
    }

    void addRejectedInvestment(final InvestmentRejectedEvent event) {
        this.rejectedInvestments.put(
                event.getRecommendation().getLoanDescriptor().getLoan().getId(),
                event.getRecommendation().getRecommendedInvestmentAmount());
    }

    @Override
    public OffsetDateTime getLastInvestmentRunTimestamp() {
        return this.lastInvestmentRunTimestamp;
    }

    void registerInvestmentRun(final ExecutionStartedEvent event) {
        this.lastInvestmentRunTimestamp = event.getCreatedOn();
    }

    @Override
    public void clear() {
        this.rejectedInvestments.clear();
        this.delegatedInvestments.clear();
        this.successfulInvestments.clear();
        this.lastInvestmentRunTimestamp = OffsetDateTime.ofInstant(Instant.EPOCH, Defaults.ZONE_ID);
    }
}
