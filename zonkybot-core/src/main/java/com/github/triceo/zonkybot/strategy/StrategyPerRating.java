/*
 *
 *  * Copyright 2016 Lukáš Petrovický
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 * /
 */
package com.github.triceo.zonkybot.strategy;

import java.math.BigDecimal;

import com.github.triceo.zonkybot.remote.Loan;
import com.github.triceo.zonkybot.remote.Rating;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class StrategyPerRating {

    private static final Logger LOGGER = LoggerFactory.getLogger(StrategyPerRating.class);

    private final boolean preferLongerTerms;
    private final Rating rating;
    private final BigDecimal targetShare, maximumInvestmentShare;
    private final int minimumAcceptableTerm, maximumAcceptableTerm, maximumInvestmentAmount;

    public StrategyPerRating(final Rating rating, final BigDecimal targetShare, final int minTerm,
                             final int maxTerm, final int maxLoanAmount, final BigDecimal maxLoanShare,
                             final boolean preferLongerTerms) {
        this.rating = rating;
        this.minimumAcceptableTerm = minTerm < 0 ? 0 : minTerm;
        this.maximumAcceptableTerm = maxTerm < 0 ? Integer.MAX_VALUE : maxTerm;
        this.targetShare = targetShare;
        this.maximumInvestmentAmount = maxLoanAmount;
        this.maximumInvestmentShare = maxLoanShare;
        this.preferLongerTerms = preferLongerTerms;
    }

    public boolean isPreferLongerTerms() {
        return preferLongerTerms;
    }

    public Rating getRating() {
        return rating;
    }

    public BigDecimal getTargetShare() {
        return targetShare;
    }

    public boolean isAcceptableTerm(final Loan loan) {
        return loan.getTermInMonths() >= minimumAcceptableTerm && loan.getTermInMonths() <= maximumAcceptableTerm;
    }

    public boolean isAcceptable(final Loan loan) {
        if (loan.getRating() != rating) {
            throw new IllegalStateException("Loan " + loan + " should never have gotten here.");
        } else if (!isAcceptableTerm(loan)) {
            StrategyPerRating.LOGGER.debug("Loan '{}' rejected; strategy looking for loans with terms in range <{}, {}>.", loan,
                    minimumAcceptableTerm, maximumAcceptableTerm);
            return false;
        }
        return true;
    }

    public int recommendInvestmentAmount(final Loan loan) {
        if (loan.getRating() != rating) {
            throw new IllegalStateException("Loan " + loan + " should never have gotten here.");
        }
        final int maximumInvestmentByShare =
                BigDecimal.valueOf(loan.getAmount()).multiply(this.maximumInvestmentShare).intValue();
        return Math.min(maximumInvestmentByShare, maximumInvestmentAmount);

    }
}
