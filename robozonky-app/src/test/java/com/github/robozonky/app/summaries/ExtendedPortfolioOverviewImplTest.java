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

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.*;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.Ratio;
import com.github.robozonky.api.notifications.ExtendedPortfolioOverview;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.test.AbstractRoboZonkyTest;

class ExtendedPortfolioOverviewImplTest extends AbstractRoboZonkyTest {

    @Test
    void delegating() {
        final PortfolioOverview portfolioOverview = mockPortfolioOverview();
        final ExtendedPortfolioOverview tested = ExtendedPortfolioOverviewImpl.extend(portfolioOverview,
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap());
        assertSoftly(softly -> {
            softly.assertThat(tested.getInvested())
                .isEqualTo(Money.ZERO);
            softly.assertThat(tested.getInvested(Rating.A))
                .isEqualTo(Money.ZERO);
            softly.assertThat(tested.getShareOnInvestment(Rating.A))
                .isEqualTo(Ratio.ZERO);
            softly.assertThat(tested.getMinimalMonthlyProfit())
                .isEqualTo(Money.ZERO);
            softly.assertThat(tested.getMinimalAnnualProfitability())
                .isEqualTo(Ratio.ZERO);
            softly.assertThat(tested.getOptimalMonthlyProfit())
                .isEqualTo(Money.ZERO);
            softly.assertThat(tested.getOptimalAnnualProfitability())
                .isEqualTo(Ratio.ZERO);
            softly.assertThat(tested.getMonthlyProfit())
                .isEqualTo(Money.ZERO);
            softly.assertThat(tested.getAnnualProfitability())
                .isEqualTo(Ratio.fromPercentage("5"));
            softly.assertThat(tested.getTimestamp())
                .isEqualTo(portfolioOverview.getTimestamp());
        });
    }

    @Test
    void risk() {
        final PortfolioOverview portfolioOverview = mockPortfolioOverview();
        when(portfolioOverview.getInvested(eq(Rating.A))).thenReturn(Money.from(10));
        when(portfolioOverview.getInvested()).thenReturn(Money.from(10));
        final ExtendedPortfolioOverview tested = ExtendedPortfolioOverviewImpl.extend(portfolioOverview,
                Collections.singletonMap(Rating.A, Money.from(1)),
                Collections.emptyMap(),
                Collections.emptyMap());
        assertSoftly(softly -> {
            softly.assertThat(tested.getAtRisk())
                .isEqualTo(Money.from(1));
            softly.assertThat(tested.getAtRisk(Rating.A))
                .isEqualTo(Money.from(1));
            softly.assertThat(tested.getAtRisk(Rating.B))
                .isEqualTo(Money.ZERO);
            softly.assertThat(tested.getShareAtRisk())
                .isEqualTo(Ratio.fromPercentage(10));
            softly.assertThat(tested.getAtRiskShareOnInvestment(Rating.A))
                .isEqualTo(Ratio.fromPercentage(10));
            softly.assertThat(tested.getAtRiskShareOnInvestment(Rating.B))
                .isEqualTo(Ratio.ZERO);
        });
    }

    @Test
    void sellable() {
        final PortfolioOverview portfolioOverview = mockPortfolioOverview();
        when(portfolioOverview.getInvested(eq(Rating.A))).thenReturn(Money.from(10));
        when(portfolioOverview.getInvested()).thenReturn(Money.from(10));
        final ExtendedPortfolioOverview tested = ExtendedPortfolioOverviewImpl.extend(portfolioOverview,
                Collections.emptyMap(),
                Collections.singletonMap(Rating.A, Money.from(1)),
                Collections.emptyMap());
        assertSoftly(softly -> {
            softly.assertThat(tested.getSellable())
                .isEqualTo(Money.from(1));
            softly.assertThat(tested.getSellable(Rating.A))
                .isEqualTo(Money.from(1));
            softly.assertThat(tested.getSellable(Rating.B))
                .isEqualTo(Money.ZERO);
            softly.assertThat(tested.getShareSellable())
                .isEqualTo(Ratio.fromPercentage(10));
            softly.assertThat(tested.getShareSellable(Rating.A))
                .isEqualTo(Ratio.fromPercentage(10));
            softly.assertThat(tested.getShareSellable(Rating.B))
                .isEqualTo(Ratio.ZERO);
        });
    }

    @Test
    void sellableFeeless() {
        final PortfolioOverview portfolioOverview = mockPortfolioOverview();
        when(portfolioOverview.getInvested(eq(Rating.A))).thenReturn(Money.from(10));
        when(portfolioOverview.getInvested()).thenReturn(Money.from(10));
        final ExtendedPortfolioOverview tested = ExtendedPortfolioOverviewImpl.extend(portfolioOverview,
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.singletonMap(Rating.A, Money.from(1)));
        assertSoftly(softly -> {
            softly.assertThat(tested.getSellableFeeless())
                .isEqualTo(Money.from(1));
            softly.assertThat(tested.getSellableFeeless(Rating.A))
                .isEqualTo(Money.from(1));
            softly.assertThat(tested.getSellableFeeless(Rating.B))
                .isEqualTo(Money.ZERO);
            softly.assertThat(tested.getShareSellableFeeless())
                .isEqualTo(Ratio.fromPercentage(10));
            softly.assertThat(tested.getShareSellableFeeless(Rating.A))
                .isEqualTo(Ratio.fromPercentage(10));
            softly.assertThat(tested.getShareSellableFeeless(Rating.B))
                .isEqualTo(Ratio.ZERO);
        });
    }
}
