/*
 * Copyright 2017 Lukáš Petrovický
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

package com.github.triceo.robozonky.app.management;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.triceo.robozonky.api.notifications.StrategyCompletedEvent;
import com.github.triceo.robozonky.api.notifications.StrategyStartedEvent;
import com.github.triceo.robozonky.api.remote.enums.Rating;
import com.github.triceo.robozonky.api.strategies.PortfolioOverview;

public class Portfolio implements PortfolioMBean {

    private static Comparator<Rating> COMPARATOR =
            Comparator.comparing(Rating::getExpectedYield).thenComparing(Rating::getCode);

    private static Stream<Rating> getRatingStream() {
        return Stream.of(Rating.values()).sorted(Portfolio.COMPARATOR);
    }

    private PortfolioOverview latestPortfolioOverview;
    private OffsetDateTime latestUpdatedDateTime;

    private <T> T get(final Function<PortfolioOverview, T> getter, final T defaultValue) {
        if (latestPortfolioOverview == null) {
            return defaultValue;
        } else {
            return getter.apply(latestPortfolioOverview);
        }
    }

    public Portfolio() {
        this.reset();
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
    public Map<String, Integer> getInvestedAmountPerRating() {
        return Portfolio.getRatingStream()
                .collect(Collectors.toMap(Rating::getCode,
                        r -> get(p -> p.getCzkInvested(r), 0), (r1, r2) -> r2, LinkedHashMap::new));
    }

    @Override
    public Map<String, BigDecimal> getRatingShare() {
        return Portfolio.getRatingStream()
                .collect(Collectors.toMap(Rating::getCode,
                        r -> get(p -> p.getShareOnInvestment(r), BigDecimal.ZERO), (r1, r2) -> r2, LinkedHashMap::new));
    }

    @Override
    public int getExpectedYield() {
        return get(PortfolioOverview::getCzkExpectedYield, 0);
    }

    @Override
    public BigDecimal getRelativeExpectedYield() {
        return get(PortfolioOverview::getRelativeExpectedYield, BigDecimal.ZERO);
    }

    @Override
    public OffsetDateTime getLatestUpdatedDateTime() {
        return this.latestUpdatedDateTime;
    }

    @Override
    public void reset() {
        this.latestPortfolioOverview = null;
        this.latestUpdatedDateTime = null;
    }

    void setPortfolioOverview(final StrategyStartedEvent event) {
        this.latestPortfolioOverview = event.getPortfolioOverview();
        this.latestUpdatedDateTime = event.getCreatedOn();
    }

    void setPortfolioOverview(final StrategyCompletedEvent event) {
        this.latestPortfolioOverview = event.getPortfolioOverview();
        this.latestUpdatedDateTime = event.getCreatedOn();
    }

}
