/*
 * Copyright 2016 Lukáš Petrovický
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
package com.github.triceo.robozonky.strategy;

import java.math.BigDecimal;

import com.github.triceo.robozonky.remote.Loan;
import com.github.triceo.robozonky.remote.Rating;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class StrategyPerRating {

    private static final Logger LOGGER = LoggerFactory.getLogger(StrategyPerRating.class);

    private final boolean preferLongerTerms;
    private final Rating rating;
    private final BigDecimal targetShare, maximumInvestmentShare;
    private final int minimumAcceptableTerm, maximumAcceptableTerm, maximumInvestmentAmount, minimumAskAmount,
            maximumAskAmount;

    StrategyPerRating(final Rating rating, final BigDecimal targetShare, final int minTerm, final int maxTerm,
                             final int maxLoanAmount, final BigDecimal maxLoanShare, final int minAskAmount,
                             final int maxAskAmount, final boolean preferLongerTerms) {
        this.rating = rating;
        this.minimumAcceptableTerm = Math.max(minTerm, 0);
        if (maxTerm == 0) {
            throw new IllegalArgumentException("Maximum acceptable term must not be 0.");
        }
        this.maximumAcceptableTerm = maxTerm < 0 ? Integer.MAX_VALUE : maxTerm;
        if (this.minimumAcceptableTerm > this.maximumAcceptableTerm) {
            throw new IllegalArgumentException("Maximum acceptable term must be greater than or equal to minimum.");
        }
        this.targetShare = targetShare;
        this.maximumInvestmentAmount = maxLoanAmount;
        if (this.maximumInvestmentAmount < 0) {
            throw new IllegalArgumentException("Maximum investment amount must not be negative.");
        }
        this.minimumAskAmount = minAskAmount;
        if (this.minimumAskAmount < 0) {
            throw new IllegalArgumentException("Minimum asked amount must not be negative.");
        }
        this.maximumAskAmount = maxAskAmount < 0 ? Integer.MAX_VALUE : maxAskAmount;
        this.maximumInvestmentShare = maxLoanShare;
        final boolean shareSubZero = this.maximumInvestmentShare.compareTo(BigDecimal.ZERO) < 0;
        final boolean shareOverOne = this.maximumInvestmentShare.compareTo(BigDecimal.ONE) > 0;
        if (shareSubZero || shareOverOne) {
            throw new IllegalArgumentException("Maximum investment share must be in range of <0, 1>.");
        }
        this.preferLongerTerms = preferLongerTerms;
    }

    public boolean isPreferLongerTerms() {
        return this.preferLongerTerms;
    }

    public BigDecimal getTargetShare() {
        return this.targetShare;
    }

    public Rating getRating() {
        return this.rating;
    }

    private boolean isAcceptableTerm(final Loan loan) {
        final int term = loan.getTermInMonths();
        return term >= this.minimumAcceptableTerm && term <= this.maximumAcceptableTerm;
    }

    private boolean isAcceptableAsk(final Loan loan) {
        final int ask = (int)loan.getAmount();
        return ask >= this.minimumAskAmount && ask <= this.maximumAskAmount;
    }

    public boolean isAcceptable(final Loan loan) {
        if (loan.getRating() != this.rating) {
            throw new IllegalArgumentException("Loan " + loan + " should never have gotten here.");
        } else if (!isAcceptableTerm(loan)) {
            StrategyPerRating.LOGGER.debug("Loan '{}' rejected; looking for loans with terms in range <{}, {}>.", loan,
                    this.minimumAcceptableTerm, this.maximumAcceptableTerm);
            return false;
        } else if (!isAcceptableAsk(loan)) {
            StrategyPerRating.LOGGER.debug("Loan '{}' rejected; looking for loans with amounts in range <{}, {}>.",
                    loan, this.minimumAskAmount, this.maximumAskAmount);
            return false;
        }
        return true;
    }

    public int recommendInvestmentAmount(final Loan loan) {
        if (loan.getRating() != this.rating) {
            throw new IllegalArgumentException("Loan " + loan + " should never have gotten here.");
        } else if (!this.isAcceptable(loan)) {
            return 0; // loan is not acceptable
        }
        final int maximumInvestmentByShare =
                BigDecimal.valueOf(loan.getAmount()).multiply(this.maximumInvestmentShare).intValue();
        return Math.min(maximumInvestmentByShare, this.maximumInvestmentAmount);

    }
}
