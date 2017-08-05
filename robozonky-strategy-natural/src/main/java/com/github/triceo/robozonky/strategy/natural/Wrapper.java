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

package com.github.triceo.robozonky.strategy.natural;

import java.math.BigDecimal;

import com.github.triceo.robozonky.api.remote.entities.Loan;
import com.github.triceo.robozonky.api.remote.entities.Participation;
import com.github.triceo.robozonky.api.remote.enums.MainIncomeType;
import com.github.triceo.robozonky.api.remote.enums.Purpose;
import com.github.triceo.robozonky.api.remote.enums.Rating;

public class Wrapper {

    private final MainIncomeType mainIncomeType;
    private final BigDecimal interestRate;
    private final Purpose purpose;
    private final Rating rating;
    private final int remainingTermInMonths;

    public Wrapper(final Loan loan) {
        this.mainIncomeType = loan.getMainIncomeType();
        this.interestRate = loan.getInterestRate();
        this.purpose = loan.getPurpose();
        this.rating = loan.getRating();
        this.remainingTermInMonths = loan.getTermInMonths();
    }

    public Wrapper(final Participation participation) {
        this.mainIncomeType = participation.getIncomeType();
        this.interestRate = participation.getInterestRate();
        this.purpose = participation.getPurpose();
        this.rating = participation.getRating();
        this.remainingTermInMonths = participation.getRemainingInstalmentCount();
    }

    public MainIncomeType getMainIncomeType() {
        return mainIncomeType;
    }

    public BigDecimal getInterestRate() {
        return interestRate;
    }

    public Purpose getPurpose() {
        return purpose;
    }

    public Rating getRating() {
        return rating;
    }

    public int getRemainingTermInMonths() {
        return remainingTermInMonths;
    }
}
