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

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.apache.logging.log4j.Logger;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.api.remote.entities.Participation;
import com.github.robozonky.api.strategies.InvestmentStrategy;
import com.github.robozonky.api.strategies.LoanDescriptor;
import com.github.robozonky.api.strategies.ParticipationDescriptor;
import com.github.robozonky.api.strategies.PurchaseStrategy;
import com.github.robozonky.app.tenant.PowerTenant;
import com.github.robozonky.internal.Defaults;
import com.github.robozonky.internal.test.DateUtil;

class StrategyExecutor<T, S, R> implements Supplier<Stream<R>> {

    private final Logger logger;
    private final PowerTenant tenant;
    private final AtomicReference<ZonedDateTime> lastSuccessfulMarketplaceCheck = new AtomicReference<>(
            Instant.EPOCH.atZone(Defaults.ZONKYCZ_ZONE_ID));
    private final OperationDescriptor<T, S, R> operationDescriptor;
    private final AbstractMarketplaceAccessor<T> marketplaceAccessor;

    StrategyExecutor(final PowerTenant tenant, final OperationDescriptor<T, S, R> operationDescriptor) {
        this.tenant = tenant;
        this.operationDescriptor = operationDescriptor;
        this.marketplaceAccessor = operationDescriptor.newMarketplaceAccessor(tenant);
        this.logger = operationDescriptor.getLogger();
    }

    public static StrategyExecutor<LoanDescriptor, InvestmentStrategy, Loan> forInvesting(final PowerTenant tenant) {
        return new StrategyExecutor<>(tenant, new InvestingOperationDescriptor());
    }

    public static StrategyExecutor<ParticipationDescriptor, PurchaseStrategy, Participation> forPurchasing(
            final PowerTenant tenant) {
        return new StrategyExecutor<>(tenant, new PurchasingOperationDescriptor());
    }

    private boolean skipStrategyEvaluation(final AbstractMarketplaceAccessor<T> marketplace) {
        if (!tenant.getAvailability()
            .isAvailable()) {
            /*
             * If we are in a forced pause due to some remote server error, we need to make sure we've tried as many
             * remote operations before resuming from such forced pause. In such cases, we will force a marketplace
             * check, which would likely uncover any persistent JSON parsing issues etc.
             */
            logger.debug("Forcing marketplace check to see if we can resume from forced pause.");
            return false;
        } else if (marketplace.getMaximumItemsToRead()
            .orElse(Integer.MAX_VALUE) < 1) {
            logger.debug("Asleep due to settings to read 0 items from the marketplace.");
            return true;
        } else if (marketplace.hasUpdates()) {
            logger.debug("Waking up due to a change in marketplace.");
            return false;
        } else if (needsToForceMarketplaceCheck(marketplace)) {
            logger.debug("Forcing a periodic marketplace check.");
            return false;
        } else {
            logger.debug("Asleep as there was no change since last checked.");
            return true;
        }
    }

    private boolean needsToForceMarketplaceCheck(final AbstractMarketplaceAccessor<T> marketplace) {
        return lastSuccessfulMarketplaceCheck.get()
            .plus(marketplace.getForcedMarketplaceCheckInterval())
            .isBefore(DateUtil.zonedNow());
    }

    private Stream<R> invest(final S strategy) {
        if (skipStrategyEvaluation(marketplaceAccessor)) {
            return Stream.empty();
        }
        var marketplaceCheckTimestamp = DateUtil.zonedNow();
        try {
            var marketplace = marketplaceAccessor.getMarketplace();
            var result = operationDescriptor.getOperation()
                .apply(tenant, marketplace, strategy);
            lastSuccessfulMarketplaceCheck.set(marketplaceCheckTimestamp);
            logger.trace("Marketplace processing complete.");
            return result;
        } finally {
            ResponseTimeTracker.executeAsync((r, nanotime) -> r.clear());
        }
    }

    @Override
    public Stream<R> get() {
        if (!operationDescriptor.isEnabled(tenant)) {
            logger.debug("Access to marketplace disabled by Zonky.");
            return Stream.empty();
        }
        final Money currentBalance = tenant.getKnownBalanceUpperBound();
        final Money minimum = operationDescriptor.getMinimumBalance(tenant);
        if (currentBalance.compareTo(minimum) < 0) {
            logger.debug("Asleep due to balance estimated below minimum. ({} < {})", currentBalance, minimum);
            return Stream.empty();
        }
        return operationDescriptor.getStrategy(tenant)
            .map(this::invest)
            .orElseGet(() -> {
                logger.debug("Asleep as there is no strategy.");
                return Stream.empty();
            });
    }
}
