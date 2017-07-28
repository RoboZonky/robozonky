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

import java.time.OffsetDateTime;

import com.github.triceo.robozonky.api.remote.entities.Investment;
import com.github.triceo.robozonky.api.remote.entities.Loan;

/**
 * Fired immediately after an {@link Investment} is identified as delinquent.
 */
public abstract class LoanDelinquentEvent extends Event {

    private final Loan loan;
    private final OffsetDateTime since;

    public LoanDelinquentEvent(final Loan loan, final OffsetDateTime since) {
        this.loan = loan;
        this.since = since;
    }

    public Loan getLoan() {
        return loan;
    }

    public OffsetDateTime getSince() {
        return since;
    }
}
