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
import java.util.Collections;
import java.util.stream.Stream;

import com.github.triceo.robozonky.api.notifications.StrategyStartedEvent;
import com.github.triceo.robozonky.api.remote.enums.Rating;
import com.github.triceo.robozonky.api.strategies.PortfolioOverview;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public class PortfolioTest {

    @Test
    public void run() {
        final PortfolioOverview portfolio = Mockito.mock(PortfolioOverview.class);
        Mockito.when(portfolio.getCzkAvailable()).thenReturn(1000);
        Mockito.when(portfolio.getCzkInvested()).thenReturn(10000);
        Mockito.when(portfolio.getRelativeExpectedYield()).thenReturn(BigDecimal.ONE);
        Mockito.when(portfolio.getCzkExpectedYield()).thenReturn(10000);
        Mockito.when(portfolio.getShareOnInvestment(ArgumentMatchers.any())).thenReturn(BigDecimal.ONE);
        Mockito.when(portfolio.getCzkInvested(ArgumentMatchers.any())).thenReturn(1000);
        final Portfolio mbean = new Portfolio();
        final StrategyStartedEvent evt =
                new StrategyStartedEvent(null, Collections.emptyList(), portfolio);
        mbean.setPortfolioOverview(evt);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(mbean.getAvailableBalance()).isEqualTo(portfolio.getCzkAvailable());
            softly.assertThat(mbean.getExpectedYield()).isEqualTo(portfolio.getCzkExpectedYield());
            softly.assertThat(mbean.getRelativeExpectedYield()).isEqualTo(portfolio.getRelativeExpectedYield());
            softly.assertThat(mbean.getInvestedAmount()).isEqualTo(portfolio.getCzkInvested());
            softly.assertThat(mbean.getLatestUpdatedDateTime()).isBefore(OffsetDateTime.now());
            Stream.of(Rating.values()).forEach(r -> {
                softly.assertThat(mbean.getInvestedAmountPerRating()).containsEntry(r.getCode(), 1000);
                softly.assertThat(mbean.getRatingShare()).containsEntry(r.getCode(), BigDecimal.ONE);
            });
        });
    }
}
