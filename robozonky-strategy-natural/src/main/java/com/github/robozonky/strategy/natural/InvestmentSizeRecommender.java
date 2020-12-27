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

package com.github.robozonky.strategy.natural;

import static com.github.robozonky.internal.util.BigDecimalCalculator.times;
import static com.github.robozonky.strategy.natural.Audit.LOGGER;

import java.math.BigDecimal;
import java.util.function.BiFunction;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.api.remote.entities.Loan;

class InvestmentSizeRecommender implements BiFunction<Loan, SessionInfo, Money> {

    private final ParsedStrategy strategy;

    public InvestmentSizeRecommender(final ParsedStrategy strategy) {
        this.strategy = strategy;
    }

    private static Money roundToNearestIncrement(final Money number, final Money increment) {
        final double value = number.getValue()
            .doubleValue();
        final double incr = increment.getValue()
            .doubleValue();
        return Money.from((int) (value / incr) * incr);
    }

    private static Money getPercentage(final Money original, final int percentage) {
        if (percentage == 0) {
            return original.getZero();
        }
        final BigDecimal result = times(original.getValue(), percentage).scaleByPowerOfTen(-2);
        return Money.from(result);
    }

    private static Money[] getInvestmentBounds(final ParsedStrategy strategy, final Loan loan,
            final SessionInfo sessionInfo) {
        var rating = loan.getInterestRate();
        var absoluteMinimum = strategy.getMinimumInvestmentSize(rating)
            .max(sessionInfo.getMinimumInvestmentAmount());
        var minimumRecommendation = roundToNearestIncrement(absoluteMinimum, sessionInfo.getInvestmentStep());
        var maximumUserRecommendation = roundToNearestIncrement(strategy.getMaximumInvestmentSize(rating),
                sessionInfo.getInvestmentStep());
        var maximumInvestmentAmount = sessionInfo.getMaximumInvestmentAmount();
        if (maximumUserRecommendation.compareTo(maximumInvestmentAmount) > 0) {
            LOGGER.info("Maximum investment amount reduced to {} by Zonky.", maximumInvestmentAmount);
        }
        var maximumRecommendation = maximumUserRecommendation.min(maximumInvestmentAmount);
        var loanId = loan.getId();
        LOGGER.trace("Strategy gives investment range for loan #{} of <{}; {}>.", loanId, minimumRecommendation,
                maximumRecommendation);
        var minimumInvestmentByShare = getPercentage(loan.getAmount(), strategy.getMinimumInvestmentShareInPercent());
        var minimumInvestment = minimumInvestmentByShare.max(strategy.getMinimumInvestmentSize(rating));
        var maximumInvestmentByShare = getPercentage(loan.getAmount(),
                strategy.getMaximumInvestmentShareInPercent());
        var maximumInvestment = maximumInvestmentByShare.min(strategy.getMaximumInvestmentSize(rating));
        // minimums are guaranteed to be <= maximums due to the contract of strategy implementation
        return new Money[] { minimumInvestment, maximumInvestment };
    }

    @Override
    public Money apply(final Loan loan, final SessionInfo sessionInfo) {
        final int id = loan.getId();
        final Money[] recommended = getInvestmentBounds(strategy, loan, sessionInfo);
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
        final Money r = roundToNearestIncrement(recommendedAmount, sessionInfo.getInvestmentStep());
        if (r.compareTo(minimumRecommendation) < 0) {
            LOGGER.debug("Not recommending loan #{} due to recommendation below minimum.", id);
            return zero;
        } else {
            LOGGER.debug("Final recommendation for loan #{} is {}.", id, r);
            return r;
        }
    }
}
