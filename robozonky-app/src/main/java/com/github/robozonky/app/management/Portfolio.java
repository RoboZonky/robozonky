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

package com.github.robozonky.app.management;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.robozonky.api.notifications.ExecutionCompletedEvent;
import com.github.robozonky.api.notifications.ExecutionStartedEvent;
import com.github.robozonky.api.notifications.PurchasingCompletedEvent;
import com.github.robozonky.api.notifications.PurchasingStartedEvent;
import com.github.robozonky.api.notifications.SellingCompletedEvent;
import com.github.robozonky.api.notifications.SellingStartedEvent;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.PortfolioOverview;

public class Portfolio implements PortfolioMBean {

    private PortfolioOverview latestPortfolioOverview;
    private OffsetDateTime latestUpdatedDateTime;

    private static <T> BinaryOperator<T> throwingMerger() {
        return (u, v) -> {
            throw new IllegalStateException("Impossible.");
        };
    }

    private static Stream<Rating> getRatingStream() {
        return Stream.of(Rating.values()).sorted();
    }

    private static <T> Map<String, T> get(final Function<Rating, T> f) {
        return Portfolio.getRatingStream()
                .collect(Collectors.toMap(Rating::getCode, f, Portfolio.throwingMerger(), LinkedHashMap::new));
    }

    private <T> T get(final Function<PortfolioOverview, T> getter, final T defaultValue) {
        if (latestPortfolioOverview == null) {
            return defaultValue;
        } else {
            return getter.apply(latestPortfolioOverview);
        }
    }

    @Override
    public int getAvailableBalance() {
        return get(PortfolioOverview::getCzkAvailable, 0);
    }

    @Override
    public int getInvestedAmount() {
        return get(PortfolioOverview::getCzkInvested, 0);
    }

    @Override
    public int getAmountAtRisk() {
        return get(PortfolioOverview::getCzkAtRisk, 0);
    }

    @Override
    public Map<String, Integer> getInvestedAmountPerRating() {
        return get(r -> get(p -> p.getCzkInvested(r), 0));
    }

    @Override
    public Map<String, Integer> getAmountAtRiskPerRating() {
        return get(r -> get(p -> p.getCzkAtRisk(r), 0));
    }

    @Override
    public Map<String, BigDecimal> getRatingShare() {
        return get(r -> get(p -> p.getShareOnInvestment(r), BigDecimal.ZERO));
    }

    @Override
    public Map<String, BigDecimal> getShareAtRiskPerRating() {
        return get(r -> get(p -> p.getAtRiskShareOnInvestment(r), BigDecimal.ZERO));
    }

    @Override
    public OffsetDateTime getLatestUpdatedDateTime() {
        return this.latestUpdatedDateTime;
    }

    void handle(final ExecutionStartedEvent event) {
        this.latestPortfolioOverview = event.getPortfolioOverview();
        this.latestUpdatedDateTime = event.getCreatedOn();
    }

    void handle(final ExecutionCompletedEvent event) {
        this.latestPortfolioOverview = event.getPortfolioOverview();
        this.latestUpdatedDateTime = event.getCreatedOn();
    }

    void handle(final SellingStartedEvent event) {
        this.latestPortfolioOverview = event.getPortfolioOverview();
        this.latestUpdatedDateTime = event.getCreatedOn();
    }

    void handle(final SellingCompletedEvent event) {
        this.latestPortfolioOverview = event.getPortfolioOverview();
        this.latestUpdatedDateTime = event.getCreatedOn();
    }

    void handle(final PurchasingStartedEvent event) {
        this.latestPortfolioOverview = event.getPortfolioOverview();
        this.latestUpdatedDateTime = event.getCreatedOn();
    }

    void handle(final PurchasingCompletedEvent event) {
        this.latestPortfolioOverview = event.getPortfolioOverview();
        this.latestUpdatedDateTime = event.getCreatedOn();
    }
}
