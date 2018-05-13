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
import com.github.robozonky.api.remote.enums.MainIncomeType;
import com.github.robozonky.api.remote.enums.Purpose;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.remote.enums.Region;

public class InvestmentBasedWrapper implements Wrapper {

    private final Investment investment;
    private final String identifier;

    public InvestmentBasedWrapper(final Investment investment) {
        this.investment = investment;
        this.identifier = "Loan #" + investment.getLoanId() + " (investment #" + investment.getId() + ")";
    }

    public boolean isInsuranceActive() {
        return investment.isInsuranceActive();
    }

    public int getLoanId() {
        return investment.getLoanId();
    }

    @Deprecated
    public Region getRegion() {
        return null;
    }

    @Deprecated
    public String getStory() {
        return null;
    }

    @Deprecated
    public MainIncomeType getMainIncomeType() {
        return null;
    }

    public BigDecimal getInterestRate() {
        return investment.getInterestRate();
    }

    @Deprecated
    public Purpose getPurpose() {
        return null;
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
        return investment.getOriginalPrincipal().intValue();
    }

    public BigDecimal getRemainingAmount() {
        return investment.getRemainingPrincipal();
    }

    public String getIdentifier() {
        return identifier;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !Objects.equals(getClass(), o.getClass())) {
            return false;
        }
        final InvestmentBasedWrapper wrapper = (InvestmentBasedWrapper) o;
        return Objects.equals(identifier, wrapper.identifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier);
    }
}
