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

import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.strategies.InvestmentStrategy;
import com.github.robozonky.api.strategies.LoanDescriptor;
import com.github.robozonky.api.strategies.ParticipationDescriptor;
import com.github.robozonky.api.strategies.PurchaseStrategy;
import com.github.robozonky.app.tenant.PowerTenant;
import com.github.robozonky.internal.test.DateUtil;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

class StrategyExecutor<T, S> implements Supplier<Collection<Investment>> {

    private static final Duration FORCED_MARKETPLACE_CHECK_PERIOD = Duration.ofSeconds(30);
    private final Logger logger;
    private final PowerTenant tenant;
    private final AtomicReference<Instant> lastSuccessfulMarketplaceCheck = new AtomicReference<>(Instant.EPOCH);
    private final OperationDescriptor<T, S> operationDescriptor;

    StrategyExecutor(final PowerTenant tenant, final OperationDescriptor<T, S> operationDescriptor) {
        this.tenant = tenant;
        this.operationDescriptor = operationDescriptor;
        this.logger = operationDescriptor.getLogger();
    }

    public static StrategyExecutor<LoanDescriptor, InvestmentStrategy> forInvesting(final PowerTenant tenant) {
        return new StrategyExecutor<>(tenant, new InvestingOperationDescriptor());
    }

    public static StrategyExecutor<ParticipationDescriptor, PurchaseStrategy> forPurchasing(final PowerTenant tenant) {
        return new StrategyExecutor<>(tenant, new PurchasingOperationDescriptor());
    }

    private boolean skipStrategyEvaluation(final MarketplaceAccessor<T> marketplace) {
        if (needsToForceMarketplaceCheck()) {
            logger.debug("Forcing a marketplace check.");
            return false;
        } else if (marketplace.hasUpdates()) {
            logger.debug("Waking up due to a change in marketplace.");
            return false;
        } else {
            logger.debug("Asleep as there was no change since last checked.");
            return true;
        }
    }

    /**
     * Marketplace check needs to be forced in the following situations:
     *
     * <ul>
     *     <li>If we are in a forced pause due to some previous remote server error, we need to make sure we've tried
     *     as many remote operations before resuming from such forced pause. Forcing a marketplace chech will maximize
     *     the chances of uncovering any persistent JSON parsing issues etc.</li>
     *     <li>If it's been a while since the full marketplace was last checked.</li>
     * </ul>
     * @return True if the check is to be forced.
     */
    private boolean needsToForceMarketplaceCheck() {
        return !tenant.getAvailability().isAvailable() || lastSuccessfulMarketplaceCheck.get()
                .plus(FORCED_MARKETPLACE_CHECK_PERIOD)
                .isBefore(DateUtil.now());
    }

    private Collection<Investment> invest(final S strategy) {
        final MarketplaceAccessor<T> marketplaceAccessor = operationDescriptor.newMarketplaceAccessor(tenant);
        if (skipStrategyEvaluation(marketplaceAccessor)) {
            return Collections.emptyList();
        }
        final Collection<T> marketplace = marketplaceAccessor.getMarketplace();
        if (!needsToForceMarketplaceCheck() && marketplace.isEmpty()) {
            logger.debug("Marketplace is empty.");
            return Collections.emptyList();
        }
        logger.trace("Processing {} items from the marketplace.", marketplace.size());
        final Collection<Investment> result = operationDescriptor.getOperation().apply(tenant, marketplace, strategy);
        lastSuccessfulMarketplaceCheck.set(DateUtil.now());
        logger.trace("Marketplace processing complete.");
        return result;
    }

    @Override
    public Collection<Investment> get() {
        if (!operationDescriptor.isEnabled(tenant)) {
            logger.debug("Access to marketplace disabled by Zonky.");
            return Collections.emptyList();
        }
        final long currentBalance = tenant.getKnownBalanceUpperBound();
        final long minimum = operationDescriptor.getMinimumBalance(tenant);
        if (!needsToForceMarketplaceCheck() && currentBalance < minimum) {
            logger.debug("Asleep due to balance estimated below minimum. ({} < {})", currentBalance, minimum);
            return Collections.emptyList();
        }
        return operationDescriptor.getStrategy(tenant)
                .map(this::invest)
                .orElseGet(() -> {
                    logger.debug("Asleep as there is no strategy.");
                    return Collections.emptyList();
                });
    }
}
