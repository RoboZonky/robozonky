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
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.function.ToLongFunction;

import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.app.tenant.PowerTenant;
import com.github.robozonky.util.NumberUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class StrategyExecutor<T, S> implements Supplier<Collection<Investment>> {

    private static final long[] NO_LONGS = new long[0];
    private static final Logger LOGGER = LoggerFactory.getLogger(StrategyExecutor.class);
    private final PowerTenant tenant;
    private final AtomicBoolean marketplaceCheckPending = new AtomicBoolean(false);
    private final AtomicReference<BigDecimal> balanceWhenLastChecked = new AtomicReference<>(BigDecimal.ZERO);
    private final AtomicReference<long[]> lastChecked = new AtomicReference<>(NO_LONGS);
    private final OperationDescriptor<T, S> operationDescriptor;

    public StrategyExecutor(final PowerTenant tenant, final OperationDescriptor<T, S> operationDescriptor) {
        this.tenant = tenant;
        this.operationDescriptor = operationDescriptor;
    }

    protected PowerTenant getTenant() {
        return tenant;
    }

    private boolean skipStrategyEvaluation(final Collection<T> marketplace) {
        if (marketplace.isEmpty()) {
            LOGGER.debug("Asleep as the marketplace is empty.");
            return true;
        } else if (marketplaceCheckPending.get()) {
            LOGGER.debug("Waking up to finish a pending marketplace check.");
            return false;
        }
        final BigDecimal currentBalance = tenant.getPortfolio().getBalance();
        final BigDecimal lastCheckedBalance = balanceWhenLastChecked.getAndSet(currentBalance);
        final boolean balanceChangedMeaningfully = currentBalance.compareTo(lastCheckedBalance) > 0;
        if (balanceChangedMeaningfully) {
            LOGGER.debug("Waking up due to a balance increase.");
            return false;
        } else if (hasMarketplaceUpdates(marketplace, operationDescriptor::identify)) {
            LOGGER.debug("Waking up due to a change in marketplace.");
            return false;
        } else {
            LOGGER.debug("Asleep as there was no change since last checked.");
            return true;
        }
    }

    /**
     * In order to not have to run the strategy over a marketplace and save CPU cycles, we need to know if the
     * marketplace changed since the last time this method was called.
     * @param marketplace Present contents of the marketplace.
     * @return Returning true triggers evaluation of the strategy.
     */
    private boolean hasMarketplaceUpdates(final Collection<T> marketplace, final ToLongFunction<T> idSupplier) {
        final long[] idsFromMarketplace = marketplace.stream().mapToLong(idSupplier).toArray();
        final long[] presentWhenLastChecked = lastChecked.getAndSet(idsFromMarketplace);
        return NumberUtil.hasAdditions(presentWhenLastChecked, idsFromMarketplace);
    }

    private Collection<Investment> invest(final S strategy, final Collection<T> marketplace) {
        if (skipStrategyEvaluation(marketplace)) {
            return Collections.emptyList();
        }
        LOGGER.trace("Processing {} items from the marketplace.", marketplace.size());
        /*
         * if the strategy evaluation fails with an exception, store that so that the next time - even if shouldSleep()
         * says to sleep - we will check the marketplace.
         */
        marketplaceCheckPending.set(true);
        final Collection<Investment> result = operationDescriptor.getOperation().apply(tenant, marketplace, strategy);
        marketplaceCheckPending.set(false);
        LOGGER.trace("Marketplace processing complete.");
        return result;
    }

    public Duration getRefreshInterval() {
        return operationDescriptor.getRefreshInterval();
    }

    @Override
    public Collection<Investment> get() {
        if (!operationDescriptor.isEnabled(tenant)) {
            LOGGER.debug("Access to marketplace disabled by Zonky.");
            return Collections.emptyList();
        }
        final BigDecimal currentBalance = tenant.getPortfolio().getBalance();
        final BigDecimal minimum = operationDescriptor.getMinimumBalance(tenant);
        if (minimum.compareTo(currentBalance) > 0) {
            LOGGER.debug("Asleep due to balance being less than minimum. ({} < {})", currentBalance, minimum);
            return Collections.emptyList();
        }
        return operationDescriptor.getStrategy(tenant)
                .map(strategy -> invest(strategy, operationDescriptor.readMarketplace(tenant)))
                .orElse(Collections.emptyList());
    }
}
