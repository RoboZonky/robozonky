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

package com.github.robozonky.app.summaries;

import java.util.function.IntFunction;

import com.github.robozonky.api.notifications.LoanAndInvestment;
import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import io.vavr.Lazy;

final class LoanAndInvestmentImpl implements LoanAndInvestment {

    private final Investment investment;
    private final Lazy<Loan> loan;

    public LoanAndInvestmentImpl(final Investment i, final IntFunction<Loan> loanSupplier) {
        this.investment = i;
        this.loan = Lazy.of(() -> loanSupplier.apply(i.getLoanId()));
    }

    @Override
    public Investment getInvestment() {
        return investment;
    }

    @Override
    public Loan getLoan() {
        return loan.get();
    }
}
