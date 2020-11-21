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

import java.util.function.Supplier;

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

    private boolean getFreeAndOutsideStrategy(final InvestmentDescriptor available, final PortfolioOverview portfolio) {
        return isFree(available) &&
                strategy.matchesPrimaryMarketplaceFilters(available, portfolio);
    }

    @Override
    public boolean recommend(final InvestmentDescriptor investmentDescriptor,
            final Supplier<PortfolioOverview> portfolioOverviewSupplier,
            final SessionInfo sessionInfo) {
        return strategy.getSellingMode()
            .map(mode -> {
                var portfolio = portfolioOverviewSupplier.get();
                switch (mode) {
                    case SELL_FILTERS:
                        return strategy.matchesSellFilters(investmentDescriptor, portfolio);
                    case FREE_AND_OUTSIDE_STRATEGY:
                        return getFreeAndOutsideStrategy(investmentDescriptor, portfolio);
                    case FREE_UNDISCOUNTED_AND_OUTSIDE_STRATEGY:
                        return isUndiscounted(investmentDescriptor) &&
                                getFreeAndOutsideStrategy(investmentDescriptor, portfolio);
                    default:
                        throw new IllegalStateException("Impossible.");
                }
            })
            .orElse(false);
    }
}
