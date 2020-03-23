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

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.apache.logging.log4j.Logger;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.api.remote.entities.Participation;
import com.github.robozonky.api.strategies.InvestmentStrategy;
import com.github.robozonky.api.strategies.LoanDescriptor;
import com.github.robozonky.api.strategies.ParticipationDescriptor;
import com.github.robozonky.api.strategies.PurchaseStrategy;
import com.github.robozonky.app.tenant.PowerTenant;
import com.github.robozonky.internal.test.DateUtil;

class StrategyExecutor<T, S, R> implements Supplier<Collection<R>> {

    private final Logger logger;
    private final PowerTenant tenant;
    private final AtomicReference<Instant> lastSuccessfulMarketplaceCheck = new AtomicReference<>(Instant.EPOCH);
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
            .isBefore(DateUtil.now());
    }

    private Collection<R> invest(final S strategy) {
        if (skipStrategyEvaluation(marketplaceAccessor)) {
            return Collections.emptyList();
        }
        final Collection<T> marketplace = marketplaceAccessor.getMarketplace();
        if (marketplace.isEmpty()) {
            logger.debug("Marketplace is empty.");
            return Collections.emptyList();
        }
        logger.trace("Processing {} items from the marketplace.", marketplace.size());
        final Collection<R> result = operationDescriptor.getOperation()
            .apply(tenant, marketplace, strategy);
        lastSuccessfulMarketplaceCheck.set(DateUtil.now());
        logger.trace("Marketplace processing complete.");
        return result;
    }

    @Override
    public Collection<R> get() {
        if (!operationDescriptor.isEnabled(tenant)) {
            logger.debug("Access to marketplace disabled by Zonky.");
            return Collections.emptyList();
        }
        final Money currentBalance = tenant.getKnownBalanceUpperBound();
        final Money minimum = operationDescriptor.getMinimumBalance(tenant);
        if (currentBalance.compareTo(minimum) < 0) {
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
