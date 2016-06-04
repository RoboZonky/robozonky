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
    private final int minimumAcceptableTerm, maximumAcceptableTerm, maximumInvestmentAmount;

    public StrategyPerRating(final Rating rating, final BigDecimal targetShare, final int minTerm, final int maxTerm,
                             final int maxLoanAmount, final BigDecimal maxLoanShare, final boolean preferLongerTerms) {
        this.rating = rating;
        this.minimumAcceptableTerm = minTerm < 0 ? 0 : minTerm;
        this.maximumAcceptableTerm = maxTerm < 0 ? Integer.MAX_VALUE : maxTerm;
        this.targetShare = targetShare;
        this.maximumInvestmentAmount = maxLoanAmount;
        this.maximumInvestmentShare = maxLoanShare;
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
        return loan.getTermInMonths() >= this.minimumAcceptableTerm
                && loan.getTermInMonths() <= this.maximumAcceptableTerm;
    }

    public boolean isAcceptable(final Loan loan) {
        if (loan.getRating() != this.rating) {
            throw new IllegalArgumentException("Loan " + loan + " should never have gotten here.");
        } else if (!isAcceptableTerm(loan)) {
            StrategyPerRating.LOGGER.debug("Loan '{}' rejected; looking for loans with terms in range <{}, {}>.", loan,
                    this.minimumAcceptableTerm, this.maximumAcceptableTerm);
            return false;
        }
        return true;
    }

    public int recommendInvestmentAmount(final Loan loan) {
        if (loan.getRating() != this.rating) {
            throw new IllegalArgumentException("Loan " + loan + " should never have gotten here.");
        }
        final int maximumInvestmentByShare =
                BigDecimal.valueOf(loan.getAmount()).multiply(this.maximumInvestmentShare).intValue();
        return Math.min(maximumInvestmentByShare, this.maximumInvestmentAmount);

    }
}
