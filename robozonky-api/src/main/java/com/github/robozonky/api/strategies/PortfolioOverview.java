/*
 * Copyright 2017 The RoboZonky Project
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
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.github.robozonky.api.remote.entities.RiskPortfolio;
import com.github.robozonky.api.remote.entities.Statistics;
import com.github.robozonky.api.remote.enums.Rating;

import static com.github.robozonky.internal.util.BigDecimalCalculator.divide;

/**
 * Class with some aggregate statistics about user's portfolio. Used primarily as the main input into
 * {@link InvestmentStrategy}.
 */
public class PortfolioOverview {

    private final BigDecimal czkAvailable;
    private final BigDecimal czkInvested;
    private final BigDecimal czkAtRisk;
    private final Map<Rating, BigDecimal> czkInvestedPerRating;
    private final Map<Rating, BigDecimal> czkAtRiskPerRating;

    private PortfolioOverview(final BigDecimal czkAvailable, final Map<Rating, BigDecimal> czkInvestedPerRating,
                              final Map<Rating, BigDecimal> czkAtRiskPerRating) {
        this.czkAvailable = czkAvailable;
        this.czkInvested = PortfolioOverview.sum(czkInvestedPerRating.values());
        if (isZero(this.czkInvested)) {
            this.czkInvestedPerRating = Collections.emptyMap();
            this.czkAtRiskPerRating = Collections.emptyMap();
            this.czkAtRisk = BigDecimal.ZERO;
        } else {
            this.czkInvestedPerRating = czkInvestedPerRating;
            this.czkAtRisk = PortfolioOverview.sum(czkAtRiskPerRating.values());
            this.czkAtRiskPerRating = isZero(czkAtRisk) ? Collections.emptyMap() : czkAtRiskPerRating;
        }
    }

    private static boolean isZero(final BigDecimal bigDecimal) {
        return bigDecimal.compareTo(BigDecimal.ZERO) == 0;
    }

    private static BigDecimal sum(final Collection<BigDecimal> vals) {
        return vals.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal sum(final RiskPortfolio portfolio) {
        return portfolio.getDue().add(portfolio.getUnpaid());
    }

    public static PortfolioOverview calculate(final BigDecimal balance, final Statistics statistics,
                                              final Map<Rating, BigDecimal> adjustments,
                                              final Map<Rating, BigDecimal> atRisk) {
        final Map<Rating, BigDecimal> amounts = statistics.getRiskPortfolio().stream()
                .collect(Collectors.toMap(RiskPortfolio::getRating, PortfolioOverview::sum));
        adjustments.forEach((r, v) -> amounts.put(r, amounts.getOrDefault(r, BigDecimal.ZERO).add(v)));
        final Map<Rating, BigDecimal> amountsAtRisk = new EnumMap<>(Rating.class);
        atRisk.forEach(amountsAtRisk::put);
        return calculate(balance, amounts, amountsAtRisk);
    }

    public static PortfolioOverview calculate(final BigDecimal balance, final Map<Rating, BigDecimal> amounts) {
        return calculate(balance, amounts, Collections.emptyMap());
    }

    static PortfolioOverview calculate(final BigDecimal balance, final Map<Rating, BigDecimal> amounts,
                                       final Map<Rating, BigDecimal> atRiskAmounts) {
        return new PortfolioOverview(balance, amounts, atRiskAmounts);
    }

    /**
     * Available balance in the wallet.
     * @return Amount in CZK.
     */
    public BigDecimal getCzkAvailable() {
        return this.czkAvailable;
    }

    /**
     * Sum total of all amounts yet unpaid.
     * @return Amount in CZK.
     */
    public BigDecimal getCzkInvested() {
        return this.czkInvested;
    }

    /**
     * Amount yet unpaid in a given rating.
     * @param r Rating in question.
     * @return Amount in CZK.
     */
    public BigDecimal getCzkInvested(final Rating r) {
        return this.czkInvestedPerRating.getOrDefault(r, BigDecimal.ZERO);
    }

    /**
     * Sum total of all remaining principal where loans are currently overdue.
     * @return Amount in CZK.
     */
    public BigDecimal getCzkAtRisk() {
        return this.czkAtRisk;
    }

    /**
     * How much is at risk out of the entire portfolio, in relative terms.
     * @return Percentage.
     */
    public BigDecimal getShareAtRisk() {
        if (isZero(czkInvested)) { // protected against division by zero
            return BigDecimal.ZERO;
        }
        return divide(czkAtRisk, czkInvested);
    }

    /**
     * Sum total of all remaining principal where loans in a given rating are currently overdue.
     * @param r Rating in question.
     * @return Amount in CZK.
     */
    public BigDecimal getCzkAtRisk(final Rating r) {
        return this.czkAtRiskPerRating.getOrDefault(r, BigDecimal.ZERO);
    }

    /**
     * Retrieve the amounts due in a given rating, divided by {@link #getCzkInvested()}.
     * @param r Rating in question.
     * @return Share of the given rating on overall investments.
     */
    public BigDecimal getShareOnInvestment(final Rating r) {
        if (isZero(czkInvested)) { // protected against division by zero
            return BigDecimal.ZERO;
        }
        final BigDecimal investedPerRating = this.getCzkInvested(r);
        return divide(investedPerRating, czkInvested);
    }

    /**
     * Retrieve the amounts due in a given rating, divided by {@link #getCzkInvested()}.
     * @param r Rating in question.
     * @return Share of the given rating on overall investments.
     */
    public BigDecimal getAtRiskShareOnInvestment(final Rating r) {
        final BigDecimal investedPerRating = this.getCzkInvested(r);
        if (isZero(investedPerRating)) { // protected against division by zero
            return BigDecimal.ZERO;
        }
        return divide(getCzkAtRisk(r), investedPerRating);
    }

    @Override
    public String toString() {
        return "PortfolioOverview{" +
                "czkAtRisk=" + czkAtRisk +
                ", czkAtRiskPerRating=" + czkAtRiskPerRating +
                ", czkAvailable=" + czkAvailable +
                ", czkInvested=" + czkInvested +
                ", czkInvestedPerRating=" + czkInvestedPerRating +
                '}';
    }
}
