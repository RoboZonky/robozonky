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

package com.github.robozonky.app.daemon;

import java.util.Objects;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.api.strategies.InvestmentStrategy;
import com.github.robozonky.api.strategies.LoanDescriptor;

/**
 * Represents the decision of the {@link InvestmentStrategy} to recommend a {@link Loan} for investing.
 */
final class RecommendedLoan implements Recommended<LoanDescriptor, Loan> {

    private final LoanDescriptor loanDescriptor;
    private final Money recommendedInvestment;

    RecommendedLoan(final LoanDescriptor loanDescriptor, final Money amount) {
        if (loanDescriptor == null) {
            throw new IllegalArgumentException("Loan descriptor must not be null.");
        }
        this.loanDescriptor = loanDescriptor;
        this.recommendedInvestment = amount;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof RecommendedLoan)) {
            return false;
        }
        final RecommendedLoan that = (RecommendedLoan) o;
        return Objects.equals(recommendedInvestment, that.recommendedInvestment) &&
                Objects.equals(loanDescriptor, that.loanDescriptor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(loanDescriptor, recommendedInvestment);
    }

    @Override
    public String toString() {
        return "Recommendation{" +
                "loanDescriptor=" + loanDescriptor +
                ", recommendedInvestmentAmount=" + recommendedInvestment +
                '}';
    }

    @Override
    public LoanDescriptor descriptor() {
        return loanDescriptor;
    }

    @Override
    public Money amount() {
        return recommendedInvestment;
    }
}
