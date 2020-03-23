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

package com.github.robozonky.app.tenant;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.Ratio;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.test.AbstractRoboZonkyTest;

class PortfolioOverviewImplTest extends AbstractRoboZonkyTest {

    @Test
    void equality() {
        PortfolioOverview po = new PortfolioOverviewImpl(Collections.emptyMap(), Ratio.ZERO);
        assertThat(po)
            .isEqualTo(po)
            .isNotEqualTo(null)
            .isNotEqualTo("");
        PortfolioOverview po2 = new PortfolioOverviewImpl(Collections.emptyMap(), Ratio.ZERO);
        assertThat(po).isEqualTo(po2);
        assertThat(po2).isEqualTo(po);
        resetClock();
        PortfolioOverview po3 = new PortfolioOverviewImpl(Map.of(Rating.A, Money.from(100)), Ratio.ZERO);
        assertThat(po3)
            .isNotEqualTo(po)
            .isNotEqualTo(po2);
    }

    @Test
    void timestamp() {
        final PortfolioOverview po = new PortfolioOverviewImpl(Collections.emptyMap(), Ratio.ZERO);
        assertThat(po.getTimestamp()).isBeforeOrEqualTo(ZonedDateTime.now());
    }

    @Test
    void emptyPortfolio() {
        final PortfolioOverview po = new PortfolioOverviewImpl(Collections.emptyMap(), Ratio.ZERO);
        assertSoftly(softly -> {
            softly.assertThat(po.getInvested())
                .isEqualTo(Money.ZERO);
            for (final Rating r : Rating.values()) {
                softly.assertThat(po.getInvested(r))
                    .as(r + " invested")
                    .isEqualTo(Money.ZERO);
                softly.assertThat(po.getShareOnInvestment(r))
                    .as(r + " as a share")
                    .isEqualTo(Ratio.ZERO);
            }
        });
    }

    @Test
    void profitability() {
        final Map<Rating, Money> investments = new EnumMap<>(Rating.class);
        investments.put(Rating.AAAAA, Money.from(200_000));
        investments.put(Rating.D, Money.from(20_000));
        final PortfolioOverview po = new PortfolioOverviewImpl(investments, Ratio.fromPercentage(4));
        assertSoftly(softly -> {
            // the values tested against have been calculated manually and are guaranteed correct
            softly.assertThat(po.getAnnualProfitability())
                .isEqualTo(Ratio.fromPercentage(4));
            softly.assertThat(po.getMinimalAnnualProfitability()
                .asPercentage()
                .doubleValue())
                .isCloseTo(3.72, within(0.01));
            softly.assertThat(po.getOptimalAnnualProfitability()
                .asPercentage()
                .doubleValue())
                .isCloseTo(4.87, within(0.01));
            softly.assertThat(po.getMonthlyProfit()
                .getValue()
                .intValue())
                .isCloseTo(733, within(1));
            softly.assertThat(po.getMinimalMonthlyProfit()
                .getValue()
                .intValue())
                .isCloseTo(682, within(1));
            softly.assertThat(po.getOptimalMonthlyProfit()
                .getValue()
                .intValue())
                .isCloseTo(893, within(1));
        });
    }

    @Test
    void emptyPortfolioWithAdjustmentsAndRisks() {
        final Money adj = Money.from(10);
        final Map<Rating, Money> in = Collections.singletonMap(Rating.D, adj);
        final PortfolioOverview po = new PortfolioOverviewImpl(in, Ratio.ZERO);
        assertSoftly(softly -> {
            softly.assertThat(po.getInvested())
                .isEqualTo(adj);
            for (final Rating r : Rating.values()) {
                final Money expectedAbsolute = r == Rating.D ? adj : Money.ZERO;
                final Money expectedRelative = r == Rating.D ? Money.from(1) : Money.ZERO;
                softly.assertThat(po.getInvested(r))
                    .as(r + " invested")
                    .isEqualTo(expectedAbsolute);
                softly.assertThat(po.getShareOnInvestment(r))
                    .as(r + " as a share")
                    .isEqualTo(Ratio.fromRaw(expectedRelative.getValue()));
            }
        });
    }
}
