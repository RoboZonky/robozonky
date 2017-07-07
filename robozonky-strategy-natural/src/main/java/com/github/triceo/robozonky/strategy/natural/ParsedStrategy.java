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
import java.util.stream.Stream;

import com.github.triceo.robozonky.api.remote.enums.Rating;
import com.github.triceo.robozonky.api.strategies.LoanDescriptor;

class ParsedStrategy {

    private final PortfolioStructure portfolioStructure;
    private final InvestmentSize investmentSize;
    private final Collection<MarketplaceFilter> marketplaceFilters;

    public ParsedStrategy(final PortfolioStructure portfolioStructure, final InvestmentSize investmentSize,
                          final Collection<MarketplaceFilter> marketplaceFilters) {
        this.portfolioStructure = portfolioStructure;
        this.investmentSize = investmentSize;
        this.marketplaceFilters = new LinkedHashSet<>(marketplaceFilters);
    }

    public boolean needsConfirmation(final LoanDescriptor loan) {
        return this.portfolioStructure.needsConfirmation(loan.getLoan());
    }

    public int getInvestmentCeiling() {
        return this.portfolioStructure.getTargetPortfolioSize();
    }

    public int getInvestmentCeiling(final Rating r) {
        return this.investmentSize.getMaximumInvestmentSizeInCzk(r);
    }

    public int getInvestmentFloor(final Rating r) {
        return this.investmentSize.getMinimumInvestmentSizeInCzk(r);
    }

    public int getRatingShareCeiling(final Rating rating) {
        return this.portfolioStructure.getMaximumShare(rating);
    }

    public Stream<LoanDescriptor> getApplicableLoans(final Collection<LoanDescriptor> loans) {
        return loans.stream().filter(l -> marketplaceFilters.stream().allMatch(f -> f.test(l.getLoan())));
    }

}
