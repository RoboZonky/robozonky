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

package com.github.robozonky.app.daemon.operations;

import java.util.Collection;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import com.github.robozonky.api.remote.entities.Restrictions;
import com.github.robozonky.api.strategies.InvestmentStrategy;
import com.github.robozonky.api.strategies.LoanDescriptor;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.api.strategies.RecommendedLoan;

class RestrictedInvestmentStrategy
        implements BiFunction<Collection<LoanDescriptor>, PortfolioOverview, Stream<RecommendedLoan>> {

    private final InvestmentStrategy strategy;
    private final Restrictions restrictions;

    public RestrictedInvestmentStrategy(final InvestmentStrategy strategy, final Restrictions restrictions) {
        this.strategy = strategy;
        this.restrictions = restrictions;
    }

    @Override
    public Stream<RecommendedLoan> apply(final Collection<LoanDescriptor> loanDescriptors,
                                         final PortfolioOverview portfolioOverview) {
        return strategy.recommend(loanDescriptors, portfolioOverview, restrictions);
    }
}
