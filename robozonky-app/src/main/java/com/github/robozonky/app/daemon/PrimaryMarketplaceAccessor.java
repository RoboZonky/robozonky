/*
 * Copyright 2021 The RoboZonky Project
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
import java.util.Objects;
import java.util.OptionalInt;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import org.apache.logging.log4j.Logger;

import com.github.robozonky.api.remote.entities.LastPublishedItem;
import com.github.robozonky.api.strategies.LoanDescriptor;
import com.github.robozonky.internal.Settings;
import com.github.robozonky.internal.remote.Select;
import com.github.robozonky.internal.remote.Zonky;
import com.github.robozonky.internal.tenant.Tenant;

final class PrimaryMarketplaceAccessor extends AbstractMarketplaceAccessor<LoanDescriptor> {

    private static final Duration FULL_CHECK_INTERVAL = Duration.ofHours(1);
    private static final Logger LOGGER = Audit.investing();
    private final Tenant tenant;
    private final UnaryOperator<LastPublishedItem> stateAccessor;

    public PrimaryMarketplaceAccessor(final Tenant tenant, final UnaryOperator<LastPublishedItem> stateAccessor) {
        super(LOGGER);
        this.tenant = tenant;
        this.stateAccessor = stateAccessor;
    }

    @Override
    protected OptionalInt getMaximumItemsToRead() {
        var max = Settings.INSTANCE.getMaxItemsReadFromPrimaryMarketplace();
        return sanitizeMaximumItemCount(max);
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
    public Stream<LoanDescriptor> getMarketplace() {
        /*
         * Do not make this parallel.
         * Each loan will be processed individually and if done in parallel, the portfolio structure could go haywire.
         * The code is designed to invest, rebuild the portfolio structure, and then invest again.
         */
        var loans = tenant.call(zonky -> zonky.getAvailableLoans(getIncrementalFilter()))
            .peek(l -> {
                FirstNoticeTracker.executeAsync((r, now) -> r.register(now, l));
                ResponseTimeTracker.executeAsync((r, nanotime) -> r.registerLoan(nanotime, l.getId()));
            })
            .filter(l -> l.getMyInvestment()
                .isEmpty()); // re-investing would fail
        if (getMaximumItemsToRead().isPresent()) {
            var limit = getMaximumItemsToRead().orElseThrow();
            LOGGER.trace("Enforcing read limit of {} latest items.", limit);
            loans = loans.limit(limit);
        }
        return loans.map(LoanDescriptor::new);
    }

    @Override
    public boolean hasUpdates() {
        try {
            final LastPublishedItem current = tenant.call(Zonky::getLastPublishedLoanInfo);
            ResponseTimeTracker.executeAsync((r, nanotime) -> r.registerLoan(nanotime, current.getId()));
            final LastPublishedItem previous = stateAccessor.apply(current);
            LOGGER.trace("Current is {}, previous is {}.", current, previous);
            return !Objects.equals(previous, current);
        } catch (final Exception ex) {
            LOGGER.debug("Zonky primary marketplace status endpoint failed, forcing live marketplace check.", ex);
            return true;
        }
    }

}
