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
import java.util.List;

import com.github.triceo.robozonky.api.events.StrategyStartedEvent;
import com.github.triceo.robozonky.api.strategies.InvestmentStrategy;
import com.github.triceo.robozonky.api.strategies.LoanDescriptor;

class StrategyStartedEventImpl implements StrategyStartedEvent {

    private final InvestmentStrategy strategy;
    private final List<LoanDescriptor> loanDesciptors;
    private final BigDecimal balance;

    public StrategyStartedEventImpl(final InvestmentStrategy strategy, final List<LoanDescriptor> loans,
                                    final BigDecimal balance) {
        this.strategy = strategy;
        this.loanDesciptors = Collections.unmodifiableList(loans);
        this.balance = balance;
    }

    @Override
    public InvestmentStrategy getStrategy() {
        return strategy;
    }

    @Override
    public Collection<LoanDescriptor> getLoanDesciptors() {
        return loanDesciptors;
    }

    @Override
    public BigDecimal getBalance() {
        return this.balance;
    }
}
