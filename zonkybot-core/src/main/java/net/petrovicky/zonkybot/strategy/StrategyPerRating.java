/*
 * Copyright 2016 Lukáš Petrovický
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.petrovicky.zonkybot.strategy;

import java.math.BigDecimal;

import net.petrovicky.zonkybot.remote.Loan;
import net.petrovicky.zonkybot.remote.Rating;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class StrategyPerRating {

    private static final Logger LOGGER = LoggerFactory.getLogger(StrategyPerRating.class);

    private final boolean preferLongerTerms;
    private final Rating rating;
    private final BigDecimal targetShare;
    private final int minimumAcceptableTerm, maximumAcceptableTerm, minimumInvestmentAmount, maximumInvestmentAmount;

    public StrategyPerRating(final Rating rating, final BigDecimal targetShare, final int minTerm,
                             final int maxTerm, final int minAmount, final int maxAmount,
                             final boolean preferLongerTerms) {
        this.rating = rating;
        minimumAcceptableTerm = minTerm < 0 ? 0 : minTerm;
        maximumAcceptableTerm = maxTerm < 0 ? Integer.MAX_VALUE : maxTerm;
        this.targetShare = targetShare;
        minimumInvestmentAmount = minAmount;
        maximumInvestmentAmount = maxAmount;
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

    public int getMinimumInvestmentAmount() {
        return minimumInvestmentAmount;
    }

    public int getMaximumInvestmentAmount() {
        return maximumInvestmentAmount;
    }

    public boolean isAcceptableTerm(final Loan loan) {
        return loan.getTermInMonths() >= minimumAcceptableTerm && loan.getTermInMonths() <= maximumAcceptableTerm;
    }

    public boolean isAcceptableAmount(final Loan loan) {
        return loan.getRemainingInvestment() >= minimumInvestmentAmount;
    }

    public boolean isAcceptable(final Loan loan) {
        if (loan.getRating() != rating) {
            throw new IllegalStateException("Loan " + loan + " should never have gotten here.");
        } else if (!isAcceptableTerm(loan)) {
            StrategyPerRating.LOGGER.debug("Loan '{}' rejected; strategy looking for loans with terms in range <{}, {}>.", loan,
                    minimumAcceptableTerm, maximumAcceptableTerm);
            return false;
        } else if (!isAcceptableAmount(loan)) {
            StrategyPerRating.LOGGER.debug("Loan '{}' rejected; strategy looking for minimum investment of {} CZK.", loan,
                    minimumInvestmentAmount);
            return false;
        }
        return true;
    }
}
