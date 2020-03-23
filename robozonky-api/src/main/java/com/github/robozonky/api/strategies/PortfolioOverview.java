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

package com.github.robozonky.api.strategies;

import static com.github.robozonky.internal.util.BigDecimalCalculator.divide;
import static com.github.robozonky.internal.util.BigDecimalCalculator.times;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.function.Function;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.Ratio;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.internal.util.BigDecimalCalculator;

/**
 * Class with some aggregate statistics about user's portfolio. Used primarily as the main input into
 * {@link InvestmentStrategy}.
 */
public interface PortfolioOverview {

    /**
     * Sum total of all amounts yet unpaid.
     * 
     * @return Amount.
     */
    Money getInvested();

    /**
     * Amount yet unpaid in a given rating.
     * 
     * @param r Rating in question.
     * @return Amount.
     */
    Money getInvested(final Rating r);

    /**
     * Retrieve the amounts due in a given rating, divided by {@link #getInvested()}.
     * 
     * @param r Rating in question.
     * @return Share of the given rating on overall investments.
     */
    default Ratio getShareOnInvestment(final Rating r) {
        Money total = getInvested();
        if (total.isZero()) {
            return Ratio.ZERO;
        }
        Money investedInRating = getInvested(r);
        BigDecimal ratio = divide(investedInRating.getValue(), total.getValue());
        return Ratio.fromRaw(ratio);
    }

    /**
     * Retrieve annual rate of return of the entire portfolio as reported by Zonky.
     * 
     * @return
     */
    Ratio getAnnualProfitability();

    private BigDecimal calculateProfitability(final Rating r, final Function<Rating, Ratio> metric) {
        final Ratio ratingShare = getShareOnInvestment(r);
        final Ratio ratingProfitability = metric.apply(r);
        return times(ratingShare.bigDecimalValue(), ratingProfitability.bigDecimalValue());
    }

    private Ratio getProfitability(final Function<Rating, Ratio> metric) {
        final BigDecimal result = Arrays.stream(Rating.values())
            .map(r -> calculateProfitability(r, metric))
            .reduce(BigDecimalCalculator::plus)
            .orElse(BigDecimal.ZERO);
        return Ratio.fromRaw(result);
    }

    /**
     * Retrieve minimal annual rate of return of the entire portfolio, assuming Zonky rist cost model holds.
     * (See {@link Rating#getMinimalRevenueRate(Money)}.)
     * 
     * @return
     */
    default Ratio getMinimalAnnualProfitability() {
        return getProfitability(r -> r.getMinimalRevenueRate(getInvested()));
    }

    /**
     * Retrieve maximal annual rate of return of the entire portfolio, assuming none of the loans are ever delinquent.
     * (See {@link Rating#getMaximalRevenueRate(Money)}.)
     * 
     * @return
     */
    default Ratio getOptimalAnnualProfitability() {
        return getProfitability(r -> r.getMaximalRevenueRate(getInvested()));
    }

    private Money calculateProfit(Ratio rate) {
        return rate.apply(getInvested())
            .divideBy(12);
    }

    /**
     * Retrieve the expected monthly revenue, based on {@link #getAnnualProfitability()}.
     * 
     * @return Amount.
     */
    default Money getMonthlyProfit() {
        return calculateProfit(getAnnualProfitability());
    }

    /**
     * Retrieve the expected monthly revenue, based on {@link #getMinimalAnnualProfitability()}.
     * 
     * @return Amount.
     */
    default Money getMinimalMonthlyProfit() {
        return calculateProfit(getMinimalAnnualProfitability());
    }

    /**
     * Retrieve the expected monthly revenue, based on {@link #getOptimalAnnualProfitability()}.
     * 
     * @return Amount.
     */
    default Money getOptimalMonthlyProfit() {
        return calculateProfit(getOptimalAnnualProfitability());
    }

    /**
     * @return When this instance was created.
     */
    ZonedDateTime getTimestamp();
}
