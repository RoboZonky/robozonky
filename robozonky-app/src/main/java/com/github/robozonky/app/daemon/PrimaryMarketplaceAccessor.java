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

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Objects;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import com.github.robozonky.api.remote.entities.LastPublishedLoan;
import com.github.robozonky.api.strategies.LoanDescriptor;
import com.github.robozonky.common.remote.Select;
import com.github.robozonky.common.remote.Zonky;
import com.github.robozonky.common.tenant.Tenant;
import com.github.robozonky.internal.util.DateUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

final class PrimaryMarketplaceAccessor implements MarketplaceAccessor<LoanDescriptor> {

    private static final Logger LOGGER = LogManager.getLogger();
    /**
     * Will make sure that the endpoint only loads loans that are on the marketplace, and not the entire history.
     */
    private static final Select SELECT = new Select().greaterThan("nonReservedRemainingInvestment", 0);
    private final Tenant tenant;
    private final UnaryOperator<LastPublishedLoan> stateAccessor;

    public PrimaryMarketplaceAccessor(final Tenant tenant, final UnaryOperator<LastPublishedLoan> stateAccessor) {
        this.tenant = tenant;
        this.stateAccessor = stateAccessor;
    }

    private static boolean isActionable(final LoanDescriptor loanDescriptor) {
        final OffsetDateTime now = DateUtil.offsetNow();
        return loanDescriptor.getLoanCaptchaProtectionEndDateTime()
                .map(d -> d.isBefore(now))
                .orElse(true);
    }

    @Override
    public Collection<LoanDescriptor> getMarketplace() {
        return tenant.call(zonky -> zonky.getAvailableLoans(SELECT))
                .parallel()
                .filter(l -> !l.getMyInvestment().isPresent()) // re-investing would fail
                .map(LoanDescriptor::new)
                .filter(PrimaryMarketplaceAccessor::isActionable)
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
            LOGGER.debug("Zonky marketplace status endpoint failed, forcing live marketplace check.", ex);
            return true;
        }
    }
}
