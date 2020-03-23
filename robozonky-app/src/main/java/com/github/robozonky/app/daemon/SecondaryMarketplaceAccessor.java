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

import java.time.Duration;
import java.util.Collection;
import java.util.Objects;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;

import com.github.robozonky.api.remote.entities.LastPublishedParticipation;
import com.github.robozonky.api.strategies.ParticipationDescriptor;
import com.github.robozonky.app.tenant.PowerTenant;
import com.github.robozonky.internal.remote.Select;
import com.github.robozonky.internal.remote.Zonky;

final class SecondaryMarketplaceAccessor extends AbstractMarketplaceAccessor<ParticipationDescriptor> {

    private static final Duration FULL_CHECK_INTERVAL = Duration.ofMinutes(5);
    private static final Logger LOGGER = Audit.purchasing();

    private final PowerTenant tenant;
    private final UnaryOperator<LastPublishedParticipation> stateAccessor;

    public SecondaryMarketplaceAccessor(final PowerTenant tenant,
            final UnaryOperator<LastPublishedParticipation> stateAccessor) {
        this.tenant = tenant;
        this.stateAccessor = stateAccessor;
    }

    @Override
    protected Select getBaseFilter() {
        return new Select()
            .equalsPlain("willNotExceedLoanInvestmentLimit", "true")
            .greaterThanOrEquals("remainingPrincipal", 2) // Ignore near-0 participation clutter.
            .lessThanOrEquals("remainingPrincipal", tenant.getKnownBalanceUpperBound()
                .getValue()
                .longValue());
    }

    @Override
    public Duration getForcedMarketplaceCheckInterval() {
        return FULL_CHECK_INTERVAL;
    }

    @Override
    public Collection<ParticipationDescriptor> getMarketplace() {
        var cache = SoldParticipationCache.forTenant(tenant);
        return tenant.call(zonky -> zonky.getAvailableParticipations(getIncrementalFilter()))
            .filter(p -> { // never re-purchase what was once sold
                final int loanId = p.getLoanId();
                if (cache.wasOnceSold(loanId)) {
                    LOGGER.debug("Loan #{} already sold before, ignoring.", loanId);
                    return false;
                } else {
                    return true;
                }
            })
            .map(p -> new ParticipationDescriptor(p, () -> tenant.getLoan(p.getLoanId())))
            .collect(Collectors.toList());
    }

    @Override
    public boolean hasUpdates() {
        try {
            final LastPublishedParticipation current = tenant.call(Zonky::getLastPublishedParticipationInfo);
            final LastPublishedParticipation previous = stateAccessor.apply(current);
            LOGGER.trace("Current is {}, previous is {}.", current, previous);
            return !Objects.equals(previous, current);
        } catch (final Exception ex) {
            LOGGER.debug("Zonky secondary marketplace status endpoint failed, forcing live marketplace check.", ex);
            return true;
        }
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }
}
