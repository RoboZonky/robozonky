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

import java.util.StringJoiner;
import java.util.function.Supplier;

import com.github.robozonky.api.notifications.LoanNoLongerDelinquentEvent;
import com.github.robozonky.api.remote.entities.Investment;
import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.api.remote.entities.SellInfo;

final class LoanNoLongerDelinquentEventImpl extends AbstractEventImpl implements LoanNoLongerDelinquentEvent {

    private final Investment investment;
    private final Loan loan;
    private final Supplier<SellInfo> sellInfoSupplier;

    public LoanNoLongerDelinquentEventImpl(final Investment investment, final Loan loan,
            final Supplier<SellInfo> sellInfoSupplier) {
        this.investment = investment;
        this.loan = loan;
        this.sellInfoSupplier = sellInfoSupplier;
    }

    @Override
    public Loan getLoan() {
        return loan;
    }

    @Override
    public Investment getInvestment() {
        return investment;
    }

    @Override
    public SellInfo getSellInfo() {
        return sellInfoSupplier.get();
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", LoanNoLongerDelinquentEventImpl.class.getSimpleName() + "[", "]")
            .add("super=" + super.toString())
            .add("investment=" + investment)
            .add("loan=" + loan)
            .toString();
    }
}
