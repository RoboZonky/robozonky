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

import static com.github.robozonky.strategy.natural.Audit.LOGGER;

import java.util.Optional;
import java.util.function.Supplier;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.api.strategies.InvestmentStrategy;
import com.github.robozonky.api.strategies.LoanDescriptor;
import com.github.robozonky.api.strategies.PortfolioOverview;

class NaturalLanguageInvestmentStrategy implements InvestmentStrategy {

    private final ParsedStrategy strategy;
    private final InvestmentSizeRecommender recommender;

    public NaturalLanguageInvestmentStrategy(final ParsedStrategy p) {
        this.strategy = p;
        this.recommender = new InvestmentSizeRecommender(p);
    }

    @Override
    public Optional<Money> recommend(final LoanDescriptor loanDescriptor,
            final Supplier<PortfolioOverview> portfolioOverviewSupplier, final SessionInfo sessionInfo) {
        var portfolio = portfolioOverviewSupplier.get();
        if (!Util.isAcceptable(strategy, portfolio)) {
            return Optional.empty();
        }
        var loan = loanDescriptor.item();
        LOGGER.trace("Evaluating {}.", loan);
        var preferences = Preferences.get(strategy, portfolio);
        var isAcceptable = preferences.isDesirable(loan.getRating());
        if (!isAcceptable) {
            LOGGER.debug("Loan #{} skipped due to an undesirable rating.", loan.getId());
            return Optional.empty();
        } else if (!strategy.isApplicable(loanDescriptor, portfolio)) {
            return Optional.empty();
        }
        var recommendedAmount = recommender.apply(loan, sessionInfo);
        if (recommendedAmount.isZero()) {
            return Optional.empty();
        }
        return Optional.of(recommendedAmount);
    }
}
