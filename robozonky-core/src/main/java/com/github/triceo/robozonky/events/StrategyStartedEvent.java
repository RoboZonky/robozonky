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

package com.github.triceo.robozonky.events;

import java.math.BigDecimal;
import java.util.Collection;

import com.github.triceo.robozonky.remote.Loan;
import com.github.triceo.robozonky.strategy.InvestmentStrategy;

/**
 * Fired before any loans are submitted for evaluation by the strategy and subsequent investment operations. May be
 * followed by {@link LoanEvaluationEvent} and will eventually be followed by {@link StrategyCompleteEvent}.
 */
public class StrategyStartedEvent implements Event {

    private final InvestmentStrategy strategy;
    private final Collection<Loan> loans;
    private final BigDecimal balance;

    public StrategyStartedEvent(final InvestmentStrategy strategy, final Collection<Loan> loans, final BigDecimal
            balance) {
        this.strategy = strategy;
        this.loans = loans;
        this.balance = balance;
    }

    /**
     * @return Strategy to use for evaluation of loans.
     */
    public InvestmentStrategy getStrategy() {
        return strategy;
    }

    /**
     * @return Loans that will be evaluated.
     */
    public Collection<Loan> getLoans() {
        return loans;
    }

    /**
     * @return Current balance reported by Zonky API.
     */
    public BigDecimal getBalance() {
        return balance;
    }
}
