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

import com.github.robozonky.api.strategies.ParticipationDescriptor;
import com.github.robozonky.api.strategies.PurchaseStrategy;
import com.github.robozonky.common.tenant.Tenant;
import jdk.jfr.Event;

class PurchasingOperationDescriptor implements OperationDescriptor<ParticipationDescriptor, PurchaseStrategy> {

    private static final long[] NO_LONGS = new long[0];
    private final AtomicReference<long[]> lastChecked = new AtomicReference<>(NO_LONGS);

    @Override
    public boolean isEnabled(final Tenant tenant) {
        return !tenant.getRestrictions().isCannotAccessSmp();
    }

    @Override
    public Optional<PurchaseStrategy> getStrategy(final Tenant tenant) {
        return tenant.getPurchaseStrategy();
    }

    @Override
    public MarketplaceAccessor<ParticipationDescriptor> newMarketplaceAccessor(final Tenant tenant) {
        return new SecondaryMarketplaceAccessor(tenant, lastChecked::getAndSet, this::identify);
    }

    @Override
    public BigDecimal getMinimumBalance(final Tenant tenant) {
        return BigDecimal.ONE;
    }

    @Override
    public long identify(final ParticipationDescriptor descriptor) {
        return descriptor.item().getId();
    }

    @Override
    public Operation<ParticipationDescriptor, PurchaseStrategy> getOperation() {
        return PurchasingSession::purchase;
    }

    @Override
    public Event newJfrEvent() {
        return new SecondaryMarketplaceJfrEvent();
    }
}
