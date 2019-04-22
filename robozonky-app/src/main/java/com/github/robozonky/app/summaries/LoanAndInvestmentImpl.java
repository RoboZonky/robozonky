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

import com.github.robozonky.api.notifications.LoanAndInvestment;
import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.entities.sanitized.Loan;

final class LoanAndInvestmentImpl implements LoanAndInvestment {

    private final Investment investment;
    private final Loan loan;

    public LoanAndInvestmentImpl(final Investment i, final Loan l) {
        if (i.getLoanId() != l.getId()) { // a bit of defensive programming
            throw new IllegalArgumentException("Investment and Loan don't match.");
        }
        this.investment = i;
        this.loan = l;
    }

    @Override
    public Investment getInvestment() {
        return investment;
    }

    @Override
    public Loan getLoan() {
        return loan;
    }
}
