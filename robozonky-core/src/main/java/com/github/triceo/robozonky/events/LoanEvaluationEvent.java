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

import com.github.triceo.robozonky.remote.Loan;
import com.github.triceo.robozonky.strategy.InvestmentStrategy;

/**
 * Fired immediately before {@link InvestmentStrategy} is asked to evaluate a particular loan. Based on the strategy's
 * deliberations, {@link InvestmentRequestedEvent} may be fired next.
 */
public class LoanEvaluationEvent implements Event {

    private final Loan loan;

    public LoanEvaluationEvent(final Loan loan) {
        this.loan = loan;
    }

    /*
     * @return The loan to be evaluated.
     */
    public Loan getLoan() {
        return loan;
    }
}
