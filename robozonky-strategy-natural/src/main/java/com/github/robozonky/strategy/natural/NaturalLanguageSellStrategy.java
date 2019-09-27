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

package com.github.robozonky.strategy.natural;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.strategies.InvestmentDescriptor;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.api.strategies.RecommendedInvestment;
import com.github.robozonky.api.strategies.SellStrategy;

import java.util.Collection;
import java.util.Comparator;
import java.util.stream.Stream;

class NaturalLanguageSellStrategy implements SellStrategy {

    private static final Comparator<RecommendedInvestment> COMPARATOR =
            Comparator.comparing(r -> r.descriptor().item().getSmpFee().orElse(Money.ZERO));
    private final ParsedStrategy strategy;

    public NaturalLanguageSellStrategy(final ParsedStrategy p) {
        this.strategy = p;
    }

    private static boolean isFree(final InvestmentDescriptor descriptor) {
        final Money fee = descriptor.item().getSmpFee().orElse(Money.ZERO);
        return fee.getValue().signum() == 0;
    }

    @Override
    public Stream<RecommendedInvestment> recommend(final Collection<InvestmentDescriptor> available,
                                                   final PortfolioOverview portfolio) {
        return strategy.getSellingMode()
                .map(mode -> {
                    switch (mode) {
                        case SELL_FILTERS:
                            return strategy.getMatchingSellFilters(available, portfolio);
                        case FREE_AND_OUTSIDE_STRATEGY:
                            return strategy.getMatchingPrimaryMarketplaceFilters(available, portfolio)
                                    .filter(NaturalLanguageSellStrategy::isFree);
                        default:
                            throw new IllegalStateException("Impossible.");
                    }
                })
                .orElse(Stream.empty())
                .map(InvestmentDescriptor::recommend) // must do full amount; Zonky enforces
                .flatMap(r -> r.map(Stream::of).orElse(Stream.empty()))
                .sorted(COMPARATOR); // investments without sale fee come first
    }
}
