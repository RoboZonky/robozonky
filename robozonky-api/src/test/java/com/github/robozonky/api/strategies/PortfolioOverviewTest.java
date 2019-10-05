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

package com.github.robozonky.api.strategies;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.Ratio;
import com.github.robozonky.api.remote.enums.Rating;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PortfolioOverviewTest {

    private final PortfolioOverview portfolioOverview = new PortfolioOverview() {
        @Override
        public Money getInvested() {
            return Money.from(100_000);
        }

        @Override
        public Money getInvested(Rating r) {
            return getInvested().divideBy(Rating.values().length);
        }

        @Override
        public Ratio getAnnualProfitability() {
            return Ratio.fromRaw("0.05");
        }

        @Override
        public ZonedDateTime getTimestamp() {
            return ZonedDateTime.now();
        }
    };

    @Test
    void values() {
        assertSoftly(softly -> {
            softly.assertThat(portfolioOverview.getMinimalAnnualProfitability()).isEqualTo(Ratio.fromRaw("0.04692727"));
            softly.assertThat(portfolioOverview.getAnnualProfitability()).isEqualTo(Ratio.fromRaw("0.05"));
            softly.assertThat(portfolioOverview.getOptimalAnnualProfitability()).isEqualTo(Ratio.fromRaw("0.07208182"));
            softly.assertThat(portfolioOverview.getMinimalMonthlyProfit()).isEqualTo(Money.from("391.06"));
            softly.assertThat(portfolioOverview.getMonthlyProfit()).isEqualTo(Money.from("416.67"));
            softly.assertThat(portfolioOverview.getOptimalMonthlyProfit()).isEqualTo(Money.from("600.68"));
            softly.assertThat(portfolioOverview.getShareOnInvestment(Rating.A)).isEqualTo(Ratio.fromRaw("0.09090910"));
        });
    }

    @Test
    void zeroTest() {
        final PortfolioOverview portfolioOverview = mock(PortfolioOverview.class);
        when(portfolioOverview.getInvested()).thenReturn(Money.ZERO);
        when(portfolioOverview.getShareOnInvestment(any())).thenCallRealMethod();
        when(portfolioOverview.getInvested(any())).thenReturn(Money.from(10));
        assertThat(portfolioOverview.getShareOnInvestment(Rating.A)).isEqualTo(Ratio.ZERO);
    }

}
