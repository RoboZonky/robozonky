/*
 * Copyright 2017 The RoboZonky Project
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

package com.github.robozonky.app.util;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.app.portfolio.Portfolio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class StrategyExecutor<T, S> implements BiFunction<Portfolio, Collection<T>, Collection<Investment>> {

    protected final Logger LOGGER = LoggerFactory.getLogger(this.getClass());
    private final Supplier<Optional<S>> strategyProvider;
    private final AtomicBoolean marketplaceCheckPending = new AtomicBoolean(false);
    private final AtomicReference<BigDecimal> balanceWhenLastChecked = new AtomicReference<>(BigDecimal.ZERO);

    protected StrategyExecutor(final Supplier<Optional<S>> strategy) {
        this.strategyProvider = strategy;
    }

    protected static boolean hasNewIds(final int[] original, final int... current) {
        return IntStream.of(current).anyMatch(i -> IntStream.of(original).noneMatch(j -> i == j));
    }

    protected boolean isBalanceUnderMinimum(final int currentBalance) {
        return false;
    }

    /**
     * In order to not have to run the strategy over a marketplace and save CPU cycles, we need to know if the
     * marketplace changed since the last time this method was called.
     * @param marketplace Present contents of the marketplace.
     * @return Returning true triggers evaluation of the strategy.
     */
    protected abstract boolean hasMarketplaceUpdates(final Collection<T> marketplace);

    private boolean skipStrategyEvaluation(final Portfolio portfolio, final Collection<T> marketplace) {
        final BigDecimal currentBalance = portfolio.getRemoteBalance().get();
        if (isBalanceUnderMinimum(currentBalance.intValue())) {
            LOGGER.debug("Asleep due to balance being less than minimum.");
            return true;
        } else if (marketplaceCheckPending.get()) {
            return false;
        }
        final BigDecimal lastCheckedBalance = balanceWhenLastChecked.getAndSet(currentBalance);
        final boolean balanceChangedMeaningfully = currentBalance.compareTo(lastCheckedBalance) > 0;
        if (balanceChangedMeaningfully) {
            LOGGER.debug("Waking up due to a balance increase.");
            return false;
        } else if (hasMarketplaceUpdates(marketplace)) {
            LOGGER.debug("Waking up due to a change in marketplace.");
            return false;
        } else {
            LOGGER.debug("Asleep as there was no change since last checked.");
            return true;
        }
    }

    /**
     * Execute the investment operations.
     * @param portfolio Portfolio to use.
     * @param strategy Strategy used to determine which items to take.
     * @param marketplace Items available for the taking.
     * @return Items taken by the investment algorithm, having matched the strategy.
     */
    protected abstract Collection<Investment> execute(final Portfolio portfolio, final S strategy,
                                                      final Collection<T> marketplace);

    private Collection<Investment> invest(final Portfolio portfolio, final S strategy,
                                          final Collection<T> marketplace) {
        if (skipStrategyEvaluation(portfolio, marketplace)) {
            return Collections.emptyList();
        }
        /*
         * if the strategy evaluation fails with an exception, store that so that the next time - even if shouldSleep()
         * says to sleep - we will check the marketplace.
         */
        marketplaceCheckPending.set(true);
        LOGGER.trace("Check flag set.");
        final Collection<Investment> result = execute(portfolio, strategy, marketplace);
        marketplaceCheckPending.set(false);
        LOGGER.trace("Check flag unset.");
        return result;
    }

    @Override
    public Collection<Investment> apply(final Portfolio portfolio, final Collection<T> marketplace) {
        LOGGER.debug("Received {} items from the marketplace.", marketplace.size());
        return strategyProvider.get()
                .map(strategy -> invest(portfolio, strategy, marketplace))
                .orElseGet(() -> {
                    LOGGER.info("Marketplace is disabled by the strategy.");
                    return Collections.emptyList();
                });
    }
}
