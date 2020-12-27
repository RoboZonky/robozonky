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
import com.github.robozonky.api.notifications.ExtendedPortfolioOverview;
import com.github.robozonky.api.strategies.PortfolioOverview;

final class ExtendedPortfolioOverviewImpl implements ExtendedPortfolioOverview {

    private final PortfolioOverview parent;
    private final Money atRisk;
    private final Money sellable;
    private final Money sellableFeeless;
    private final Map<Ratio, Money> atRiskPerRating;
    private final Map<Ratio, Money> sellablePerRating;
    private final Map<Ratio, Money> sellableFeelessPerRating;

    ExtendedPortfolioOverviewImpl(final PortfolioOverview parent, final Map<Ratio, Money> atRiskPerRating,
            final Map<Ratio, Money> sellablePerRating, final Map<Ratio, Money> sellableFeelessPerRating) {
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
            final Map<Ratio, Money> atRiskPerRating, final Map<Ratio, Money> sellablePerRating,
            final Map<Ratio, Money> sellableFeelessPerRating) {
        return new ExtendedPortfolioOverviewImpl(parent, atRiskPerRating, sellablePerRating,
                sellableFeelessPerRating);
    }

    @Override
    public Money getInvested() {
        return parent.getInvested();
    }

    @Override
    public Money getInvested(final Ratio r) {
        return parent.getInvested(r);
    }

    @Override
    public Money getAtRisk() {
        return this.atRisk;
    }

    @Override
    public Money getAtRisk(final Ratio r) {
        return this.atRiskPerRating.getOrDefault(r, atRisk.getZero());
    }

    @Override
    public Ratio getShareOnInvestment(final Ratio r) {
        return parent.getShareOnInvestment(r);
    }

    @Override
    public Money getSellable() {
        return sellable;
    }

    @Override
    public Money getSellable(final Ratio r) {
        return sellablePerRating.getOrDefault(r, sellable.getZero());
    }

    @Override
    public Money getSellableFeeless() {
        return sellableFeeless;
    }

    @Override
    public Money getSellableFeeless(final Ratio r) {
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
