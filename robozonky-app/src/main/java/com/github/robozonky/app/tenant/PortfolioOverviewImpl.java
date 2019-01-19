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

package com.github.robozonky.app.tenant;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.internal.util.DateUtil;

import static com.github.robozonky.internal.util.BigDecimalCalculator.divide;

final class PortfolioOverviewImpl implements PortfolioOverview {

    private final ZonedDateTime timestamp = DateUtil.zonedNow();
    private final BigDecimal czkAvailable;
    private final BigDecimal czkInvested;
    private final BigDecimal czkAtRisk;
    private final Map<Rating, BigDecimal> czkInvestedPerRating;
    private final Map<Rating, BigDecimal> czkAtRiskPerRating;

    PortfolioOverviewImpl(final BigDecimal czkAvailable, final Map<Rating, BigDecimal> czkInvestedPerRating,
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
    public ZonedDateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "PortfolioOverviewImpl{" +
                "czkAvailable=" + czkAvailable +
                ", czkInvested=" + czkInvested +
                ", czkInvestedPerRating=" + czkInvestedPerRating +
                ", czkAtRisk=" + czkAtRisk +
                ", czkAtRiskPerRating=" + czkAtRiskPerRating +
                ", timestamp=" + timestamp +
                '}';
    }
}
