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

package com.github.robozonky.app.tenant;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.Ratio;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.internal.test.DateUtil;

final class PortfolioOverviewImpl implements PortfolioOverview {

    private final ZonedDateTime timestamp = DateUtil.zonedNow();
    private final Ratio profitability;
    private final Money invested;
    private final Map<Rating, Money> investedPerRating;

    PortfolioOverviewImpl(final RemotePortfolioImpl impl) {
        this(impl.getTotal(), impl.getRemoteData()
            .getStatistics()
            .getProfitability()
            .orElse(Ratio.ZERO));
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
        return this.investedPerRating.getOrDefault(r, invested.getZero());
    }

    @Override
    public Ratio getAnnualProfitability() {
        return profitability;
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

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !Objects.equals(getClass(), o.getClass())) {
            return false;
        }
        final PortfolioOverviewImpl that = (PortfolioOverviewImpl) o;
        return Objects.equals(invested, that.invested) &&
                Objects.equals(investedPerRating, that.investedPerRating);
    }

    @Override
    public int hashCode() {
        return Objects.hash(invested, investedPerRating);
    }
}
