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

package com.github.triceo.robozonky.api.notifications;

import java.util.Collection;
import java.util.Collections;

import com.github.triceo.robozonky.api.remote.entities.Investment;
import com.github.triceo.robozonky.api.strategies.InvestmentStrategy;
import com.github.triceo.robozonky.api.strategies.PortfolioOverview;

/**
 * Fired immediately after all loans have been evaluated and all possible investment operations performed.
 */
public final class StrategyCompletedEvent extends Event {

    private final InvestmentStrategy investmentStrategyUsed;
    private final Collection<Investment> investmentsMade;
    private final PortfolioOverview portfolioOverview;

    public StrategyCompletedEvent(final InvestmentStrategy investmentStrategy,
                                  final Collection<Investment> investmentsMade,
                                  final PortfolioOverview portfolioOverview) {
        this.investmentStrategyUsed = investmentStrategy;
        this.investmentsMade = Collections.unmodifiableCollection(investmentsMade);
        this.portfolioOverview = portfolioOverview;
    }

    public InvestmentStrategy getInvestmentStrategyUsed() {
        return investmentStrategyUsed;
    }

    public Collection<Investment> getInvestmentsMade() {
        return investmentsMade;
    }

    public PortfolioOverview getPortfolioOverview() {
        return portfolioOverview;
    }
}
