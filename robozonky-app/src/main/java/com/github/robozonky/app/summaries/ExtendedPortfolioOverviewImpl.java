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

package com.github.robozonky.app.summaries;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import com.github.robozonky.api.Ratio;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.ExtendedPortfolioOverview;
import com.github.robozonky.api.strategies.PortfolioOverview;

import static com.github.robozonky.internal.util.BigDecimalCalculator.divide;
import static com.github.robozonky.internal.util.BigDecimalCalculator.times;

final class ExtendedPortfolioOverviewImpl implements ExtendedPortfolioOverview {

    private final PortfolioOverview parent;
    private final BigDecimal czkAtRisk;
    private final BigDecimal czkSellable;
    private final BigDecimal czkSellableFeeless;
    private final Map<Rating, BigDecimal> czkAtRiskPerRating;
    private final Map<Rating, BigDecimal> czkSellablePerRating;
    private final Map<Rating, BigDecimal> czkSellableFeelessPerRating;

    ExtendedPortfolioOverviewImpl(final PortfolioOverview parent,
                                  final Map<Rating, BigDecimal> czkAtRiskPerRating,
                                  final Map<Rating, BigDecimal> czkSellablePerRating,
                                  final Map<Rating, BigDecimal> czkSellableFeelessPerRating) {
        this.parent = parent;
        this.czkSellable = sum(czkSellablePerRating.values());
        this.czkSellableFeeless = sum(czkSellableFeelessPerRating.values());
        if (isZero(parent.getCzkInvested())) {
            this.czkAtRiskPerRating = Collections.emptyMap();
            this.czkSellablePerRating = Collections.emptyMap();
            this.czkSellableFeelessPerRating = Collections.emptyMap();
            this.czkAtRisk = BigDecimal.ZERO;
        } else {
            this.czkAtRisk = ExtendedPortfolioOverviewImpl.sum(czkAtRiskPerRating.values());
            this.czkAtRiskPerRating = isZero(czkAtRisk) ? Collections.emptyMap() : czkAtRiskPerRating;
            this.czkSellablePerRating = isZero(czkSellable) ? Collections.emptyMap() : czkSellablePerRating;
            this.czkSellableFeelessPerRating =
                    isZero(czkSellableFeeless) ? Collections.emptyMap() : czkSellableFeelessPerRating;
        }
    }

    public static ExtendedPortfolioOverview extend(final PortfolioOverview parent,
                                                   final Map<Rating, BigDecimal> czkAtRiskPerRating,
                                                   final Map<Rating, BigDecimal> czkSellablePerRating,
                                                   final Map<Rating, BigDecimal> czkSellableFeelessPerRating) {
        return new ExtendedPortfolioOverviewImpl(parent, czkAtRiskPerRating, czkSellablePerRating,
                                                 czkSellableFeelessPerRating);
    }

    private static boolean isZero(final BigDecimal bigDecimal) {
        return bigDecimal.compareTo(BigDecimal.ZERO) == 0;
    }

    private static BigDecimal sum(final Collection<BigDecimal> vals) {
        return vals.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public BigDecimal getCzkAvailable() {
        return parent.getCzkAvailable();
    }

    @Override
    public BigDecimal getCzkInvested() {
        return parent.getCzkInvested();
    }

    @Override
    public BigDecimal getCzkInvested(final Rating r) {
        return parent.getCzkInvested(r);
    }

    @Override
    public BigDecimal getCzkAtRisk() {
        return this.czkAtRisk;
    }

    @Override
    public Ratio getShareAtRisk() {
        final BigDecimal czkInvested = getCzkInvested();
        if (isZero(czkInvested)) { // protected against division by zero
            return Ratio.ZERO;
        }
        return Ratio.fromRaw(divide(czkAtRisk, czkInvested));
    }

    @Override
    public BigDecimal getCzkAtRisk(final Rating r) {
        return this.czkAtRiskPerRating.getOrDefault(r, BigDecimal.ZERO);
    }

    @Override
    public Ratio getShareOnInvestment(final Rating r) {
        final BigDecimal czkInvested = getCzkInvested();
        if (isZero(czkInvested)) { // protected against division by zero
            return Ratio.ZERO;
        }
        final BigDecimal investedPerRating = this.getCzkInvested(r);
        return Ratio.fromRaw(divide(investedPerRating, czkInvested));
    }

    @Override
    public Ratio getAtRiskShareOnInvestment(final Rating r) {
        final BigDecimal investedPerRating = this.getCzkInvested(r);
        if (isZero(investedPerRating)) { // protected against division by zero
            return Ratio.ZERO;
        }
        return Ratio.fromRaw(divide(getCzkAtRisk(r), investedPerRating));
    }

    @Override
    public BigDecimal getCzkSellable() {
        return czkSellable;
    }

    @Override
    public Ratio getShareSellable() {
        final BigDecimal czkInvested = getCzkInvested();
        if (isZero(czkInvested)) { // protected against division by zero
            return Ratio.ZERO;
        }
        return Ratio.fromRaw(divide(czkSellable, czkInvested));
    }

    @Override
    public BigDecimal getCzkSellable(final Rating r) {
        return czkSellablePerRating.getOrDefault(r, BigDecimal.ZERO);
    }

    @Override
    public Ratio getShareSellable(final Rating r) {
        final BigDecimal investedPerRating = this.getCzkInvested(r);
        if (isZero(investedPerRating)) { // protected against division by zero
            return Ratio.ZERO;
        }
        return Ratio.fromRaw(divide(getCzkSellable(r), investedPerRating));
    }

    @Override
    public BigDecimal getCzkSellableFeeless() {
        return czkSellableFeeless;
    }

    @Override
    public Ratio getShareSellableFeeless() {
        final BigDecimal czkInvested = getCzkInvested();
        if (isZero(czkInvested)) { // protected against division by zero
            return Ratio.ZERO;
        }
        return Ratio.fromRaw(divide(czkSellableFeeless, czkInvested));
    }

    @Override
    public BigDecimal getCzkSellableFeeless(final Rating r) {
        return czkSellableFeelessPerRating.getOrDefault(r, BigDecimal.ZERO);
    }

    @Override
    public Ratio getShareSellableFeeless(final Rating r) {
        final BigDecimal investedPerRating = this.getCzkInvested(r);
        if (isZero(investedPerRating)) { // protected against division by zero
            return Ratio.ZERO;
        }
        return Ratio.fromRaw(divide(getCzkSellableFeeless(r), investedPerRating));
    }

    @Override
    public Ratio getAnnualProfitability() {
        return parent.getAnnualProfitability();
    }

    @Override
    public Ratio getMinimalAnnualProfitability() {
        return parent.getMinimalAnnualProfitability();
    }

    @Override
    public Ratio getOptimalAnnualProfitability() {
        return parent.getOptimalAnnualProfitability();
    }

    @Override
    public BigDecimal getCzkMonthlyProfit() {
        return parent.getCzkMonthlyProfit();
    }

    @Override
    public BigDecimal getCzkMinimalMonthlyProfit() {
        return parent.getCzkMinimalMonthlyProfit();
    }

    @Override
    public BigDecimal getCzkOptimalMonthyProfit() {
        return divide(times(getOptimalAnnualProfitability().bigDecimalValue(), getCzkInvested()), 12);
    }

    @Override
    public ZonedDateTime getTimestamp() {
        return parent.getTimestamp();
    }

    @Override
    public String toString() {
        return "ExtendedPortfolioOverviewImpl{" +
                "czkAtRisk=" + czkAtRisk +
                ", czkAtRiskPerRating=" + czkAtRiskPerRating +
                ", czkSellable=" + czkSellable +
                ", czkSellableFeeless=" + czkSellableFeeless +
                ", czkSellableFeelessPerRating=" + czkSellableFeelessPerRating +
                ", czkSellablePerRating=" + czkSellablePerRating +
                ", parent=" + parent +
                '}';
    }
}
