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

package com.github.robozonky.app.daemon;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import com.github.robozonky.api.remote.entities.LastPublishedLoan;
import com.github.robozonky.api.strategies.InvestmentStrategy;
import com.github.robozonky.api.strategies.LoanDescriptor;
import com.github.robozonky.common.tenant.Tenant;
import jdk.jfr.Event;

class InvestingOperationDescriptor implements OperationDescriptor<LoanDescriptor, InvestmentStrategy> {

    private final Investor investor;
    private final AtomicReference<LastPublishedLoan> lastChecked = new AtomicReference<>(null);

    public InvestingOperationDescriptor(final Investor investor) {
        this.investor = investor;
    }

    public InvestingOperationDescriptor() {
        this(null);
    }

    @Override
    public boolean isEnabled(final Tenant tenant) {
        return !tenant.getRestrictions().isCannotInvest();
    }

    @Override
    public Optional<InvestmentStrategy> getStrategy(final Tenant tenant) {
        return tenant.getInvestmentStrategy();
    }

    @Override
    public MarketplaceAccessor<LoanDescriptor> newMarketplaceAccessor(final Tenant tenant) {
        return new PrimaryMarketplaceAccessor(tenant, lastChecked::getAndSet);
    }

    @Override
    public BigDecimal getMinimumBalance(final Tenant tenant) {
        return BigDecimal.valueOf(tenant.getRestrictions().getMinimumInvestmentAmount());
    }

    @Override
    public long identify(final LoanDescriptor descriptor) {
        return descriptor.item().getId();
    }

    @Override
    public Operation<LoanDescriptor, InvestmentStrategy> getOperation() {
        return (a, b, c) -> InvestingSession.invest(investor, a, b, c);
    }

    @Override
    public Event newJfrEvent() {
        return new PrimaryMarketplaceJfrEvent();
    }
}
