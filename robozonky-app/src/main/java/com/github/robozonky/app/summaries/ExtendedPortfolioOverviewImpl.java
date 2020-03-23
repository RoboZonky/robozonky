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

package com.github.robozonky.app.summaries;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Map;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.Ratio;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.ExtendedPortfolioOverview;
import com.github.robozonky.api.strategies.PortfolioOverview;

final class ExtendedPortfolioOverviewImpl implements ExtendedPortfolioOverview {

    private final PortfolioOverview parent;
    private final Money atRisk;
    private final Money sellable;
    private final Money sellableFeeless;
    private final Map<Rating, Money> atRiskPerRating;
    private final Map<Rating, Money> sellablePerRating;
    private final Map<Rating, Money> sellableFeelessPerRating;

    ExtendedPortfolioOverviewImpl(final PortfolioOverview parent,
            final Map<Rating, Money> atRiskPerRating,
            final Map<Rating, Money> sellablePerRating,
            final Map<Rating, Money> sellableFeelessPerRating) {
        this.parent = parent;
        this.sellable = Money.sum(sellablePerRating.values());
        this.sellableFeeless = Money.sum(sellableFeelessPerRating.values());
        if (parent.getInvested()
            .isZero()) {
            this.atRiskPerRating = Collections.emptyMap();
            this.sellablePerRating = Collections.emptyMap();
            this.sellableFeelessPerRating = Collections.emptyMap();
            this.atRisk = parent.getInvested()
                .getZero();
        } else {
            this.atRisk = Money.sum(atRiskPerRating.values());
            this.atRiskPerRating = atRisk.isZero() ? Collections.emptyMap() : atRiskPerRating;
            this.sellablePerRating = sellable.isZero() ? Collections.emptyMap() : sellablePerRating;
            this.sellableFeelessPerRating = sellableFeeless.isZero() ? Collections.emptyMap()
                    : sellableFeelessPerRating;
        }
    }

    public static ExtendedPortfolioOverview extend(final PortfolioOverview parent,
            final Map<Rating, Money> atRiskPerRating,
            final Map<Rating, Money> sellablePerRating,
            final Map<Rating, Money> sellableFeelessPerRating) {
        return new ExtendedPortfolioOverviewImpl(parent, atRiskPerRating, sellablePerRating,
                sellableFeelessPerRating);
    }

    @Override
    public Money getInvested() {
        return parent.getInvested();
    }

    @Override
    public Money getInvested(final Rating r) {
        return parent.getInvested(r);
    }

    @Override
    public Money getAtRisk() {
        return this.atRisk;
    }

    @Override
    public Money getAtRisk(final Rating r) {
        return this.atRiskPerRating.getOrDefault(r, atRisk.getZero());
    }

    @Override
    public Ratio getShareOnInvestment(final Rating r) {
        return parent.getShareOnInvestment(r);
    }

    @Override
    public Money getSellable() {
        return sellable;
    }

    @Override
    public Money getSellable(final Rating r) {
        return sellablePerRating.getOrDefault(r, sellable.getZero());
    }

    @Override
    public Money getSellableFeeless() {
        return sellableFeeless;
    }

    @Override
    public Money getSellableFeeless(final Rating r) {
        return sellableFeelessPerRating.getOrDefault(r, sellable.getZero());
    }

    @Override
    public Ratio getAnnualProfitability() {
        return parent.getAnnualProfitability();
    }

    @Override
    public ZonedDateTime getTimestamp() {
        return parent.getTimestamp();
    }

    @Override
    public String toString() {
        return "ExtendedPortfolioOverviewImpl{" +
                "czkAtRisk=" + atRisk +
                ", czkAtRiskPerRating=" + atRiskPerRating +
                ", czkSellable=" + sellable +
                ", czkSellableFeeless=" + sellableFeeless +
                ", czkSellableFeelessPerRating=" + sellableFeelessPerRating +
                ", czkSellablePerRating=" + sellablePerRating +
                ", parent=" + parent +
                '}';
    }
}
