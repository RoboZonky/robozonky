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

package com.github.robozonky.app.tenant;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

import com.github.robozonky.api.Ratio;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.test.AbstractRoboZonkyTest;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

import static com.github.robozonky.internal.util.BigDecimalCalculator.divide;
import static org.assertj.core.api.Assertions.*;

class PortfolioOverviewImplTest extends AbstractRoboZonkyTest {

    @Test
    void timestamp() {
        final PortfolioOverview po = new PortfolioOverviewImpl(BigDecimal.TEN, Collections.emptyMap(),
                                                               Collections.emptyMap(), Ratio.ZERO);
        assertThat(po.getTimestamp()).isBeforeOrEqualTo(ZonedDateTime.now());
    }

    @Test
    void emptyPortfolio() {
        final BigDecimal balance = BigDecimal.TEN;
        final PortfolioOverview po = new PortfolioOverviewImpl(balance, Collections.emptyMap(), Collections.emptyMap(),
                                                               Ratio.ZERO);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(po.getCzkAvailable()).isEqualTo(balance);
            softly.assertThat(po.getCzkInvested()).isEqualTo(BigDecimal.ZERO);
            softly.assertThat(po.getCzkAtRisk()).isEqualTo(BigDecimal.ZERO);
            softly.assertThat(po.getShareAtRisk()).isEqualTo(Ratio.ZERO);
            for (final Rating r : Rating.values()) {
                softly.assertThat(po.getCzkInvested(r)).as(r + " invested").isEqualTo(BigDecimal.ZERO);
                softly.assertThat(po.getCzkAtRisk(r)).as(r + " at risk").isEqualTo(BigDecimal.ZERO);
                softly.assertThat(po.getShareOnInvestment(r))
                        .as(r + " as a share")
                        .isEqualTo(Ratio.ZERO);
                softly.assertThat(po.getAtRiskShareOnInvestment(r))
                        .as(r + " at risk as a share")
                        .isEqualTo(Ratio.ZERO);
            }
        });
    }

    @Test
    void profitability() {
        final BigDecimal balance = BigDecimal.TEN;
        final Map<Rating, BigDecimal> investments = new EnumMap<>(Rating.class);
        investments.put(Rating.AAAAA, BigDecimal.valueOf(200_000));
        investments.put(Rating.D, BigDecimal.valueOf(20_000));
        final PortfolioOverview po = new PortfolioOverviewImpl(balance, investments, Collections.emptyMap(),
                                                               Ratio.fromPercentage(4));
        SoftAssertions.assertSoftly(softly -> {
            // the values tested against have been calculated manually and are guaranteed correct
            softly.assertThat(po.getAnnualProfitability()).isEqualTo(Ratio.fromPercentage(4));
            softly.assertThat(po.getMinimalAnnualProfitability().asPercentage().doubleValue())
                    .isCloseTo(3.72, within(0.01));
            softly.assertThat(po.getOptimalAnnualProfitability().asPercentage().doubleValue())
                    .isCloseTo(4.87, within(0.01));
            softly.assertThat(po.getCzkMonthlyProfit().intValue()).isCloseTo(733, within(1));
            softly.assertThat(po.getCzkMinimalMonthlyProfit().intValue()).isCloseTo(682, within(1));
            softly.assertThat(po.getCzkOptimalMonthyProfit().intValue()).isCloseTo(893, within(1));
        });
    }

    @Test
    void emptyPortfolioWithAdjustmentsAndRisks() {
        final BigDecimal adj = BigDecimal.TEN;
        final Map<Rating, BigDecimal> in = Collections.singletonMap(Rating.D, adj);
        final PortfolioOverview po = new PortfolioOverviewImpl(BigDecimal.ZERO, in, in, Ratio.ZERO);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(po.getCzkAvailable()).isEqualTo(BigDecimal.ZERO);
            softly.assertThat(po.getCzkInvested()).isEqualTo(adj);
            softly.assertThat(po.getCzkAtRisk()).isEqualTo(adj);
            final BigDecimal share = divide(po.getCzkAtRisk(), po.getCzkInvested());
            softly.assertThat(po.getShareAtRisk()).isEqualTo(Ratio.fromRaw(share));
            for (final Rating r : Rating.values()) {
                final BigDecimal expectedAbsolute = r == Rating.D ? adj : BigDecimal.ZERO;
                final BigDecimal expectedRelative = r == Rating.D ? BigDecimal.ONE : BigDecimal.ZERO;
                softly.assertThat(po.getCzkInvested(r)).as(r + " invested").isEqualTo(expectedAbsolute);
                softly.assertThat(po.getCzkAtRisk(r)).as(r + " at risk").isEqualTo(expectedAbsolute);
                softly.assertThat(po.getShareOnInvestment(r))
                        .as(r + " as a share")
                        .isEqualTo(Ratio.fromRaw(expectedRelative));
                softly.assertThat(po.getAtRiskShareOnInvestment(r))
                        .as(r + " at risk as a share")
                        .isEqualTo(Ratio.fromRaw(expectedRelative));
            }
        });
    }
}
