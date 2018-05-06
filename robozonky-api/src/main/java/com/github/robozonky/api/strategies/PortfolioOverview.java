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
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.github.robozonky.api.remote.entities.OverallPortfolio;
import com.github.robozonky.api.remote.entities.RiskPortfolio;
import com.github.robozonky.api.remote.entities.Statistics;
import com.github.robozonky.api.remote.enums.Rating;

/**
 * Class with some aggregate statistics about user's portfolio. Used primarily as the main input into
 * {@link InvestmentStrategy}.
 */
public class PortfolioOverview {

    private final int czkAvailable, czkInvested, czkAtRisk;
    private final Map<Rating, Integer> czkInvestedPerRating, czkAtRiskPerRating;

    private PortfolioOverview(final BigDecimal czkAvailable, final Map<Rating, Integer> czkInvestedPerRating,
                              final Map<Rating, Integer> czkAtRiskPerRating) {
        this.czkAvailable = czkAvailable.intValue();
        this.czkInvested = PortfolioOverview.sum(czkInvestedPerRating.values());
        if (this.czkInvested == 0) {
            this.czkInvestedPerRating = Collections.emptyMap();
            this.czkAtRiskPerRating = Collections.emptyMap();
            this.czkAtRisk = 0;
        } else {
            this.czkInvestedPerRating = czkInvestedPerRating;
            this.czkAtRisk = PortfolioOverview.sum(czkAtRiskPerRating.values());
            this.czkAtRiskPerRating = czkAtRisk == 0 ? Collections.emptyMap() : czkAtRiskPerRating;
        }
    }

    private static int sum(final Collection<Integer> vals) {
        return vals.stream().mapToInt(i -> i).sum();
    }

    public static PortfolioOverview calculate(final BigDecimal balance, final Statistics statistics,
                                              final Map<Rating, BigDecimal> adjustments,
                                              final Map<Rating, BigDecimal> atRisk) {
        final Map<Rating, Integer> amounts = statistics.getRiskPortfolio().stream()
                .collect(Collectors.toMap(RiskPortfolio::getRating, OverallPortfolio::getUnpaid));
        adjustments.forEach((r, v) -> amounts.put(r, amounts.getOrDefault(r, 0) + v.intValue()));
        final Map<Rating, Integer> amountsAtRisk = new EnumMap<>(Rating.class);
        atRisk.forEach((r, v) -> amountsAtRisk.put(r, v.intValue()));
        return calculate(balance, amounts, amountsAtRisk);
    }

    public static PortfolioOverview calculate(final BigDecimal balance, final Map<Rating, Integer> amounts) {
        return calculate(balance, amounts, Collections.emptyMap());
    }

    static PortfolioOverview calculate(final BigDecimal balance, final Map<Rating, Integer> amounts,
                                       final Map<Rating, Integer> atRiskAmounts) {
        return new PortfolioOverview(balance, amounts, atRiskAmounts);
    }

    private static BigDecimal divide(final BigDecimal a, final BigDecimal b) {
        return a.divide(b, 4, RoundingMode.HALF_EVEN).stripTrailingZeros();
    }

    /**
     * Available balance in the wallet.
     * @return Amount in CZK.
     */
    public int getCzkAvailable() {
        return this.czkAvailable;
    }

    /**
     * Sum total of all amounts yet unpaid.
     * @return Amount in CZK.
     */
    public int getCzkInvested() {
        return this.czkInvested;
    }

    /**
     * Amount yet unpaid in a given rating.
     * @param r Rating in question.
     * @return Amount in CZK.
     */
    public int getCzkInvested(final Rating r) {
        return this.czkInvestedPerRating.getOrDefault(r, 0);
    }

    /**
     * Sum total of all remaining principal where loans are currently overdue.
     * @return Amount in CZK.
     */
    public int getCzkAtRisk() {
        return this.czkAtRisk;
    }

    /**
     * Sum total of all remaining principal where loans in a given rating are currently overdue.
     * @param r Rating in question.
     * @return Amount in CZK.
     */
    public int getCzkAtRisk(final Rating r) {
        return this.czkAtRiskPerRating.getOrDefault(r, 0);
    }

    /**
     * Retrieve the amounts due in a given rating, divided by {@link #getCzkInvested()}.
     * @param r Rating in question.
     * @return Share of the given rating on overall investments.
     */
    public BigDecimal getShareOnInvestment(final Rating r) {
        final int investedPerRating = this.getCzkInvested(r);
        if (investedPerRating == 0) {
            return BigDecimal.ZERO;
        }
        final BigDecimal invested = BigDecimal.valueOf(this.czkInvested);
        return divide(BigDecimal.valueOf(investedPerRating), invested);
    }

    /**
     * Retrieve the amounts due in a given rating, divided by {@link #getCzkInvested()}.
     * @param r Rating in question.
     * @return Share of the given rating on overall investments.
     */
    public BigDecimal getAtRiskShareOnInvestment(final Rating r) {
        final int investedPerRating = this.getCzkInvested(r);
        if (investedPerRating == 0) {
            return BigDecimal.ZERO;
        }
        final BigDecimal atRisk = BigDecimal.valueOf(getCzkAtRisk(r));
        return divide(atRisk, BigDecimal.valueOf(investedPerRating));
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
