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

package com.github.triceo.robozonky.app.investing;

import java.util.Collection;
import java.util.Optional;

import com.github.triceo.robozonky.api.notifications.LoanRecommendedEvent;
import com.github.triceo.robozonky.api.notifications.StrategyCompletedEvent;
import com.github.triceo.robozonky.api.notifications.StrategyStartedEvent;
import com.github.triceo.robozonky.api.strategies.InvestmentStrategy;
import com.github.triceo.robozonky.api.strategies.LoanDescriptor;
import com.github.triceo.robozonky.api.strategies.Recommendation;
import com.github.triceo.robozonky.app.Events;

final class StrategyBasedInvestmentCommand implements InvestmentCommand {

    private final InvestmentStrategy strategy;
    private final Collection<LoanDescriptor> loans;

    public StrategyBasedInvestmentCommand(final InvestmentStrategy strategy,
                                          final Collection<LoanDescriptor> loans) {
        this.strategy = strategy;
        this.loans = loans;
    }

    @Override
    public Collection<LoanDescriptor> getLoans() {
        return loans;
    }

    @Override
    public void accept(final Session s) {
        Events.fire(new StrategyStartedEvent(strategy, s.getAvailableLoans(), s.getPortfolioOverview()));
        do {
            final Optional<Recommendation> invested = strategy.evaluate(s.getAvailableLoans(), s.getPortfolioOverview())
                    .peek(r -> Events.fire(new LoanRecommendedEvent(r)))
                    .filter(s::invest)
                    .findFirst();
            if (!invested.isPresent()) { // there is nothing to invest into; RoboZonky is finished now
                break;
            }
        } while (s.getPortfolioOverview().getCzkAvailable() >= 0);
        Events.fire(new StrategyCompletedEvent(strategy, s.getInvestmentsMade(), s.getPortfolioOverview()));
    }

}
