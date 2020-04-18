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

package com.github.robozonky.app.daemon;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.logging.log4j.Logger;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.remote.entities.LastPublishedParticipation;
import com.github.robozonky.api.remote.entities.Participation;
import com.github.robozonky.api.strategies.ParticipationDescriptor;
import com.github.robozonky.api.strategies.PurchaseStrategy;
import com.github.robozonky.app.tenant.PowerTenant;
import com.github.robozonky.internal.tenant.Tenant;

class PurchasingOperationDescriptor
        implements OperationDescriptor<ParticipationDescriptor, PurchaseStrategy, Participation> {

    private final AtomicReference<LastPublishedParticipation> lastChecked = new AtomicReference<>();

    @Override
    public boolean isEnabled(final Tenant tenant) {
        return tenant.getSessionInfo()
            .canAccessSmp();
    }

    @Override
    public Optional<PurchaseStrategy> getStrategy(final Tenant tenant) {
        return tenant.getPurchaseStrategy();
    }

    @Override
    public AbstractMarketplaceAccessor<ParticipationDescriptor> newMarketplaceAccessor(final PowerTenant tenant) {
        return new SecondaryMarketplaceAccessor(tenant, lastChecked::getAndSet);
    }

    @Override
    public long identify(final ParticipationDescriptor descriptor) {
        return descriptor.item()
            .getId();
    }

    @Override
    public Operation<ParticipationDescriptor, PurchaseStrategy, Participation> getOperation() {
        return PurchasingSession::purchase;
    }

    @Override
    public Money getMinimumBalance(final PowerTenant tenant) {
        return Money.from(1);
    }

    @Override
    public Logger getLogger() {
        return Audit.purchasing();
    }
}
