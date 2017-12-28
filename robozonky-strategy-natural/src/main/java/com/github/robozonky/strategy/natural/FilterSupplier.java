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

package com.github.robozonky.strategy.natural;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import com.github.robozonky.strategy.natural.conditions.LoanTermCondition;
import com.github.robozonky.strategy.natural.conditions.MarketplaceFilter;
import com.github.robozonky.strategy.natural.conditions.MarketplaceFilterCondition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The set of filters prescribed by the strategy changes based on whether or not the user has chosen to gradually exit
 * Zonky. If so, exit strategy is enabled and filters are altered in accordance.
 * <p>
 * The focus of this class is to always return the correct filters, no matter whether we are in a normal mode or in exit
 * strategy mode.
 */
public class FilterSupplier {

    private static final Logger LOGGER = LoggerFactory.getLogger(FilterSupplier.class);

    private final DefaultValues defaults;
    private final Supplier<Collection<MarketplaceFilter>> primarySupplier, secondarySupplier, sellSupplier;
    private final Lock lock = new ReentrantLock(true);
    private volatile Collection<MarketplaceFilter> primaryMarketplaceFilters, secondaryMarketplaceFilters, sellFilters;
    private volatile long lastCheckedMonthsBeforeExit = -1;
    private volatile boolean wasCheckedOnce = false, lastCheckedSellOffStarted = false;

    public FilterSupplier(final DefaultValues defaults, final Collection<MarketplaceFilter> primaryMarketplaceFilters,
                          final Collection<MarketplaceFilter> secondaryMarketplaceFilters,
                          final Collection<MarketplaceFilter> sellFilters) {
        this.defaults = defaults;
        this.primarySupplier = () -> primaryMarketplaceFilters;
        this.secondarySupplier = () -> secondaryMarketplaceFilters;
        this.sellSupplier = () -> sellFilters;
    }

    public FilterSupplier(final DefaultValues defaults, final Collection<MarketplaceFilter> primaryMarketplaceFilters,
                          final Collection<MarketplaceFilter> secondaryMarketplaceFilters) {
        this(defaults, primaryMarketplaceFilters, secondaryMarketplaceFilters, Collections.emptySet());
    }

    public FilterSupplier(final DefaultValues defaults, final Collection<MarketplaceFilter> primaryMarketplaceFilters) {
        this(defaults, primaryMarketplaceFilters, Collections.emptySet());
    }

    public FilterSupplier(final DefaultValues defaults) {
        this(defaults, Collections.emptySet());
    }

    private static Collection<MarketplaceFilter> getFilters(final Supplier<Collection<MarketplaceFilter>> unlessSelloff,
                                                            final boolean isSelloff) {
        if (isSelloff) { // everything must go
            return Collections.singleton(MarketplaceFilter.of(MarketplaceFilterCondition.alwaysAccepting()));
        } else {
            return unlessSelloff.get();
        }
    }

    private static Collection<MarketplaceFilter> supplyFilters(final Collection<MarketplaceFilter> filters,
                                                               final long monthsBeforeExit) {
        final Collection<MarketplaceFilter> result = new ArrayList<>(filters.size());
        if (monthsBeforeExit > -1) { // ignore marketplace items that go over the exit date
            final MarketplaceFilterCondition c = new LoanTermCondition(monthsBeforeExit + 1);
            final MarketplaceFilter f = MarketplaceFilter.of(c);
            result.add(f);
        }
        result.addAll(filters);
        return Collections.unmodifiableCollection(result);
    }

    private Collection<MarketplaceFilter> refreshPrimaryMarketplaceFilters() {
        return getFilters(() -> supplyFilters(primarySupplier.get(), defaults.getMonthsBeforeExit()),
                          defaults.isSelloffStarted());
    }

    private Collection<MarketplaceFilter> refreshSecondaryMarketplaceFilters() {
        return getFilters(() -> supplyFilters(secondarySupplier.get(), defaults.getMonthsBeforeExit()),
                          defaults.isSelloffStarted());
    }

    private Collection<MarketplaceFilter> refreshSellFilters() {
        return getFilters(() -> Collections.unmodifiableCollection(sellSupplier.get()), defaults.isSelloffStarted());
    }

    private void refreshLocked() {
        final boolean sellOffTriggered = defaults.isSelloffStarted() != lastCheckedSellOffStarted;
        final boolean exitStrategyTriggered = defaults.getMonthsBeforeExit() != lastCheckedMonthsBeforeExit;
        final boolean needsReinit = !wasCheckedOnce || sellOffTriggered || exitStrategyTriggered;
        if (!needsReinit) {
            return;
        }
        this.wasCheckedOnce = true;
        this.lastCheckedMonthsBeforeExit = defaults.getMonthsBeforeExit();
        this.lastCheckedSellOffStarted = defaults.isSelloffStarted();
        this.primaryMarketplaceFilters = refreshPrimaryMarketplaceFilters();
        this.secondaryMarketplaceFilters = refreshSecondaryMarketplaceFilters();
        this.sellFilters = refreshSellFilters();
        if (sellOffTriggered) {
            if (lastCheckedSellOffStarted) {
                LOGGER.info("Exit sell-off in effect. No new loans will be invested, full portfolio is up for sale.");
            } else if (lastCheckedMonthsBeforeExit > -1) {
                LOGGER.info("Exit sell-off no longer in effect, exit strategy still active.");
            } else {
                LOGGER.info("Returning from exit strategy, resuming normal operation.");
            }
        } else if (exitStrategyTriggered) { // no need to notify of this if already notified of the big sell-off
            if (lastCheckedMonthsBeforeExit > -1) {
                LOGGER.info("Exit strategy is active. New loans and participations over {} months will be ignored.",
                            lastCheckedMonthsBeforeExit);
            } else {
                LOGGER.info("Returning from exit strategy, resuming normal operation.");
            }
        }
    }

    private void refresh() {
        lock.lock();
        try { // only ever run the refresh once at a time
            refreshLocked();
        } finally {
            lock.unlock();
        }
    }

    public Collection<MarketplaceFilter> getPrimaryMarketplaceFilters() {
        refresh();
        return primaryMarketplaceFilters;
    }

    public Collection<MarketplaceFilter> getSecondaryMarketplaceFilters() {
        refresh();
        return secondaryMarketplaceFilters;
    }

    public Collection<MarketplaceFilter> getSellFilters() {
        refresh();
        return sellFilters;
    }

    @Override
    public String toString() {
        return "FilterSupplier{" +
                "primary=" + primaryMarketplaceFilters +
                ", secondary=" + secondaryMarketplaceFilters +
                ", sell=" + sellFilters +
                '}';
    }
}
