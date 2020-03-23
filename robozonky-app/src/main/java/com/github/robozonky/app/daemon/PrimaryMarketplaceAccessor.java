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

import com.github.robozonky.api.remote.entities.LastPublishedLoan;
import com.github.robozonky.api.strategies.LoanDescriptor;
import com.github.robozonky.internal.remote.Select;
import com.github.robozonky.internal.remote.Zonky;
import com.github.robozonky.internal.tenant.Tenant;

final class PrimaryMarketplaceAccessor extends AbstractMarketplaceAccessor<LoanDescriptor> {

    private static final Duration FULL_CHECK_INTERVAL = Duration.ofMinutes(1);
    private static final Logger LOGGER = Audit.investing();
    private final Tenant tenant;
    private final UnaryOperator<LastPublishedLoan> stateAccessor;

    public PrimaryMarketplaceAccessor(final Tenant tenant, final UnaryOperator<LastPublishedLoan> stateAccessor) {
        this.tenant = tenant;
        this.stateAccessor = stateAccessor;
    }

    @Override
    protected Select getBaseFilter() {
        // Will make sure that the endpoint only loads loans that are on the marketplace, and not the entire history.
        return new Select()
            .greaterThan("nonReservedRemainingInvestment", 0);
    }

    @Override
    public Duration getForcedMarketplaceCheckInterval() {
        return FULL_CHECK_INTERVAL;
    }

    @Override
    public Collection<LoanDescriptor> getMarketplace() {
        return tenant.call(zonky -> zonky.getAvailableLoans(getIncrementalFilter()))
            .parallel()
            .filter(l -> l.getMyInvestment()
                .isEmpty()) // re-investing would fail
            .map(LoanDescriptor::new)
            .collect(Collectors.toList());
    }

    @Override
    public boolean hasUpdates() {
        try {
            final LastPublishedLoan current = tenant.call(Zonky::getLastPublishedLoanInfo);
            final LastPublishedLoan previous = stateAccessor.apply(current);
            LOGGER.trace("Current is {}, previous is {}.", current, previous);
            return !Objects.equals(previous, current);
        } catch (final Exception ex) {
            LOGGER.debug("Zonky primary marketplace status endpoint failed, forcing live marketplace check.", ex);
            return true;
        }
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }
}
