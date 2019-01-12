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

package com.github.robozonky.app.daemon;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;
import java.util.stream.Stream;

import com.github.robozonky.api.remote.entities.Participation;
import com.github.robozonky.api.strategies.ParticipationDescriptor;
import com.github.robozonky.api.strategies.PurchaseStrategy;
import com.github.robozonky.app.tenant.SoldParticipationCache;
import com.github.robozonky.common.remote.Select;
import com.github.robozonky.common.tenant.Tenant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class PurchasingOperationDescriptor implements OperationDescriptor<ParticipationDescriptor, PurchaseStrategy> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PurchasingOperationDescriptor.class);

    private final Duration refreshInterval;

    public PurchasingOperationDescriptor(final Duration refreshInterval) {
        this.refreshInterval = refreshInterval;
    }

    PurchasingOperationDescriptor() {
        this(Duration.ZERO);
    }

    private static ParticipationDescriptor toDescriptor(final Participation p, final Tenant tenant) {
        return new ParticipationDescriptor(p, () -> tenant.getLoan(p.getLoanId()));
    }

    @Override
    public boolean isEnabled(final Tenant tenant) {
        return !tenant.getRestrictions().isCannotAccessSmp();
    }

    @Override
    public Optional<PurchaseStrategy> getStrategy(final Tenant tenant) {
        return tenant.getPurchaseStrategy();
    }

    @Override
    public Duration getRefreshInterval() {
        return refreshInterval;
    }

    @Override
    public Stream<ParticipationDescriptor> readMarketplace(final Tenant tenant) {
        final long balance = tenant.getPortfolio().getBalance().longValue();
        final Select s = new Select()
                .lessThanOrEquals("remainingPrincipal", balance)
                .equalsPlain("willNotExceedLoanInvestmentLimit", "true");
        final SoldParticipationCache cache = SoldParticipationCache.forTenant(tenant);
        return tenant.call(zonky -> zonky.getAvailableParticipations(s))
                .filter(p -> { // never re-purchase what was once sold
                    final int loanId = p.getLoanId();
                    final boolean wasSoldBefore = cache.wasOnceSold(loanId);
                    LOGGER.debug("Loan #{} already sold before, ignoring: {}.", loanId, wasSoldBefore);
                    return !wasSoldBefore;
                })
                .map(p -> toDescriptor(p, tenant));
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
}
