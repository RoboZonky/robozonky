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

package com.github.triceo.robozonky.strategy.rules;

import com.github.triceo.robozonky.api.remote.entities.Loan;

public class ProcessedLoan implements Comparable<ProcessedLoan> {

    private final Loan loan;
    private final int priority;
    private int amount = 0;
    private boolean confirmationRequired = false;

    public ProcessedLoan(final Loan loan, final int priority) {
        this.loan = loan;
        this.priority = priority;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(final int amount) {
        this.amount = amount;
    }

    public boolean isConfirmationRequired() {
        return confirmationRequired;
    }

    public void setConfirmationRequired(final boolean confirmationRequired) {
        this.confirmationRequired = confirmationRequired;
    }

    public Loan getLoan() {
        return loan;
    }

    public int getPriority() {
        return priority;
    }

    @Override
    public int compareTo(final ProcessedLoan other) {
        return new ProcessedLoanComparator().compare(this, other);
    }

}
