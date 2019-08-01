/*
 * Copyright 2019 The RoboZonky Project
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

import com.github.robozonky.api.remote.entities.RawLoan;
import com.github.robozonky.api.remote.entities.sanitized.MarketplaceLoan;

/**
 * Represents the decision of the {@link InvestmentStrategy} to recommend a {@link RawLoan} for investing.
 */
public final class RecommendedLoan implements Recommended<RecommendedLoan, LoanDescriptor, MarketplaceLoan> {

    private final LoanDescriptor loanDescriptor;
    private final int recommendedInvestmentAmount;

    RecommendedLoan(final LoanDescriptor loanDescriptor, final int amount) {
        if (loanDescriptor == null) {
            throw new IllegalArgumentException("Loan descriptor must not be null.");
        }
        this.loanDescriptor = loanDescriptor;
        this.recommendedInvestmentAmount = amount;
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
                Objects.equals(loanDescriptor, that.loanDescriptor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(loanDescriptor, recommendedInvestmentAmount);
    }

    @Override
    public String toString() {
        return "Recommendation{" +
                "loanDescriptor=" + loanDescriptor +
                ", recommendedInvestmentAmount=" + recommendedInvestmentAmount +
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
