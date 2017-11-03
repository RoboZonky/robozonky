/*
 * Copyright 2017 The RoboZonky Project
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

package com.github.robozonky.strategy.natural;

import java.math.BigDecimal;
import java.util.Objects;

import com.github.robozonky.api.remote.entities.Investment;
import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.api.remote.entities.Participation;
import com.github.robozonky.api.remote.enums.MainIncomeType;
import com.github.robozonky.api.remote.enums.Purpose;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.remote.enums.Region;

public class Wrapper {

    private final Loan loan;
    private final String identifier;
    private final int remainingTermInMonths, originalTermInMonths;
    private final BigDecimal remainingAmount;

    public Wrapper(final Loan loan) {
        this.loan = loan;
        this.identifier = "Loan #" + loan.getId();
        this.remainingTermInMonths = loan.getTermInMonths();
        this.originalTermInMonths = loan.getTermInMonths();
        this.remainingAmount = null;
    }

    public Wrapper(final Participation participation, final Loan loan) {
        this.loan = loan;
        this.identifier = "Loan #" + loan.getId() + " (participation #" + participation.getId() + ")";
        this.remainingTermInMonths = participation.getRemainingInstalmentCount();
        this.originalTermInMonths = participation.getOriginalInstalmentCount();
        this.remainingAmount = null;
    }

    public Wrapper(final Investment investment, final Loan loan) {
        this.loan = loan;
        this.identifier = "Loan #" + loan.getId() + " (investment #" + investment.getId() + ")";
        this.remainingTermInMonths = investment.getRemainingMonths();
        this.originalTermInMonths = investment.getLoanTermInMonth();
        this.remainingAmount = investment.getRemainingPrincipal();
    }

    public int getLoanId() {
        return loan.getId();
    }

    public Region getRegion() {
        return loan.getRegion();
    }

    public String getStory() {
        return loan.getStory();
    }

    public MainIncomeType getMainIncomeType() {
        return loan.getMainIncomeType();
    }

    public BigDecimal getInterestRate() {
        return loan.getInterestRate();
    }

    public Purpose getPurpose() {
        return loan.getPurpose();
    }

    public Rating getRating() {
        return loan.getRating();
    }

    public int getOriginalTermInMonths() {
        return originalTermInMonths;
    }

    public int getRemainingTermInMonths() {
        return remainingTermInMonths;
    }

    public int getOriginalAmount() {
        return (int) loan.getAmount();
    }

    public BigDecimal getRemainingAmount() {
        if (remainingAmount == null) {
            throw new IllegalStateException("Cannot request remaining amount here.");
        }
        return remainingAmount;
    }

    public String getIdentifier() {
        return identifier;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final Wrapper wrapper = (Wrapper) o;
        return Objects.equals(identifier, wrapper.identifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier);
    }
}
