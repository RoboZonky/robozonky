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

import com.github.robozonky.api.Money;
import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.api.remote.entities.Restrictions;
import com.github.robozonky.api.remote.enums.Rating;

import java.math.BigDecimal;
import java.util.function.BiFunction;

import static com.github.robozonky.internal.util.BigDecimalCalculator.divide;
import static com.github.robozonky.internal.util.BigDecimalCalculator.times;
import static com.github.robozonky.strategy.natural.Audit.LOGGER;

class InvestmentSizeRecommender implements BiFunction<Loan, Restrictions, Money> {

    private static final BigDecimal HUNDRED = BigDecimal.TEN.pow(2);
    private final ParsedStrategy strategy;

    public InvestmentSizeRecommender(final ParsedStrategy strategy) {
        this.strategy = strategy;
    }

    private static Money roundToNearestIncrement(final Money number, final Money increment) {
        final double value = number.getValue().doubleValue();
        final double incr = increment.getValue().doubleValue();
        return Money.from((int)(value / incr) * incr);
    }

    private static Money getPercentage(final Money original, final int percentage) {
        if (percentage == 0) {
            return original.getZero();
        }
        final BigDecimal result = divide(times(original.getValue(), percentage), HUNDRED);
        return Money.from(result);
    }

    private Money[] getInvestmentBounds(final ParsedStrategy strategy, final Loan loan, final Restrictions restrictions) {
        final Rating rating = loan.getRating();
        final Money absoluteMinimum = strategy.getMinimumInvestmentSize(rating).max(restrictions.getMinimumInvestmentAmount());
        final Money minimumRecommendation = roundToNearestIncrement(absoluteMinimum, restrictions.getInvestmentStep());
        final Money maximumUserRecommendation = roundToNearestIncrement(strategy.getMaximumInvestmentSize(rating),
                restrictions.getInvestmentStep());
        final Money maximumInvestmentAmount = restrictions.getMaximumInvestmentAmount();
        if (maximumUserRecommendation.compareTo(maximumInvestmentAmount) > 0) {
            LOGGER.info("Maximum investment amount reduced to {} by Zonky.", maximumInvestmentAmount);
        }
        final Money maximumRecommendation = maximumUserRecommendation.min(maximumInvestmentAmount);
        final int loanId = loan.getId();
        LOGGER.trace("Strategy gives investment range for loan #{} of <{}; {}> CZK.", loanId, minimumRecommendation,
                     maximumRecommendation);
        final Money minimumInvestmentByShare =
                getPercentage(loan.getAmount(), strategy.getMinimumInvestmentShareInPercent());
        final Money minimumInvestment =
                minimumInvestmentByShare.max(strategy.getMinimumInvestmentSize(loan.getRating()));
        final Money maximumInvestmentByShare =
                getPercentage(loan.getAmount(), strategy.getMaximumInvestmentShareInPercent());
        final Money maximumInvestment =
                maximumInvestmentByShare.min(strategy.getMaximumInvestmentSize(loan.getRating()));
        // minimums are guaranteed to be <= maximums due to the contract of strategy implementation
        return new Money[]{minimumInvestment, maximumInvestment};
    }

    @Override
    public Money apply(final Loan loan, final Restrictions restrictions) {
        final int id = loan.getId();
        final Money[] recommended = getInvestmentBounds(strategy, loan, restrictions);
        final Money minimumRecommendation = recommended[0];
        final Money maximumRecommendation = recommended[1];
        LOGGER.debug("Recommended investment range for loan #{} is <{}; {}>.", id, minimumRecommendation,
                     maximumRecommendation);
        // round to nearest lower increment
        final Money loanRemaining = loan.getNonReservedRemainingInvestment();
        final Money zero = loanRemaining.getZero();
        if (minimumRecommendation.compareTo(loanRemaining) > 0) {
            LOGGER.debug("Not recommending loan #{} due to minimum over remaining.", id);
            return zero;
        }
        final Money recommendedAmount = maximumRecommendation.min(loanRemaining);
        final Money r = roundToNearestIncrement(recommendedAmount, restrictions.getInvestmentStep());
        if (r.compareTo(minimumRecommendation) < 0) {
            LOGGER.debug("Not recommending loan #{} due to recommendation below minimum.", id);
            return zero;
        } else {
            LOGGER.debug("Final recommendation for loan #{} is {}.", id, r);
            return r;
        }
    }
}
