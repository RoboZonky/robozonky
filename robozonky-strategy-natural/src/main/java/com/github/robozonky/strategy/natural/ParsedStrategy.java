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

package com.github.robozonky.strategy.natural;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.Ratio;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.InvestmentDescriptor;
import com.github.robozonky.api.strategies.LoanDescriptor;
import com.github.robozonky.api.strategies.ParticipationDescriptor;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.api.strategies.ReservationDescriptor;
import com.github.robozonky.api.strategies.ReservationMode;
import com.github.robozonky.strategy.natural.conditions.MarketplaceFilter;
import com.github.robozonky.strategy.natural.wrappers.Wrapper;

class ParsedStrategy {

    private final DefaultValues defaults;
    private final Map<Rating, PortfolioShare> portfolio;
    private final Map<Rating, MoneyRange> investmentSizes;
    private final Map<Rating, MoneyRange> purchaseSizes;
    private final FilterSupplier filters;
    private RoboZonkyVersion minimumVersion;

    public ParsedStrategy(final DefaultPortfolio portfolio) {
        this(portfolio, Collections.emptySet());
    }

    ParsedStrategy(final DefaultValues values) {
        this(values, Collections.emptySet(), Collections.emptyMap(), Collections.emptyMap(),
                new FilterSupplier(values));
    }

    ParsedStrategy(final DefaultPortfolio portfolio, final Collection<MarketplaceFilter> filters) {
        this(new DefaultValues(portfolio), filters);
    }

    ParsedStrategy(final DefaultValues values, final Collection<MarketplaceFilter> filters) {
        this(values, new FilterSupplier(values, filters));
    }

    ParsedStrategy(final DefaultValues values, final FilterSupplier filters) {
        this(values, Collections.emptyList(), Collections.emptyMap(), Collections.emptyMap(), filters);
    }

    ParsedStrategy(final DefaultValues defaults, final Collection<PortfolioShare> portfolio,
            final Map<Rating, MoneyRange> investmentSizes, final Map<Rating, MoneyRange> purchaseSizes) {
        this(defaults, portfolio, investmentSizes, purchaseSizes, new FilterSupplier(defaults));
    }

    public ParsedStrategy(final DefaultValues defaults, final Collection<PortfolioShare> portfolio,
            final Map<Rating, MoneyRange> investmentSizes,
            final Map<Rating, MoneyRange> purchaseSizes, final FilterSupplier filters) {
        this.defaults = defaults;
        this.portfolio = portfolio.isEmpty() ? Collections.emptyMap()
                : new EnumMap<>(portfolio.stream()
                    .collect(Collectors.toMap(PortfolioShare::getRating, Function.identity())));
        this.investmentSizes = investmentSizes.isEmpty() ? Collections.emptyMap() : new EnumMap<>(investmentSizes);
        this.purchaseSizes = purchaseSizes.isEmpty() ? Collections.emptyMap() : new EnumMap<>(purchaseSizes);
        this.filters = filters;
    }

    private static boolean matchesFilter(final Wrapper<?> item, final Collection<MarketplaceFilter> filters,
            final String logMessage) {
        return filters.stream()
            .filter(f -> f.test(item))
            .peek(f -> Audit.LOGGER.debug(logMessage, item.getId(), f))
            .findFirst()
            .isPresent();
    }

    public int getMinimumInvestmentShareInPercent() {
        return defaults.getInvestmentShare()
            .getMinimumShareInPercent();
    }

    public int getMaximumInvestmentShareInPercent() {
        return defaults.getInvestmentShare()
            .getMaximumShareInPercent();
    }

    public Money getMaximumInvestmentSize() {
        return defaults.getTargetPortfolioSize();
    }

    public Optional<RoboZonkyVersion> getMinimumVersion() {
        return Optional.ofNullable(minimumVersion);
    }

    public void setMinimumVersion(final RoboZonkyVersion minimumVersion) {
        this.minimumVersion = minimumVersion;
    }

    public Ratio getPermittedShare(final Rating rating) {
        if (portfolio.containsKey(rating)) {
            return portfolio.get(rating)
                .getPermitted();
        } else { // no maximum share specified; calculate minimum share and use it as maximum too
            return defaults.getPortfolio()
                .getDefaultShare(rating);
        }
    }

    private MoneyRange getInvestmentSize(final Rating rating) {
        return investmentSizes.getOrDefault(rating, defaults.getInvestmentSize());
    }

    public Money getMinimumInvestmentSize(final Rating rating) {
        return getInvestmentSize(rating).getMinimumInvestment();
    }

    public Money getMaximumInvestmentSize(final Rating rating) {
        return getInvestmentSize(rating).getMaximumInvestment();
    }

    private MoneyRange getPurchaseSize(final Rating rating) {
        return purchaseSizes.getOrDefault(rating, defaults.getPurchaseSize());
    }

    public Money getMinimumPurchaseSize(final Rating rating) {
        return getPurchaseSize(rating).getMinimumInvestment();
    }

    public Money getMaximumPurchaseSize(final Rating rating) {
        return getPurchaseSize(rating).getMaximumInvestment();
    }

    private <T> Stream<T> getApplicable(final Stream<Wrapper<T>> wrappers, final String type) {
        var loanFilters = filters.getPrimaryMarketplaceFilters();
        var investmentFilters = filters.getSellFilters();
        return wrappers
            .filter(w -> !matchesFilter(w, loanFilters, type + " #{} skipped due to primary marketplace filter {}."))
            .filter(w -> !matchesFilter(w, investmentFilters, type + " #{} skipped due to sell filter {}."))
            .map(Wrapper::getOriginal);
    }

    private <T> boolean isApplicable(final Wrapper<T> wrapper, final String type) {
        if (matchesFilter(wrapper, filters.getPrimaryMarketplaceFilters(),
                type + " #{} skipped due to primary marketplace filter {}.")) {
            return false;
        }
        return !matchesFilter(wrapper, filters.getSellFilters(), type + " #{} skipped due to sell filter {}.");
    }

    public boolean isApplicable(final LoanDescriptor d, final PortfolioOverview portfolioOverview) {
        return isApplicable(Wrapper.wrap(d, portfolioOverview), "Loan");
    }

    public boolean isApplicable(final ParticipationDescriptor d, final PortfolioOverview portfolioOverview) {
        var wrapper = Wrapper.wrap(d, portfolioOverview);
        if (matchesFilter(wrapper, filters.getSecondaryMarketplaceFilters(),
                "Participation #{} skipped due to secondary marketplace filter {}.")) {
            return false;
        }
        return !matchesFilter(wrapper, filters.getSellFilters(), "Participation #{} skipped due to sell filter {}.");
    }

    public boolean isApplicable(final ReservationDescriptor d, final PortfolioOverview portfolioOverview) {
        return isApplicable(Wrapper.wrap(d, portfolioOverview), "Reservation");
    }

    public boolean isPurchasingEnabled() {
        return filters.isSecondaryMarketplaceEnabled();
    }

    public boolean isInvestingEnabled() {
        return filters.isPrimaryMarketplaceEnabled();
    }

    public Optional<ReservationMode> getReservationMode() {
        return defaults.getReservationMode();
    }

    public Optional<SellingMode> getSellingMode() {
        return defaults.getSellingMode();
    }

    public boolean matchesSellFilters(final InvestmentDescriptor d, final PortfolioOverview portfolioOverview) {
        return matchesFilter(Wrapper.wrap(d, portfolioOverview), filters.getSellFilters(),
                "Investment #{} to be sold due to sell filter {}.");
    }

    public boolean matchesPrimaryMarketplaceFilters(final InvestmentDescriptor d,
            final PortfolioOverview portfolioOverview) {
        return matchesFilter(Wrapper.wrap(d, portfolioOverview), filters.getPrimaryMarketplaceFilters(),
                "Investment #{} sellable due to primary marketplace filter {}.");
    }

    @Override
    public String toString() {
        return "ParsedStrategy{" +
                "defaults=" + defaults +
                ", investmentSizes=" + investmentSizes +
                ", purchaseSizes=" + purchaseSizes +
                ", minimumVersion=" + minimumVersion +
                ", portfolio=" + portfolio +
                ", filters=" + filters +
                '}';
    }
}
