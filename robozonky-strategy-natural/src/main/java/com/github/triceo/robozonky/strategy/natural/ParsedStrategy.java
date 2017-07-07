/*
 * Copyright 2017 Lukáš Petrovický
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

package com.github.triceo.robozonky.strategy.natural;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.triceo.robozonky.api.remote.enums.Rating;
import com.github.triceo.robozonky.api.strategies.LoanDescriptor;

class ParsedStrategy {

    private final DefaultValues defaults;
    private final Map<Rating, PortfolioStructureItem> portfolioStructureItems;
    private final Map<Rating, InvestmentSizeItem> investmentSizes;
    private final Collection<MarketplaceFilter> marketplaceFilters;

    public ParsedStrategy(final DefaultValues defaults,
                          final Collection<PortfolioStructureItem> portfolioStructureItems,
                          final Collection<InvestmentSizeItem> investmentSizeItems,
                          final Collection<MarketplaceFilter> marketplaceFilters) {
        this.defaults = defaults;
        this.portfolioStructureItems = portfolioStructureItems.stream()
                .collect(Collectors.toMap(PortfolioStructureItem::getRating, Function.identity()));
        this.investmentSizes = investmentSizeItems.stream()
                .collect(Collectors.toMap(InvestmentSizeItem::getRating, Function.identity()));
        this.marketplaceFilters = new LinkedHashSet<>(marketplaceFilters);
    }

    public boolean needsConfirmation(final LoanDescriptor loan) {
        return defaults.needsConfirmation(loan.getLoan());
    }

    public int getMinimumBalance() {
        return defaults.getMinimumBalance();
    }

    public int getMaximumInvestmentSizeInCzk() {
        return defaults.getTargetPortfolioSize();
    }

    public int getMinimumShare(final Rating rating) {
        if (portfolioStructureItems.containsKey(rating)) {
            return portfolioStructureItems.get(rating).getMininumShareInPercent();
        } else { // no minimum share specified; average the minimum share based on number of all unspecified ratings
            final int providedRatingCount = portfolioStructureItems.size();
            final int remainingShare = 100 - portfolioStructureItems.values().stream()
                    .mapToInt(PortfolioStructureItem::getMininumShareInPercent)
                    .sum();
            return remainingShare / providedRatingCount;
        }
    }

    public int getMaximumShare(final Rating rating) {
        if (portfolioStructureItems.containsKey(rating)) {
            return portfolioStructureItems.get(rating).getMaximumShareInPercent();
        } else { // no maximum share specified; calculate minimum share and use it as maximum too
            return this.getMinimumShare(rating);
        }
    }

    public int getMinimumInvestmentSizeInCzk(final Rating rating) {
        if (investmentSizes.containsKey(rating)) {
            return investmentSizes.get(rating).getMinimumInvestmentInCzk();
        } else { // no minimum share specified; use default
            return defaults.getInvestmentSize().getMinimumInvestmentInCzk();
        }
    }

    public int getMaximumInvestmentSizeInCzk(final Rating rating) {
        if (investmentSizes.containsKey(rating)) {
            return investmentSizes.get(rating).getMaximumInvestmentInCzk();
        } else { // no maximum share specified; use default
            return defaults.getInvestmentSize().getMaximumInvestmentInCzk();
        }
    }

    public Stream<LoanDescriptor> getApplicableLoans(final Collection<LoanDescriptor> loans) {
        return loans.stream().filter(l -> marketplaceFilters.stream().allMatch(f -> f.test(l.getLoan())));
    }

}
