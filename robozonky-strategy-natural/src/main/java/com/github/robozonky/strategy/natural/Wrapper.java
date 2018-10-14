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
import java.util.function.Supplier;

import com.github.robozonky.api.remote.entities.Participation;
import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.entities.sanitized.MarketplaceLoan;
import com.github.robozonky.api.remote.enums.MainIncomeType;
import com.github.robozonky.api.remote.enums.Purpose;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.remote.enums.Region;

public class Wrapper {

    private final Supplier<MarketplaceLoan> loan;
    private final String identifier;
    private final int remainingTermInMonths, originalTermInMonths;
    private final BigDecimal remainingAmount;
    private final boolean insuranceActive;

    private static String identify(final int loanId, final String suffix) {
        final String prefix = "Loan #" + loanId;
        return suffix == null ? prefix : prefix + " (" + suffix + ")";
    }

    public Wrapper(final MarketplaceLoan loan) {
        this.loan = () -> loan;
        this.identifier = identify(loan.getId(), null);
        this.remainingTermInMonths = loan.getTermInMonths();
        this.originalTermInMonths = loan.getTermInMonths();
        this.remainingAmount = null;
        this.insuranceActive = loan.isInsuranceActive();
    }

    /**
     *
     * @param participation
     * @param loan This has the form of a supplier. Allows for the loan to only be retrieved when actually needed, which
     * may, in turn, cause some expensive remote operations on background.
     */
    public Wrapper(final Participation participation, final Supplier<MarketplaceLoan> loan) {
        this.loan = loan;
        this.identifier = identify(participation.getLoanId(), "participation #" + participation.getId());
        this.remainingTermInMonths = participation.getRemainingInstalmentCount();
        this.originalTermInMonths = participation.getOriginalInstalmentCount();
        this.remainingAmount = participation.getRemainingPrincipal();
        this.insuranceActive = participation.isInsuranceActive();
    }

    /**
     *
     * @param investment
     * @param loan This has the form of a supplier. Allows for the loan to only be retrieved when actually needed, which
     * may, in turn, cause some expensive remote operations on background.
     */
    public Wrapper(final Investment investment, final Supplier<MarketplaceLoan> loan) {
        this.loan = loan;
        this.identifier = identify(investment.getLoanId(), "investment #" + investment.getId());
        this.remainingTermInMonths = investment.getRemainingMonths();
        this.originalTermInMonths = investment.getOriginalTerm();
        this.remainingAmount = investment.getRemainingPrincipal();
        this.insuranceActive = investment.isInsuranceActive();
    }

    public boolean isInsuranceActive() {
        return insuranceActive;
    }

    private MarketplaceLoan getLoan() {
        return loan.get();
    }

    public int getLoanId() {
        return getLoan().getId();
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
        return getLoan().getInterestRate();
    }

    public Purpose getPurpose() {
        return getLoan().getPurpose();
    }

    public Rating getRating() {
        return getLoan().getRating();
    }

    public int getOriginalTermInMonths() {
        return originalTermInMonths;
    }

    public int getRemainingTermInMonths() {
        return remainingTermInMonths;
    }

    public int getOriginalAmount() {
        return getLoan().getAmount();
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
        if (o == null || !Objects.equals(getClass(), o.getClass())) {
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
