/*
 * Copyright 2018 The RoboZonky Project
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

import com.github.robozonky.api.remote.entities.sanitized.MarketplaceLoan;
import com.github.robozonky.api.remote.enums.MainIncomeType;
import com.github.robozonky.api.remote.enums.Purpose;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.remote.enums.Region;
import com.github.robozonky.api.strategies.LoanDescriptor;

final class LoanWrapper implements Wrapper<LoanDescriptor> {

    private final MarketplaceLoan loan;
    private final LoanDescriptor original;

    public LoanWrapper(final LoanDescriptor original) {
        this.original = original;
        this.loan = original.item();
    }

    @Override
    public boolean isInsuranceActive() {
        return loan.isInsuranceActive();
    }

    @Override
    public int getLoanId() {
        return loan.getId();
    }

    @Override
    public Region getRegion() {
        return loan.getRegion();
    }

    @Override
    public String getStory() {
        return loan.getStory();
    }

    @Override
    public MainIncomeType getMainIncomeType() {
        return loan.getMainIncomeType();
    }

    @Override
    public BigDecimal getInterestRate() {
        return loan.getInterestRate();
    }

    @Override
    public Purpose getPurpose() {
        return loan.getPurpose();
    }

    @Override
    public Rating getRating() {
        return loan.getRating();
    }

    @Override
    public int getOriginalTermInMonths() {
        return loan.getTermInMonths();
    }

    @Override
    public int getRemainingTermInMonths() {
        return loan.getTermInMonths();
    }

    @Override
    public int getOriginalAmount() {
        return loan.getAmount();
    }

    @Override
    public BigDecimal getRemainingAmount() {
        throw new UnsupportedOperationException();
    }

    @Override
    public LoanDescriptor getOriginal() {
        return original;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !Objects.equals(getClass(), o.getClass())) {
            return false;
        }
        final LoanWrapper that = (LoanWrapper) o;
        return Objects.equals(original, that.original);
    }

    @Override
    public int hashCode() {
        return Objects.hash(original);
    }

    @Override
    public String toString() {
        return "Wrapper for loan #" + loan.getId();
    }

}
