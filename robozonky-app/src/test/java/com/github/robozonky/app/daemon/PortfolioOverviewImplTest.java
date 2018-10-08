/*
 * Copyright 2018 The RoboZonky Project
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

package com.github.robozonky.app.daemon;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;

import com.github.robozonky.api.remote.entities.Statistics;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.PortfolioOverview;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

import static com.github.robozonky.internal.util.BigDecimalCalculator.divide;

class PortfolioOverviewImplTest {

    @Test
    void emptyPortfolio() {
        final BigDecimal balance = BigDecimal.TEN;
        final PortfolioOverview po = PortfolioOverviewImpl.calculate(balance, Statistics.empty(),
                                                                     Collections.emptyMap(), Collections.emptyMap());
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(po.getCzkAvailable()).isEqualTo(balance);
            softly.assertThat(po.getCzkInvested()).isEqualTo(BigDecimal.ZERO);
            softly.assertThat(po.getCzkAtRisk()).isEqualTo(BigDecimal.ZERO);
            softly.assertThat(po.getShareAtRisk()).isEqualTo(BigDecimal.ZERO);
            for (final Rating r : Rating.values()) {
                softly.assertThat(po.getCzkInvested(r)).as(r + " invested").isEqualTo(BigDecimal.ZERO);
                softly.assertThat(po.getCzkAtRisk(r)).as(r + " at risk").isEqualTo(BigDecimal.ZERO);
                softly.assertThat(po.getShareOnInvestment(r))
                        .as(r + " as a share")
                        .isEqualTo(BigDecimal.ZERO);
                softly.assertThat(po.getAtRiskShareOnInvestment(r))
                        .as(r + " at risk as a share")
                        .isEqualTo(BigDecimal.ZERO);
            }
        });
    }

    @Test
    void emptyPortfolioWithAdjustmentsAndRisks() {
        final BigDecimal adj = BigDecimal.TEN;
        final Map<Rating, BigDecimal> in = Collections.singletonMap(Rating.D, adj);
        final PortfolioOverview po = PortfolioOverviewImpl.calculate(BigDecimal.ZERO, Statistics.empty(), in, in);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(po.getCzkAvailable()).isEqualTo(BigDecimal.ZERO);
            softly.assertThat(po.getCzkInvested()).isEqualTo(adj);
            softly.assertThat(po.getCzkAtRisk()).isEqualTo(adj);
            final BigDecimal share = divide(po.getCzkAtRisk(), po.getCzkInvested());
            softly.assertThat(po.getShareAtRisk()).isEqualTo(share);
            for (final Rating r : Rating.values()) {
                final BigDecimal expectedAbsolute = r == Rating.D ? adj : BigDecimal.ZERO;
                final BigDecimal expectedRelative = r == Rating.D ? BigDecimal.ONE : BigDecimal.ZERO;
                softly.assertThat(po.getCzkInvested(r)).as(r + " invested").isEqualTo(expectedAbsolute);
                softly.assertThat(po.getCzkAtRisk(r)).as(r + " at risk").isEqualTo(expectedAbsolute);
                softly.assertThat(po.getShareOnInvestment(r))
                        .as(r + " as a share")
                        .isEqualTo(expectedRelative);
                softly.assertThat(po.getAtRiskShareOnInvestment(r))
                        .as(r + " at risk as a share")
                        .isEqualTo(expectedRelative);
            }
        });
    }
}
