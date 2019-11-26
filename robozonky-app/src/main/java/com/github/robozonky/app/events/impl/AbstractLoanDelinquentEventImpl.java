/*
 * Copyright 2019 The RoboZonky Project
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

package com.github.robozonky.app.events.impl;

import java.time.LocalDate;
import java.util.StringJoiner;

import com.github.robozonky.api.notifications.LoanDelinquentEvent;
import com.github.robozonky.api.remote.entities.Investment;
import com.github.robozonky.api.remote.entities.Loan;

abstract class AbstractLoanDelinquentEventImpl extends AbstractEventImpl implements LoanDelinquentEvent {

    private final Investment investment;
    private final Loan loan;
    private final LocalDate since;

    AbstractLoanDelinquentEventImpl(final Investment investment, final Loan loan, final LocalDate since) {
        this.investment = investment;
        this.loan = loan;
        this.since = since;
    }

    @Override
    public Investment getInvestment() {
        return investment;
    }

    @Override
    public Loan getLoan() {
        return loan;
    }

    @Override
    public LocalDate getDelinquentSince() {
        return since;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", getClass().getSimpleName() + "[", "]")
                .add("super=" + super.toString())
                .add("loan=" + loan)
                .add("investment=" + investment)
                .add("since=" + since)
                .toString();
    }
}
