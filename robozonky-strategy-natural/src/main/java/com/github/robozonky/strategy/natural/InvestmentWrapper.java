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

import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.entities.sanitized.MarketplaceLoan;
import com.github.robozonky.api.remote.enums.MainIncomeType;
import com.github.robozonky.api.remote.enums.Purpose;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.remote.enums.Region;
import com.github.robozonky.api.strategies.InvestmentDescriptor;

final class InvestmentWrapper implements Wrapper<InvestmentDescriptor> {

    private final InvestmentDescriptor original;
    private final Investment investment;

    public InvestmentWrapper(final InvestmentDescriptor original) {
        this.original = original;
        this.investment = original.item();
    }

    public boolean isInsuranceActive() {
        return investment.isInsuranceActive();
    }

    private MarketplaceLoan getLoan() {
        return original.related();
    }

    public int getLoanId() {
        return investment.getLoanId();
    }

    public Region getRegion() {
        return getLoan().getRegion();
    }

    public String getStory() {
        return getLoan().getStory();
    }

    public MainIncomeType getMainIncomeType() {
        return getLoan().getMainIncomeType();
    }

    public BigDecimal getInterestRate() {
        return investment.getInterestRate();
    }

    public Purpose getPurpose() {
        return getLoan().getPurpose();
    }

    public Rating getRating() {
        return investment.getRating();
    }

    public int getOriginalTermInMonths() {
        return investment.getOriginalTerm();
    }

    public int getRemainingTermInMonths() {
        return investment.getRemainingMonths();
    }

    public int getOriginalAmount() {
        return getLoan().getAmount();
    }

    public BigDecimal getRemainingAmount() {
        return investment.getRemainingPrincipal();
    }

    @Override
    public InvestmentDescriptor getOriginal() {
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
        final InvestmentWrapper that = (InvestmentWrapper) o;
        return Objects.equals(original, that.original);
    }

    @Override
    public int hashCode() {
        return Objects.hash(original);
    }

    @Override
    public String toString() {
        return "Wrapper for loan #" + investment.getLoanId() + ", investment #" + investment.getId();
    }
}
