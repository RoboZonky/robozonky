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

package com.github.robozonky.strategy.natural;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.github.robozonky.api.remote.entities.Restrictions;
import com.github.robozonky.api.remote.entities.sanitized.MarketplaceLoan;
import com.github.robozonky.api.remote.enums.Rating;

class InvestmentSizeRecommender {

    private final ParsedStrategy strategy;

    public InvestmentSizeRecommender(final ParsedStrategy strategy) {
        this.strategy = strategy;
    }

    private static int roundToNearestIncrement(final int number, final int increment) {
        return (number / increment) * increment;
    }

    private static int getPercentage(final double original, final int percentage) {
        return BigDecimal.valueOf(original)
                .multiply(BigDecimal.valueOf(percentage))
                .divide(BigDecimal.valueOf(100), RoundingMode.HALF_EVEN)
                .intValue();
    }

    private int[] getInvestmentBounds(final ParsedStrategy strategy, final MarketplaceLoan loan,
                                      final Restrictions restrictions) {
        final Rating rating = loan.getRating();
        final int absoluteMinimum = Math.max(strategy.getMinimumInvestmentSizeInCzk(rating),
                                             restrictions.getMinimumInvestmentAmount());
        final int minimumRecommendation = roundToNearestIncrement(absoluteMinimum, restrictions.getInvestmentStep());
        final int maximumUserRecommendation = roundToNearestIncrement(strategy.getMaximumInvestmentSizeInCzk(rating),
                                                                      restrictions.getInvestmentStep());
        final int maximumInvestmentAmount = restrictions.getMaximumInvestmentAmount();
        if (maximumUserRecommendation > maximumInvestmentAmount) {
            Decisions.report(l -> l.info("Maximum investment amount reduced to {} by Zonky.", maximumInvestmentAmount));
        }
        final int maximumRecommendation = Math.min(maximumUserRecommendation, maximumInvestmentAmount);
        final int loanId = loan.getId();
        Decisions.report(l -> l.trace("Strategy gives investment range for loan #{} of <{}; {}> CZK.", loanId,
                                      minimumRecommendation, maximumRecommendation));
        final int minimumInvestmentByShare =
                getPercentage(loan.getAmount(), strategy.getMinimumInvestmentShareInPercent());
        final int minimumInvestment =
                Math.max(minimumInvestmentByShare, strategy.getMinimumInvestmentSizeInCzk(loan.getRating()));
        final int maximumInvestmentByShare =
                getPercentage(loan.getAmount(), strategy.getMaximumInvestmentShareInPercent());
        final int maximumInvestment =
                Math.min(maximumInvestmentByShare, strategy.getMaximumInvestmentSizeInCzk(loan.getRating()));
        // minimums are guaranteed to be <= maximums due to the contract of strategy implementation
        return new int[]{minimumInvestment, maximumInvestment};
    }

    public Integer apply(final MarketplaceLoan loan, final Integer balance, final Restrictions restrictions) {
        final int id = loan.getId();
        final int[] recommended = getInvestmentBounds(strategy, loan, restrictions);
        final int minimumRecommendation = recommended[0];
        final int maximumRecommendation = recommended[1];
        Decisions.report(l -> l.debug("Recommended investment range for loan #{} is <{}; {}> CZK.", id,
                                      minimumRecommendation, maximumRecommendation));
        // round to nearest lower increment
        final int loanRemaining = loan.getNonReservedRemainingInvestment();
        if (minimumRecommendation > balance) {
            Decisions.report(l -> l.debug("Not recommending loan #{} due to minimum over balance.", id));
            return 0;
        } else if (minimumRecommendation > loanRemaining) {
            Decisions.report(l -> l.debug("Not recommending loan #{} due to minimum over remaining.", id));
            return 0;
        }
        final int recommendedAmount = Math.min(balance, Math.min(maximumRecommendation, loanRemaining));
        final int r = roundToNearestIncrement(recommendedAmount, restrictions.getInvestmentStep());
        if (r < minimumRecommendation) {
            Decisions.report(l -> l.debug("Not recommending loan #{} due to recommendation below minimum.", id));
            return 0;
        } else {
            Decisions.report(l -> l.debug("Final recommendation for loan #{} is {} CZK.", id, r));
            return r;
        }
    }
}
