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

package com.github.triceo.robozonky.app.events;

import com.github.triceo.robozonky.events.Event;
import com.github.triceo.robozonky.remote.Loan;

/**
 * Immediately after @{@link MarketplaceCheckStartedEvent}, when a loan is present that can be invested into right
 * away.
 */
public class UnprotectedLoanArrivalEvent implements Event {

    private final Loan loan;

    public UnprotectedLoanArrivalEvent(final Loan loan) {
        this.loan = loan;
    }

    /**
     * @return The loan in question.
     */
    public Loan getLoan() {
        return loan;
    }

}
