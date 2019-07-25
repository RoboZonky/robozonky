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
import java.util.Collections;

import com.github.robozonky.api.Ratio;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.ExtendedPortfolioOverview;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.test.AbstractRoboZonkyTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.*;

class ExtendedPortfolioOverviewImplTest extends AbstractRoboZonkyTest {

    @Test
    void delegating() {
        final PortfolioOverview portfolioOverview = mockPortfolioOverview(10_000);
        final ExtendedPortfolioOverview tested = ExtendedPortfolioOverviewImpl.extend(portfolioOverview,
                                                                                      Collections.emptyMap(),
                                                                                      Collections.emptyMap(),
                                                                                      Collections.emptyMap());
        assertSoftly(softly -> {
            softly.assertThat(tested.getCzkAvailable()).isEqualTo(BigDecimal.valueOf(10_000));
            softly.assertThat(tested.getCzkInvested()).isEqualTo(BigDecimal.ZERO);
            softly.assertThat(tested.getCzkInvested(Rating.A)).isEqualTo(BigDecimal.ZERO);
            softly.assertThat(tested.getShareOnInvestment(Rating.A)).isEqualTo(Ratio.ZERO);
            softly.assertThat(tested.getCzkMinimalMonthlyProfit()).isEqualTo(BigDecimal.ZERO);
            softly.assertThat(tested.getMinimalAnnualProfitability()).isEqualTo(Ratio.ZERO);
            softly.assertThat(tested.getCzkOptimalMonthlyProfit()).isEqualTo(BigDecimal.TEN);
            softly.assertThat(tested.getOptimalAnnualProfitability()).isEqualTo(Ratio.ONE);
            softly.assertThat(tested.getCzkMonthlyProfit()).isEqualTo(BigDecimal.ONE);
            softly.assertThat(tested.getAnnualProfitability()).isEqualTo(Ratio.fromPercentage("5"));
        });
    }

    @Test
    void risk() {
        final PortfolioOverview portfolioOverview = mockPortfolioOverview(10_000);
        when(portfolioOverview.getCzkInvested(eq(Rating.A))).thenReturn(BigDecimal.TEN);
        when(portfolioOverview.getCzkInvested()).thenReturn(BigDecimal.TEN);
        final ExtendedPortfolioOverview tested =
                ExtendedPortfolioOverviewImpl.extend(portfolioOverview,
                                                     Collections.singletonMap(Rating.A, BigDecimal.ONE),
                                                     Collections.emptyMap(),
                                                     Collections.emptyMap());
        assertSoftly(softly -> {
            softly.assertThat(tested.getCzkAtRisk()).isEqualTo(BigDecimal.ONE);
            softly.assertThat(tested.getCzkAtRisk(Rating.A)).isEqualTo(BigDecimal.ONE);
            softly.assertThat(tested.getCzkAtRisk(Rating.B)).isEqualTo(BigDecimal.ZERO);
            softly.assertThat(tested.getShareAtRisk()).isEqualTo(Ratio.fromPercentage(10));
            softly.assertThat(tested.getAtRiskShareOnInvestment(Rating.A)).isEqualTo(Ratio.fromPercentage(10));
            softly.assertThat(tested.getAtRiskShareOnInvestment(Rating.B)).isEqualTo(Ratio.ZERO);
        });
    }

    @Test
    void sellable() {
        final PortfolioOverview portfolioOverview = mockPortfolioOverview(10_000);
        when(portfolioOverview.getCzkInvested(eq(Rating.A))).thenReturn(BigDecimal.TEN);
        when(portfolioOverview.getCzkInvested()).thenReturn(BigDecimal.TEN);
        final ExtendedPortfolioOverview tested =
                ExtendedPortfolioOverviewImpl.extend(portfolioOverview,
                                                     Collections.emptyMap(),
                                                     Collections.singletonMap(Rating.A, BigDecimal.ONE),
                                                     Collections.emptyMap());
        assertSoftly(softly -> {
            softly.assertThat(tested.getCzkSellable()).isEqualTo(BigDecimal.ONE);
            softly.assertThat(tested.getCzkSellable(Rating.A)).isEqualTo(BigDecimal.ONE);
            softly.assertThat(tested.getCzkSellable(Rating.B)).isEqualTo(BigDecimal.ZERO);
            softly.assertThat(tested.getShareSellable()).isEqualTo(Ratio.fromPercentage(10));
            softly.assertThat(tested.getShareSellable(Rating.A)).isEqualTo(Ratio.fromPercentage(10));
            softly.assertThat(tested.getShareSellable(Rating.B)).isEqualTo(Ratio.ZERO);
        });
    }

    @Test
    void sellableFeeless() {
        final PortfolioOverview portfolioOverview = mockPortfolioOverview(10_000);
        when(portfolioOverview.getCzkInvested(eq(Rating.A))).thenReturn(BigDecimal.TEN);
        when(portfolioOverview.getCzkInvested()).thenReturn(BigDecimal.TEN);
        final ExtendedPortfolioOverview tested =
                ExtendedPortfolioOverviewImpl.extend(portfolioOverview,
                                                     Collections.emptyMap(),
                                                     Collections.emptyMap(),
                                                     Collections.singletonMap(Rating.A, BigDecimal.ONE));
        assertSoftly(softly -> {
            softly.assertThat(tested.getCzkSellableFeeless()).isEqualTo(BigDecimal.ONE);
            softly.assertThat(tested.getCzkSellableFeeless(Rating.A)).isEqualTo(BigDecimal.ONE);
            softly.assertThat(tested.getCzkSellableFeeless(Rating.B)).isEqualTo(BigDecimal.ZERO);
            softly.assertThat(tested.getShareSellableFeeless()).isEqualTo(Ratio.fromPercentage(10));
            softly.assertThat(tested.getShareSellableFeeless(Rating.A)).isEqualTo(Ratio.fromPercentage(10));
            softly.assertThat(tested.getShareSellableFeeless(Rating.B)).isEqualTo(Ratio.ZERO);
        });
    }
}
