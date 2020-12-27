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

package com.github.robozonky.strategy.natural.wrappers;

import java.math.BigDecimal;
import java.util.OptionalInt;

import com.github.robozonky.api.Ratio;
import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.api.remote.enums.MainIncomeType;
import com.github.robozonky.api.remote.enums.Purpose;
import com.github.robozonky.api.remote.enums.Region;
import com.github.robozonky.api.strategies.LoanDescriptor;
import com.github.robozonky.api.strategies.PortfolioOverview;

final class LoanWrapper extends AbstractWrapper<LoanDescriptor> {

    private final Loan loan;

    public LoanWrapper(final LoanDescriptor original, final PortfolioOverview portfolioOverview) {
        super(original, portfolioOverview);
        this.loan = original.item();
    }

    @Override
    public long getId() {
        return loan.getId();
    }

    @Override
    public boolean isInsuranceActive() {
        return loan.isInsuranceActive();
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
    public Ratio getInterestRate() {
        return loan.getInterestRate();
    }

    @Override
    public Ratio getRevenueRate() {
        return loan.getRevenueRate()
            .orElseGet(super::getRevenueRate);
    }

    @Override
    public Purpose getPurpose() {
        return loan.getPurpose();
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
        return loan.getAmount()
            .getValue()
            .intValue();
    }

    @Override
    public int getOriginalAnnuity() {
        return loan.getAnnuity()
            .getValue()
            .intValue();
    }

    @Override
    public OptionalInt getCurrentDpd() {
        return OptionalInt.empty();
    }

    @Override
    public OptionalInt getLongestDpd() {
        return OptionalInt.empty();
    }

    @Override
    public OptionalInt getDaysSinceDpd() {
        return OptionalInt.empty();
    }

    @Override
    public BigDecimal getRemainingPrincipal() {
        return BigDecimal.valueOf(getOriginalAmount());
    }

    @Override
    public String toString() {
        return "Wrapper for loan #" + getId();
    }
}
