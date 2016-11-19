/*
 * Copyright 2016 Lukáš Petrovický
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

package com.github.triceo.robozonky;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;

import com.github.triceo.robozonky.api.events.StrategyCompleteEvent;
import com.github.triceo.robozonky.api.remote.entities.Investment;
import com.github.triceo.robozonky.api.strategies.InvestmentStrategy;

class StrategyCompleteEventImpl implements StrategyCompleteEvent {

    private final InvestmentStrategy strategy;
    private final Collection<Investment> investments;
    private final BigDecimal balance;

    public StrategyCompleteEventImpl(final InvestmentStrategy strategy, final Collection<Investment> result,
                                     final BigDecimal balance) {
        this.strategy = strategy;
        this.investments = Collections.unmodifiableCollection(result);
        this.balance = balance;
    }

    @Override
    public InvestmentStrategy getStrategy() {
        return this.strategy;
    }

    @Override
    public Collection<Investment> getInvestments() {
        return this.investments;
    }

    @Override
    public BigDecimal getBalance() {
        return this.balance;
    }
}
