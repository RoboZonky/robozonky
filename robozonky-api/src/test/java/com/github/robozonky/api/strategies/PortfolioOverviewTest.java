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

package com.github.robozonky.api.strategies;

import java.math.BigDecimal;
import java.util.stream.Stream;

import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.entities.sanitized.MarketplaceLoan;
import com.github.robozonky.api.remote.enums.Rating;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.SoftAssertions.*;

class PortfolioOverviewTest {

    private static MarketplaceLoan mockLoan(final Rating r) {
        return MarketplaceLoan.custom()
                .setId(1)
                .setRating(r)
                .build();
    }

    @Test
    void emptyPortfolio() {
        final int balance = 5000;
        final PortfolioOverview o = PortfolioOverview.calculate(BigDecimal.valueOf(balance), Stream.empty());
        assertSoftly(softly -> {
            for (final Rating r : Rating.values()) {
                softly.assertThat(o.getShareOnInvestment(r)).isEqualTo(BigDecimal.ZERO);
            }
            softly.assertThat(o.getCzkAvailable()).isEqualTo(balance);
            softly.assertThat(o.getCzkInvested()).isEqualTo(0);
        });
    }

    @Test
    void somePortfolio() {
        final int balance = 5000;
        final Investment i1 = Investment.fresh(mockLoan(Rating.A), 400);
        final Investment i2 = Investment.fresh(mockLoan(Rating.B), 600);
        final PortfolioOverview o = PortfolioOverview.calculate(BigDecimal.valueOf(balance), Stream.of(i1, i2));
        assertSoftly(softly -> {
            softly.assertThat(o.getShareOnInvestment(Rating.A)).isEqualTo(new BigDecimal("0.4"));
            softly.assertThat(o.getShareOnInvestment(Rating.B)).isEqualTo(new BigDecimal("0.6"));
            softly.assertThat(o.getCzkAvailable()).isEqualTo(balance);
            softly.assertThat(o.getCzkInvested()).isEqualTo(1000);
        });
    }
}
