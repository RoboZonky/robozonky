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

package com.github.robozonky.api.strategies;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.Ratio;
import com.github.robozonky.api.notifications.ExtendedPortfolioOverview;
import com.github.robozonky.api.remote.enums.Rating;

class ExtendedPortfolioOverviewTest {

    private static final ExtendedPortfolioOverview OVERVIEW = new ExtendedPortfolioOverview() {
        @Override
        public Money getInvested() {
            return Money.from(110_000);
        }

        @Override
        public Money getInvested(Rating r) {
            return Money.from(10_000);
        }

        @Override
        public Ratio getAnnualProfitability() {
            return Ratio.fromRaw("0.05");
        }

        @Override
        public ZonedDateTime getTimestamp() {
            return ZonedDateTime.now();
        }

        @Override
        public Money getAtRisk() {
            return Money.from(11_000);
        }

        @Override
        public Money getAtRisk(Rating r) {
            return Money.from(1_000);
        }

        @Override
        public Money getSellable() {
            return Money.from(55_000);
        }

        @Override
        public Money getSellable(Rating r) {
            return Money.from(5_000);
        }

        @Override
        public Money getSellableFeeless() {
            return Money.from(33_000);
        }

        @Override
        public Money getSellableFeeless(Rating r) {
            return Money.from(3_000);
        }

    };

    @Test
    void values() {
        assertSoftly(softly -> {
            softly.assertThat(OVERVIEW.getShareAtRisk())
                .isEqualTo(Ratio.fromRaw("0.10"));
            softly.assertThat(OVERVIEW.getAtRiskShareOnInvestment(Rating.A))
                .isEqualTo(Ratio.fromRaw("0.1"));
            softly.assertThat(OVERVIEW.getShareSellable())
                .isEqualTo(Ratio.fromRaw("0.5"));
            softly.assertThat(OVERVIEW.getShareSellable(Rating.A))
                .isEqualTo(Ratio.fromRaw("0.5"));
            softly.assertThat(OVERVIEW.getShareSellableFeeless())
                .isEqualTo(Ratio.fromRaw("0.3"));
            softly.assertThat(OVERVIEW.getShareSellableFeeless(Rating.A))
                .isEqualTo(Ratio.fromRaw("0.3"));
        });
    }

    @Test
    void zeroTest() {
        final ExtendedPortfolioOverview portfolioOverview = mock(ExtendedPortfolioOverview.class);
        when(portfolioOverview.getInvested()).thenReturn(Money.ZERO);
        when(portfolioOverview.getInvested(any())).thenReturn(Money.ZERO);
        when(portfolioOverview.getShareAtRisk()).thenCallRealMethod();
        when(portfolioOverview.getAtRiskShareOnInvestment(any())).thenCallRealMethod();
        when(portfolioOverview.getShareSellable()).thenCallRealMethod();
        when(portfolioOverview.getShareSellable(any())).thenCallRealMethod();
        when(portfolioOverview.getShareSellableFeeless(any())).thenCallRealMethod();
        when(portfolioOverview.getShareSellableFeeless()).thenCallRealMethod();
        assertSoftly(softly -> {
            softly.assertThat(portfolioOverview.getShareAtRisk())
                .isEqualTo(Ratio.ZERO);
            softly.assertThat(portfolioOverview.getAtRiskShareOnInvestment(Rating.A))
                .isEqualTo(Ratio.ZERO);
            softly.assertThat(portfolioOverview.getShareSellable())
                .isEqualTo(Ratio.ZERO);
            softly.assertThat(portfolioOverview.getShareSellable(Rating.A))
                .isEqualTo(Ratio.ZERO);
            softly.assertThat(portfolioOverview.getShareSellableFeeless())
                .isEqualTo(Ratio.ZERO);
            softly.assertThat(portfolioOverview.getShareSellableFeeless(Rating.A))
                .isEqualTo(Ratio.ZERO);
        });
    }

}
