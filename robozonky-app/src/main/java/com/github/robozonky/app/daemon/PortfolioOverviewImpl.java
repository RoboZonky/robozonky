/*
 * Copyright 2018 The RoboZonky Project
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

package com.github.robozonky.app.daemon;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.github.robozonky.api.remote.entities.OverallPortfolio;
import com.github.robozonky.api.remote.entities.RiskPortfolio;
import com.github.robozonky.api.remote.entities.Statistics;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.PortfolioOverview;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.github.robozonky.internal.util.BigDecimalCalculator.divide;

final class PortfolioOverviewImpl implements PortfolioOverview {

    private static Logger LOGGER = LoggerFactory.getLogger(PortfolioOverviewImpl.class);

    private final Supplier<BigDecimal> czkAvailable;
    private final BigDecimal czkInvested;
    private final BigDecimal czkAtRisk;
    private final Map<Rating, BigDecimal> czkInvestedPerRating;
    private final Map<Rating, BigDecimal> czkAtRiskPerRating;

    private PortfolioOverviewImpl(final Supplier<BigDecimal> czkAvailable,
                                  final Map<Rating, BigDecimal> czkInvestedPerRating,
                                  final Map<Rating, BigDecimal> czkAtRiskPerRating) {
        this.czkAvailable = czkAvailable;
        this.czkInvested = sum(czkInvestedPerRating.values());
        if (isZero(this.czkInvested)) {
            this.czkInvestedPerRating = Collections.emptyMap();
            this.czkAtRiskPerRating = Collections.emptyMap();
            this.czkAtRisk = BigDecimal.ZERO;
        } else {
            this.czkInvestedPerRating = czkInvestedPerRating;
            this.czkAtRisk = PortfolioOverviewImpl.sum(czkAtRiskPerRating.values());
            this.czkAtRiskPerRating = isZero(czkAtRisk) ? Collections.emptyMap() : czkAtRiskPerRating;
        }
    }

    private static boolean isZero(final BigDecimal bigDecimal) {
        return bigDecimal.compareTo(BigDecimal.ZERO) == 0;
    }

    private static BigDecimal sum(final Collection<BigDecimal> vals) {
        return vals.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal sum(final OverallPortfolio portfolio) {
        return portfolio.getDue().add(portfolio.getUnpaid());
    }

    public static PortfolioOverviewImpl calculate(final Supplier<BigDecimal> balance, final Statistics statistics,
                                                  final Map<Rating, BigDecimal> adjustments,
                                                  final Map<Rating, BigDecimal> atRisk) {
        LOGGER.debug("Risk portfolio from Zonky: {}.", statistics.getRiskPortfolio());
        LOGGER.debug("Adjustments: {}.", adjustments);
        LOGGER.debug("At risk: {}.", atRisk);
        final Map<Rating, BigDecimal> amounts = statistics.getRiskPortfolio().stream()
                .collect(Collectors.toMap(RiskPortfolio::getRating,
                                          PortfolioOverviewImpl::sum,
                                          BigDecimal::add, // should not be necessary
                                          () -> new EnumMap<>(Rating.class)));
        adjustments.forEach((r, v) -> amounts.put(r, amounts.getOrDefault(r, BigDecimal.ZERO).add(v)));
        final Map<Rating, BigDecimal> amountsAtRisk = new EnumMap<>(Rating.class);
        amountsAtRisk.putAll(atRisk);
        return calculate(balance, amounts, amountsAtRisk);
    }

    private static PortfolioOverviewImpl calculate(final Supplier<BigDecimal> balance,
                                                   final Map<Rating, BigDecimal> amounts,
                                                   final Map<Rating, BigDecimal> atRiskAmounts) {
        return new PortfolioOverviewImpl(balance, amounts, atRiskAmounts);
    }

    @Override
    public BigDecimal getCzkAvailable() {
        return this.czkAvailable.get();
    }

    @Override
    public BigDecimal getCzkInvested() {
        return this.czkInvested;
    }

    @Override
    public BigDecimal getCzkInvested(final Rating r) {
        return this.czkInvestedPerRating.getOrDefault(r, BigDecimal.ZERO);
    }

    @Override
    public BigDecimal getCzkAtRisk() {
        return this.czkAtRisk;
    }

    @Override
    public BigDecimal getShareAtRisk() {
        if (isZero(czkInvested)) { // protected against division by zero
            return BigDecimal.ZERO;
        }
        return divide(czkAtRisk, czkInvested);
    }

    @Override
    public BigDecimal getCzkAtRisk(final Rating r) {
        return this.czkAtRiskPerRating.getOrDefault(r, BigDecimal.ZERO);
    }

    @Override
    public BigDecimal getShareOnInvestment(final Rating r) {
        if (isZero(czkInvested)) { // protected against division by zero
            return BigDecimal.ZERO;
        }
        final BigDecimal investedPerRating = this.getCzkInvested(r);
        return divide(investedPerRating, czkInvested);
    }

    @Override
    public BigDecimal getAtRiskShareOnInvestment(final Rating r) {
        final BigDecimal investedPerRating = this.getCzkInvested(r);
        if (isZero(investedPerRating)) { // protected against division by zero
            return BigDecimal.ZERO;
        }
        return divide(getCzkAtRisk(r), investedPerRating);
    }

    @Override
    public String toString() {
        return "PortfolioOverviewImpl{" +
                "czkAvailable=" + czkAvailable.get() +
                ", czkInvested=" + czkInvested +
                ", czkInvestedPerRating=" + czkInvestedPerRating +
                ", czkAtRisk=" + czkAtRisk +
                ", czkAtRiskPerRating=" + czkAtRiskPerRating +
                '}';
    }
}
