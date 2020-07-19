/*
 * Copyright 2020 The RoboZonky Project
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
import java.util.function.Supplier;

import com.github.robozonky.api.notifications.LoanDelinquentEvent;
import com.github.robozonky.api.remote.entities.Investment;
import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.api.remote.entities.SellInfo;

abstract class AbstractLoanDelinquentEventImpl extends AbstractEventImpl implements LoanDelinquentEvent {

    private final Investment investment;
    private final Loan loan;
    private final Supplier<SellInfo> sellInfoSupplier;

    AbstractLoanDelinquentEventImpl(final Investment investment, final Loan loan,
            final Supplier<SellInfo> sellInfoSupplier) {
        this.investment = investment;
        this.loan = loan;
        this.sellInfoSupplier = sellInfoSupplier;
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
        var currentDaysInDue = investment.getLegalDpd()
            .orElse(0);
        return LocalDate.now()
            .minusDays(currentDaysInDue);
    }

    @Override
    public SellInfo getSellInfo() {
        return sellInfoSupplier.get();
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", getClass().getSimpleName() + "[", "]")
            .add("super=" + super.toString())
            .add("loan=" + loan)
            .add("investment=" + investment)
            .toString();
    }
}
