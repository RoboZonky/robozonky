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

import com.github.robozonky.api.Money;
import com.github.robozonky.api.Ratio;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.internal.test.DateUtil;
import com.github.robozonky.internal.util.BigDecimalCalculator;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;

import static com.github.robozonky.api.Money.ZERO;
import static com.github.robozonky.api.Money.from;
import static com.github.robozonky.internal.util.BigDecimalCalculator.times;

final class PortfolioOverviewImpl implements PortfolioOverview {

    private final ZonedDateTime timestamp = DateUtil.zonedNow();
    private final Ratio profitability;
    private final Money invested;
    private final Map<Rating, Money> investedPerRating;

    PortfolioOverviewImpl(final RemotePortfolioImpl impl) {
        this(impl.getTotal(), impl.getRemotePortfolio().getStatistics().getProfitability().orElse(Ratio.ZERO));
    }

    PortfolioOverviewImpl(final Map<Rating, Money> investedPerRating, final Ratio profitability) {
        this.profitability = profitability;
        this.invested = Money.sum(investedPerRating.values());
        if (invested.isZero()) {
            this.investedPerRating = Collections.emptyMap();
        } else {
            this.investedPerRating = investedPerRating;
        }
    }

    @Override
    public Money getInvested() {
        return this.invested;
    }

    @Override
    public Money getInvested(final Rating r) {
        return this.investedPerRating.getOrDefault(r, ZERO);
    }

    @Override
    public Ratio getShareOnInvestment(final Rating r) {
        if (invested.isZero()) { // protected against division by zero
            return Ratio.ZERO;
        }
        final Money investedPerRating = this.getInvested(r);
        return Ratio.fromRaw(investedPerRating.divideBy(invested).getValue());
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
        return getProfitability(r -> r.getMinimalRevenueRate(getInvested()));
    }

    @Override
    public Ratio getOptimalAnnualProfitability() {
        return getProfitability(r -> r.getMaximalRevenueRate(getInvested()));
    }

    @Override
    public Money getMonthlyProfit() {
        return getInvested().multiplyBy(from(profitability.bigDecimalValue())).divideBy(from(12));
    }

    @Override
    public Money getMinimalMonthlyProfit() {
        return getInvested().multiplyBy(from(getMinimalAnnualProfitability().bigDecimalValue())).divideBy(from(12));
    }

    @Override
    public Money getOptimalMonthlyProfit() {
        return getInvested().multiplyBy(from(getOptimalAnnualProfitability().bigDecimalValue())).divideBy(from(12));
    }

    @Override
    public ZonedDateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "PortfolioOverviewImpl{" +
                "czkInvested=" + invested +
                ", czkInvestedPerRating=" + investedPerRating +
                ", profitability=" + profitability +
                ", timestamp=" + timestamp +
                '}';
    }
}
