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

package com.github.robozonky.app.tenant;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;

import com.github.robozonky.api.Ratio;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.internal.test.DateUtil;
import com.github.robozonky.internal.util.BigDecimalCalculator;

import static com.github.robozonky.internal.util.BigDecimalCalculator.divide;
import static com.github.robozonky.internal.util.BigDecimalCalculator.times;

final class PortfolioOverviewImpl implements PortfolioOverview {

    private final ZonedDateTime timestamp = DateUtil.zonedNow();
    private final Ratio profitability;
    private final BigDecimal czkAvailable;
    private final BigDecimal czkInvested;
    private final Map<Rating, BigDecimal> czkInvestedPerRating;

    PortfolioOverviewImpl(final RemotePortfolioImpl impl) {
        this(impl.getBalance(), impl.getTotal(),
             impl.getRemotePortfolio().getStatistics().getProfitability().orElse(Ratio.ZERO));
    }

    PortfolioOverviewImpl(final BigDecimal czkAvailable, final Map<Rating, BigDecimal> czkInvestedPerRating,
                          final Ratio profitability) {
        this.profitability = profitability;
        this.czkAvailable = czkAvailable;
        this.czkInvested = sum(czkInvestedPerRating.values());
        if (isZero(this.czkInvested)) {
            this.czkInvestedPerRating = Collections.emptyMap();
        } else {
            this.czkInvestedPerRating = czkInvestedPerRating;
        }
    }

    private static boolean isZero(final BigDecimal bigDecimal) {
        return bigDecimal.compareTo(BigDecimal.ZERO) == 0;
    }

    private static BigDecimal sum(final Collection<BigDecimal> vals) {
        return vals.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public BigDecimal getCzkAvailable() {
        return this.czkAvailable;
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
    public Ratio getShareOnInvestment(final Rating r) {
        if (isZero(czkInvested)) { // protected against division by zero
            return Ratio.ZERO;
        }
        final BigDecimal investedPerRating = this.getCzkInvested(r);
        return Ratio.fromRaw(divide(investedPerRating, czkInvested));
    }

    @Override
    public Ratio getAnnualProfitability() {
        return profitability;
    }

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

    @Override
    public Ratio getMinimalAnnualProfitability() {
        return getProfitability(r -> r.getMinimalRevenueRate(getCzkInvested().longValue()));
    }

    @Override
    public Ratio getOptimalAnnualProfitability() {
        return getProfitability(r -> r.getMaximalRevenueRate(getCzkInvested().longValue()));
    }

    @Override
    public BigDecimal getCzkMonthlyProfit() {
        return divide(times(profitability.bigDecimalValue(), getCzkInvested()), 12);
    }

    @Override
    public BigDecimal getCzkMinimalMonthlyProfit() {
        return divide(times(getMinimalAnnualProfitability().bigDecimalValue(), getCzkInvested()), 12);
    }

    @Override
    public BigDecimal getCzkOptimalMonthyProfit() {
        return divide(times(getOptimalAnnualProfitability().bigDecimalValue(), getCzkInvested()), 12);
    }

    @Override
    public ZonedDateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "PortfolioOverviewImpl{" +
                "czkAvailable=" + czkAvailable +
                ", czkInvested=" + czkInvested +
                ", czkInvestedPerRating=" + czkInvestedPerRating +
                ", profitability=" + profitability +
                ", timestamp=" + timestamp +
                '}';
    }
}
