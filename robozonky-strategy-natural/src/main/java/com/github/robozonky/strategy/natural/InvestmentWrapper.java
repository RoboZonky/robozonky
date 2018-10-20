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

import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.enums.MainIncomeType;
import com.github.robozonky.api.remote.enums.Purpose;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.InvestmentDescriptor;

final class InvestmentWrapper extends AbstractLoanWrapper<InvestmentDescriptor> {

    private final Investment investment;

    public InvestmentWrapper(final InvestmentDescriptor original) {
        super(original);
        this.investment = original.item();
    }

    @Override
    public boolean isInsuranceActive() {
        return investment.isInsuranceActive();
    }

    @Override
    public MainIncomeType getMainIncomeType() {
        return getLoan().getMainIncomeType();
    }

    @Override
    public BigDecimal getInterestRate() {
        return investment.getInterestRate();
    }

    @Override
    public Purpose getPurpose() {
        return getLoan().getPurpose();
    }

    @Override
    public Rating getRating() {
        return investment.getRating();
    }

    @Override
    public int getOriginalTermInMonths() {
        return investment.getOriginalTerm();
    }

    @Override
    public int getRemainingTermInMonths() {
        return investment.getRemainingMonths();
    }

    @Override
    public int getOriginalAmount() {
        return getLoan().getAmount();
    }

    @Override
    public BigDecimal getRemainingPrincipal() {
        return investment.getRemainingPrincipal();
    }

    @Override
    public String toString() {
        return "Wrapper for loan #" + investment.getLoanId() + ", investment #" + investment.getId();
    }
}
