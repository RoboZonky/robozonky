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

import java.util.stream.Stream;

import com.github.robozonky.api.Ratio;
import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.api.remote.entities.SellInfo;
import com.github.robozonky.api.remote.enums.SellStatus;
import com.github.robozonky.api.strategies.InvestmentDescriptor;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.api.strategies.SellStrategy;

class NaturalLanguageSellStrategy implements SellStrategy {

    private final ParsedStrategy strategy;

    public NaturalLanguageSellStrategy(final ParsedStrategy p) {
        this.strategy = p;
    }

    private static boolean isFree(final InvestmentDescriptor descriptor) {
        return descriptor.item()
            .getSellStatus() == SellStatus.SELLABLE_WITHOUT_FEE;
    }

    private static boolean isUndiscounted(final InvestmentDescriptor descriptor) {
        var investment = descriptor.item();
        var loan = investment.getLoan();
        if (loan.getDpd() == 0 && !loan.hasCollectionHistory()) {
            return true; // The loan was never late; we can safely assume there is no discount.
        }
        return investment.getSmpSellInfo()
            .map(SellInfo::getDiscount)
            .orElse(Ratio.ZERO)
            .bigDecimalValue()
            .signum() == 0;
    }

    private Stream<InvestmentDescriptor> getFreeAndOutsideStrategy(final Stream<InvestmentDescriptor> available,
            final PortfolioOverview portfolio) {
        return strategy.getMatchingPrimaryMarketplaceFilters(available, portfolio)
            .filter(NaturalLanguageSellStrategy::isFree);
    }

    @Override
    public Stream<InvestmentDescriptor> recommend(final Stream<InvestmentDescriptor> available,
            final PortfolioOverview portfolio, final SessionInfo sessionInfo) {
        return strategy.getSellingMode()
            .map(mode -> {
                switch (mode) {
                    case SELL_FILTERS:
                        return strategy.getMatchingSellFilters(available, portfolio);
                    case FREE_AND_OUTSIDE_STRATEGY:
                        return getFreeAndOutsideStrategy(available, portfolio);
                    case FREE_UNDISCOUNTED_AND_OUTSIDE_STRATEGY:
                        return getFreeAndOutsideStrategy(available, portfolio)
                            .filter(NaturalLanguageSellStrategy::isUndiscounted);
                    default:
                        throw new IllegalStateException("Impossible.");
                }
            })
            .orElse(Stream.empty());
    }
}
