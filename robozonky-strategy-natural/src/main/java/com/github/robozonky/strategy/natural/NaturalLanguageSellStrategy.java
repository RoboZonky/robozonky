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
import java.util.Optional;
import java.util.stream.Stream;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.strategies.InvestmentDescriptor;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.api.strategies.RecommendedInvestment;
import com.github.robozonky.api.strategies.SellStrategy;

class NaturalLanguageSellStrategy implements SellStrategy {

    private final ParsedStrategy strategy;

    public NaturalLanguageSellStrategy(final ParsedStrategy p) {
        this.strategy = p;
    }

    private static boolean isFree(final InvestmentDescriptor descriptor) {
        return descriptor.item().getSmpFee()
                .map(Money::isZero)
                .orElseGet(() -> descriptor.sellInfo()
                        .map(si -> si.getPriceInfo()
                                .getFee()
                                .getValue()
                                .isZero())
                        .orElse(true));
    }

    private static boolean isUndiscounted(final InvestmentDescriptor descriptor) {
        return descriptor.sellInfo()
                        .map(si -> si.getPriceInfo()
                                .getDiscount()
                                .isZero())
                        .orElse(true);
    }

    private Stream<InvestmentDescriptor> getFreeAndOutsideStrategy(final Collection<InvestmentDescriptor> available,
                                                                   final PortfolioOverview portfolio) {
        return strategy.getMatchingPrimaryMarketplaceFilters(available.stream(), portfolio)
                .filter(NaturalLanguageSellStrategy::isFree);
    }

    @Override
    public Stream<RecommendedInvestment> recommend(final Collection<InvestmentDescriptor> available,
                                                   final PortfolioOverview portfolio) {
        return strategy.getSellingMode()
                .map(mode -> {
                    switch (mode) {
                        case SELL_FILTERS:
                            return strategy.getMatchingSellFilters(available.stream(), portfolio);
                        case FREE_AND_OUTSIDE_STRATEGY:
                            return getFreeAndOutsideStrategy(available, portfolio);
                        case FREE_UNDISCOUNTED_AND_OUTSIDE_STRATEGY:
                            return getFreeAndOutsideStrategy(available, portfolio)
                                    .filter(NaturalLanguageSellStrategy::isUndiscounted);
                        default:
                            throw new IllegalStateException("Impossible.");
                    }
                })
                .orElse(Stream.empty())
                .map(InvestmentDescriptor::recommend) // must do full amount; Zonky enforces
                .flatMap(Optional::stream);
    }
}
