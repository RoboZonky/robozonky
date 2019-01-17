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
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.function.ToLongFunction;
import java.util.stream.Collectors;

import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.strategies.InvestmentStrategy;
import com.github.robozonky.api.strategies.LoanDescriptor;
import com.github.robozonky.api.strategies.ParticipationDescriptor;
import com.github.robozonky.api.strategies.PurchaseStrategy;
import com.github.robozonky.app.tenant.PowerTenant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class StrategyExecutor<T, S> implements Supplier<Collection<Investment>> {

    private static final long[] NO_LONGS = new long[0];
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final PowerTenant tenant;
    private final AtomicReference<BigDecimal> balanceWhenLastChecked = new AtomicReference<>(BigDecimal.ZERO);
    private final AtomicReference<long[]> lastChecked = new AtomicReference<>(NO_LONGS);
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

    private static boolean contains(final long toFind, final long... original) {
        for (final long j : original) {
            if (j == toFind) {
                return true;
            }
        }
        return false;
    }

    static boolean hasAdditions(final long[] current, final long... original) {
        if (current.length == 0) {
            return false;
        } else if (current.length > original.length) {
            return true;
        }
        for (final long i : current) {
            final boolean found = contains(i, original);
            if (!found) {
                return true;
            }
        }
        return false;
    }

    private boolean skipStrategyEvaluation(final Collection<T> marketplace) {
        if (marketplace.isEmpty()) {
            logger.debug("Asleep as the marketplace is empty.");
            return true;
        }
        final BigDecimal currentBalance = tenant.getPortfolio().getBalance();
        final BigDecimal lastCheckedBalance = balanceWhenLastChecked.getAndSet(currentBalance);
        final boolean balanceChangedMeaningfully = isBiggerThan(currentBalance, lastCheckedBalance);
        if (balanceChangedMeaningfully) {
            logger.debug("Waking up due to a balance increase.");
            return false;
        } else if (hasMarketplaceUpdates(marketplace, operationDescriptor::identify)) {
            logger.debug("Waking up due to a change in marketplace.");
            return false;
        } else {
            logger.debug("Asleep as there was no change since last checked.");
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
        return hasAdditions(idsFromMarketplace, presentWhenLastChecked);
    }

    private Collection<Investment> invest(final S strategy) {
        final Collection<T> marketplace = operationDescriptor.readMarketplace(tenant).collect(Collectors.toList());
        if (skipStrategyEvaluation(marketplace)) {
            return Collections.emptyList();
        }
        logger.trace("Processing {} items from the marketplace.", marketplace.size());
        final Collection<Investment> result = operationDescriptor.getOperation().apply(tenant, marketplace, strategy);
        logger.trace("Marketplace processing complete.");
        return result;
    }

    @Override
    public Collection<Investment> get() {
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
