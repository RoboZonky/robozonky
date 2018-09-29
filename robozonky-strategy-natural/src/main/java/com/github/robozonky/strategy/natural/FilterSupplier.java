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

package com.github.robozonky.strategy.natural;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
class FilterSupplier {

    private static final Logger LOGGER = LoggerFactory.getLogger(FilterSupplier.class);

    private final DefaultValues defaults;
    private final Supplier<Collection<MarketplaceFilter>> primarySupplier, secondarySupplier, sellSupplier;
    private final boolean primaryMarketplaceEnabled, secondaryMarketplaceEnabled;
    private volatile Collection<MarketplaceFilter> primaryMarketplaceFilters, secondaryMarketplaceFilters, sellFilters;
    private volatile long lastCheckedMonthsBeforeExit = -1;
    private volatile boolean wasCheckedOnce = false, lastCheckedSellOffStarted = false;

    /**
     * @param defaults Never null.
     * @param primaryMarketplaceFilters If null, {@link #isPrimaryMarketplaceEnabled()} will return false.
     * @param secondaryMarketplaceFilters If null, {@link #isSecondaryMarketplaceEnabled()} will return false.
     * @param sellFilters Never null.
     */
    public FilterSupplier(final DefaultValues defaults, final Collection<MarketplaceFilter> primaryMarketplaceFilters,
                          final Collection<MarketplaceFilter> secondaryMarketplaceFilters,
                          final Collection<MarketplaceFilter> sellFilters) {
        this.defaults = defaults;
        this.primaryMarketplaceEnabled = primaryMarketplaceFilters != null;
        this.secondaryMarketplaceEnabled = secondaryMarketplaceFilters != null;
        this.primarySupplier = () -> primaryMarketplaceFilters;
        this.secondarySupplier = () -> secondaryMarketplaceFilters;
        this.sellSupplier = () -> sellFilters;
        refresh();
    }

    FilterSupplier(final DefaultValues defaults, final Collection<MarketplaceFilter> primaryMarketplaceFilters,
                   final Collection<MarketplaceFilter> secondaryMarketplaceFilters) {
        this(defaults, primaryMarketplaceFilters, secondaryMarketplaceFilters, Collections.emptySet());
    }

    public FilterSupplier(final DefaultValues defaults, final Collection<MarketplaceFilter> primaryMarketplaceFilters) {
        this(defaults, primaryMarketplaceFilters, null);
    }

    FilterSupplier(final DefaultValues defaults) {
        this(defaults, null);
    }

    private static Collection<MarketplaceFilter> getFilters(final Supplier<Collection<MarketplaceFilter>> unlessSelloff,
                                                            final boolean isSelloff) {
        if (isSelloff) { // accept every sale, reject every investment and participation
            return Collections.singleton(MarketplaceFilter.of(MarketplaceFilterCondition.alwaysAccepting()));
        } else {
            return unlessSelloff.get();
        }
    }

    private static Collection<MarketplaceFilter> supplyFilters(final Collection<MarketplaceFilter> filters,
                                                               final long monthsBeforeExit) {
        if (monthsBeforeExit > -1) { // ignore marketplace items that go over the exit date
            final long filteredTerms = Math.min(monthsBeforeExit + 1, 84); // fix extreme exit dates
            final MarketplaceFilterCondition c = new LoanTermCondition(filteredTerms);
            final MarketplaceFilter f = MarketplaceFilter.of(c);
            final Collection<MarketplaceFilter> result = new ArrayList<>(filters.size());
            result.add(f);
            result.addAll(filters);
            return Collections.unmodifiableCollection(result);
        } else {
            return Collections.unmodifiableCollection(filters);
        }
    }

    public boolean isPrimaryMarketplaceEnabled() {
        return primaryMarketplaceEnabled;
    }

    public boolean isSecondaryMarketplaceEnabled() {
        return secondaryMarketplaceEnabled;
    }

    private Collection<MarketplaceFilter> refreshPrimaryMarketplaceFilters() {
        if (isPrimaryMarketplaceEnabled()) {
            return getFilters(() -> supplyFilters(primarySupplier.get(), defaults.getMonthsBeforeExit()),
                              defaults.isSelloffStarted());
        } else {
            return Collections.emptyList();
        }
    }

    private Collection<MarketplaceFilter> refreshSecondaryMarketplaceFilters() {
        if (isSecondaryMarketplaceEnabled()) {
            return getFilters(() -> supplyFilters(secondarySupplier.get(), defaults.getMonthsBeforeExit()),
                              defaults.isSelloffStarted());
        } else {
            return Collections.emptyList();
        }
    }

    private Collection<MarketplaceFilter> refreshSellFilters() {
        return getFilters(() -> Collections.unmodifiableCollection(sellSupplier.get()), defaults.isSelloffStarted());
    }

    private synchronized void refresh() {
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
        final boolean exitActive = lastCheckedMonthsBeforeExit > -1;
        if (sellOffTriggered) {
            if (lastCheckedSellOffStarted) {
                LOGGER.info("Exit sell-off in effect. No new loans will be invested, full portfolio is up for sale.");
            } else if (exitActive) {
                LOGGER.info("Exit sell-off no longer in effect, exit strategy still active.");
            } else {
                LOGGER.info("Returning from exit strategy, resuming normal operation.");
            }
        } else if (exitStrategyTriggered) { // no need to notify of this if already notified of the big sell-off
            if (exitActive) {
                LOGGER.info("Exit strategy is active. New loans and participations over {} months will be ignored.",
                            lastCheckedMonthsBeforeExit);
            } else {
                LOGGER.info("Returning from exit strategy, resuming normal operation.");
            }
        }
    }

    private Collection<MarketplaceFilter> getFilters(final Supplier<Collection<MarketplaceFilter>> filters) {
        refresh();
        return filters.get();
    }

    public Collection<MarketplaceFilter> getPrimaryMarketplaceFilters() {
        return getFilters(() -> primaryMarketplaceFilters);
    }

    public Collection<MarketplaceFilter> getSecondaryMarketplaceFilters() {
        return getFilters(() -> secondaryMarketplaceFilters);
    }

    public Collection<MarketplaceFilter> getSellFilters() {
        return getFilters(() -> sellFilters);
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
