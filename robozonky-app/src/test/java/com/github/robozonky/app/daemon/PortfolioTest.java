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
import java.util.stream.Stream;

import com.github.robozonky.api.remote.entities.CurrentOverview;
import com.github.robozonky.api.remote.entities.RiskPortfolio;
import com.github.robozonky.api.remote.entities.Statistics;
import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.common.remote.Zonky;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PortfolioTest extends AbstractZonkyLeveragingTest {

    @Test
    void chargeSimulation() {
        final BlockedAmountProcessor ba = spy(new BlockedAmountProcessor());
        final Portfolio p = Portfolio.create(mockTenant(), () -> ba);
        final PortfolioOverview po = p.getOverview();
        p.simulateCharge(1, Rating.A, BigDecimal.TEN);
        verify(ba).simulateCharge(eq(1), eq(Rating.A), eq(BigDecimal.TEN));
        final PortfolioOverview po2 = p.getOverview();
        assertThat(po2).isNotSameAs(po);
    }

    @Test
    void atRiskUpdate() {
        final Investment i = Investment.custom()
                .setRating(Rating.D)
                .setRemainingPrincipal(BigDecimal.TEN)
                .setPaidInterest(BigDecimal.ZERO)
                .setPaidPenalty(BigDecimal.ZERO)
                .build();
        final Statistics stats = mock(Statistics.class);
        final RiskPortfolio r = new RiskPortfolio(i.getRating(), 0, 0, i.getRemainingPrincipal().longValue());
        when(stats.getRiskPortfolio()).thenReturn(Collections.singletonList(r));
        when(stats.getCurrentOverview()).thenReturn(mock(CurrentOverview.class));
        final Zonky zonky = harmlessZonky(10_000);
        when(zonky.getStatistics()).thenReturn(stats);
        when(zonky.getDelinquentInvestments()).thenReturn(Stream.of(i));
        final TransactionalPortfolio tp = createTransactionalPortfolio(zonky);
        final long result = tp.getPortfolio().getOverview().getCzkAtRisk(Rating.D).longValue();
        assertThat(result).isEqualTo(BigDecimal.TEN.longValue());
    }

}
