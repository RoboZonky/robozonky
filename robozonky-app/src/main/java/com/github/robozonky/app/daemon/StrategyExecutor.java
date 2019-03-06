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
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.strategies.InvestmentStrategy;
import com.github.robozonky.api.strategies.LoanDescriptor;
import com.github.robozonky.api.strategies.ParticipationDescriptor;
import com.github.robozonky.api.strategies.PurchaseStrategy;
import com.github.robozonky.app.tenant.PowerTenant;
import com.github.robozonky.internal.util.DateUtil;
import jdk.jfr.Event;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

class StrategyExecutor<T, S> implements Supplier<Collection<Investment>> {

    private static final Duration FORCED_MARKETPLACE_CHECK_PERIOD = Duration.ofSeconds(15);
    private final Logger logger = LogManager.getLogger(getClass());
    private final PowerTenant tenant;
    private final AtomicReference<BigDecimal> balanceWhenLastChecked = new AtomicReference<>(BigDecimal.ZERO);
    private final AtomicReference<Instant> lastSuccessfulMarketplaceCheck = new AtomicReference<>(Instant.EPOCH);
    private final OperationDescriptor<T, S> operationDescriptor;

    StrategyExecutor(final PowerTenant tenant, final OperationDescriptor<T, S> operationDescriptor) {
        this.tenant = tenant;
        this.operationDescriptor = operationDescriptor;
    }

    public static StrategyExecutor<LoanDescriptor, InvestmentStrategy> forInvesting(final PowerTenant tenant,
                                                                                    final Investor investor) {
        return new Investing(tenant, investor);
    }

    public static StrategyExecutor<ParticipationDescriptor, PurchaseStrategy> forPurchasing(final PowerTenant tenant) {
        return new Purchasing(tenant);
    }

    private static boolean isBiggerThan(final BigDecimal left, final BigDecimal right) {
        return left.compareTo(right) > 0;
    }

    private boolean skipStrategyEvaluation(final MarketplaceAccessor<T> marketplace) {
        final BigDecimal currentBalance = tenant.getPortfolio().getBalance();
        final BigDecimal lastCheckedBalance = balanceWhenLastChecked.getAndSet(currentBalance);
        final boolean balanceChangedMeaningfully = isBiggerThan(currentBalance, lastCheckedBalance);
        if (balanceChangedMeaningfully) {
            logger.debug("Waking up due to a balance increase.");
            return false;
        } else if (marketplace.hasUpdates()) {
            logger.debug("Waking up due to a change in marketplace.");
            return false;
        } else if (needsToForceMarketplaceCheck()) {
            logger.debug("Forcing a periodic live marketplace check.");
            return false;
        } else {
            logger.debug("Asleep as there was no change since last checked.");
            return true;
        }
    }

    private boolean needsToForceMarketplaceCheck() {
        return lastSuccessfulMarketplaceCheck.get()
                .plus(FORCED_MARKETPLACE_CHECK_PERIOD)
                .isBefore(DateUtil.now());
    }

    private Collection<Investment> invest(final S strategy) {
        final MarketplaceAccessor<T> marketplaceAccessor = operationDescriptor.newMarketplaceAccessor(tenant);
        if (skipStrategyEvaluation(marketplaceAccessor)) {
            return Collections.emptyList();
        }
        final Collection<T> marketplace = marketplaceAccessor.getMarketplace();
        if (marketplace.isEmpty()) {
            logger.debug("Marketplace is empty.");
            return Collections.emptyList();
        }
        logger.trace("Processing {} items from the marketplace.", marketplace.size());
        final Collection<Investment> result = operationDescriptor.getOperation().apply(tenant, marketplace, strategy);
        lastSuccessfulMarketplaceCheck.set(DateUtil.now());
        logger.trace("Marketplace processing complete.");
        return result;
    }

    private Collection<Investment> actuallyGet() {
        if (!operationDescriptor.isEnabled(tenant)) {
            logger.debug("Access to marketplace disabled by Zonky.");
            return Collections.emptyList();
        }
        final BigDecimal currentBalance = tenant.getPortfolio().getBalance();
        final BigDecimal minimum = operationDescriptor.getMinimumBalance(tenant);
        if (isBiggerThan(minimum, currentBalance)) {
            logger.debug("Asleep due to balance being at or below than minimum. ({} <= {})", currentBalance, minimum);
            return Collections.emptyList();
        }
        return operationDescriptor.getStrategy(tenant)
                .map(this::invest)
                .orElseGet(() -> {
                    logger.debug("Asleep as there is no strategy.");
                    return Collections.emptyList();
                });
    }

    @Override
    public Collection<Investment> get() {
        final Event event = operationDescriptor.newJfrEvent();
        try {
            event.begin();
            return actuallyGet();
        } finally {
            event.commit();
        }
    }

    /**
     * The reason for this class' existence is so that the logger in the superclass indicates the type of strategy
     * being executed.
     */
    private static final class Investing extends StrategyExecutor<LoanDescriptor, InvestmentStrategy> {

        public Investing(final PowerTenant tenant, final Investor investor) {
            super(tenant, new InvestingOperationDescriptor(investor));
        }
    }

    /**
     * The reason for this class' existence is so that the logger in the superclass indicates the type of strategy
     * being executed.
     */
    private static final class Purchasing extends StrategyExecutor<ParticipationDescriptor, PurchaseStrategy> {

        public Purchasing(final PowerTenant tenant) {
            super(tenant, new PurchasingOperationDescriptor());
        }
    }
}
