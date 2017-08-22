/*
 * Copyright 2017 The RoboZonky Project
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

package com.github.robozonky.app.investing;

import java.util.Collection;

import com.github.robozonky.api.notifications.LoanRecommendedEvent;
import com.github.robozonky.api.strategies.InvestmentStrategy;
import com.github.robozonky.api.strategies.LoanDescriptor;
import com.github.robozonky.app.Events;

final class InvestmentCommand {

    private final InvestmentStrategy strategy;
    private final Collection<LoanDescriptor> loans;

    public InvestmentCommand(final InvestmentStrategy strategy, final Collection<LoanDescriptor> loans) {
        this.strategy = strategy;
        this.loans = loans;
    }

    public Collection<LoanDescriptor> getLoans() {
        return loans;
    }

    public void accept(final Session s) {
        boolean invested;
        do {
            invested = strategy.recommend(s.getAvailable(), s.getPortfolioOverview())
                    .peek(r -> Events.fire(new LoanRecommendedEvent(r)))
                    .anyMatch(s::invest); // keep trying until investment opportunities are exhausted
        } while (invested);
    }
}
