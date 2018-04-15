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

package com.github.robozonky.api.strategies;

import java.math.BigDecimal;
import java.util.Objects;

import com.github.robozonky.api.confirmations.ConfirmationProvider;
import com.github.robozonky.api.remote.entities.RawLoan;
import com.github.robozonky.api.remote.entities.sanitized.MarketplaceLoan;

/**
 * Represents the decision of the {@link InvestmentStrategy} to recommend a {@link RawLoan} for investing.
 */
public final class RecommendedLoan implements Recommended<RecommendedLoan, LoanDescriptor, MarketplaceLoan> {

    private final LoanDescriptor loanDescriptor;
    private final int recommendedInvestmentAmount;
    private final boolean confirmationRequired;

    RecommendedLoan(final LoanDescriptor loanDescriptor, final int amount, final boolean confirmationRequired) {
        if (loanDescriptor == null) {
            throw new IllegalArgumentException("Loan descriptor must not be null.");
        }
        this.loanDescriptor = loanDescriptor;
        this.recommendedInvestmentAmount = amount;
        this.confirmationRequired = confirmationRequired;
    }

    /**
     * Whether or not a {@link ConfirmationProvider} is required to confirm the decision.
     * @return True if required.
     */
    public boolean isConfirmationRequired() {
        return confirmationRequired;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof RecommendedLoan)) {
            return false;
        }
        final RecommendedLoan that = (RecommendedLoan) o;
        return recommendedInvestmentAmount == that.recommendedInvestmentAmount &&
                confirmationRequired == that.confirmationRequired &&
                Objects.equals(loanDescriptor, that.loanDescriptor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(loanDescriptor, recommendedInvestmentAmount, confirmationRequired);
    }

    @Override
    public String toString() {
        return "Recommendation{" +
                "loanDescriptor=" + loanDescriptor +
                ", recommendedInvestmentAmount=" + recommendedInvestmentAmount +
                ", confirmationRequired=" + confirmationRequired +
                '}';
    }

    @Override
    public LoanDescriptor descriptor() {
        return loanDescriptor;
    }

    @Override
    public BigDecimal amount() {
        return BigDecimal.valueOf(recommendedInvestmentAmount);
    }
}
