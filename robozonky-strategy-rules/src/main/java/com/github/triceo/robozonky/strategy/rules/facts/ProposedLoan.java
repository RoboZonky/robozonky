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
package com.github.triceo.robozonky.strategy.rules.facts;

import java.time.OffsetDateTime;

import com.github.triceo.robozonky.api.remote.entities.Loan;
import com.github.triceo.robozonky.api.remote.entities.Rating;

public class ProposedLoan {

    public ProposedLoan(final Loan loan) {
        this.id = loan.getId();
        this.termInMonths = loan.getTermInMonths();
        this.amount = (int)loan.getAmount();
        this.investmentsCount = loan.getInvestmentsCount();
        this.remainingInvestment = (int)loan.getRemainingInvestment();
        this.interestRate = loan.getInterestRate() == null ? 0 : loan.getInterestRate().doubleValue();
        this.deadline = loan.getDeadline();
        this.rating = loan.getRating();
        this.investmentRate = loan.getInvestmentRate() == null ? 0 : loan.getInvestmentRate().doubleValue()    ;
    }

    private final int id, termInMonths, investmentsCount;
    private final int amount, remainingInvestment;
    private final double interestRate;
    private final OffsetDateTime deadline;
    private final Rating rating;
    private final double investmentRate;

    public double getInvestmentRate() {
        return investmentRate;
    }

    public int getId() {
        return id;
    }

    public int getTermInMonths() {
        return termInMonths;
    }

    public double getInterestRate() {
        return interestRate;
    }

    public Rating getRating() {
        return rating;
    }

    public int getAmount() {
        return amount;
    }

    public int getRemainingInvestment() {
        return remainingInvestment;
    }

    public OffsetDateTime getDeadline() {
        return deadline;
    }

    public int getInvestmentsCount() {
        return investmentsCount;
    }

}
