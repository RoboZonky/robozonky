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

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.remote.entities.Investment;
import com.github.robozonky.api.remote.entities.Statistics;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.internal.remote.Zonky;
import com.github.robozonky.internal.remote.entities.RiskPortfolioImpl;
import com.github.robozonky.internal.remote.entities.StatisticsImpl;
import com.github.robozonky.internal.tenant.RemotePortfolio;
import com.github.robozonky.internal.tenant.Tenant;
import com.github.robozonky.test.mock.MockInvestmentBuilder;

class RemotePortfolioImplTest extends AbstractZonkyLeveragingTest {

    @Test
    void throwsWhenRemoteFails() {
        final Zonky zonky = harmlessZonky();
        final Tenant tenant = mockTenant(zonky);
        doThrow(IllegalStateException.class).when(zonky)
            .getStatistics();
        final RemotePortfolio p = new RemotePortfolioImpl(tenant);
        assertThatThrownBy(p::getOverview)
            .isInstanceOf(IllegalStateException.class)
            .hasCauseInstanceOf(IllegalStateException.class);
    }

    @Test
    void chargesAffectAmounts() {
        final Zonky zonky = harmlessZonky();
        final Tenant tenant = mockTenant(zonky);
        Investment i = MockInvestmentBuilder.fresh()
            .setRating(Rating.C)
            .setAmount(BigDecimal.TEN)
            .build();
        when(zonky.getInvestments(any())).thenReturn(Stream.of(i));
        Statistics s = mock(StatisticsImpl.class);
        when(s.getRiskPortfolio())
            .thenReturn(singletonList(new RiskPortfolioImpl(Rating.D, Money.from(1), Money.from(2), Money.from(3))));
        when(zonky.getStatistics()).thenReturn(s);
        final RemotePortfolio p = new RemotePortfolioImpl(tenant);
        assertSoftly(softly -> {
            softly.assertThat(p.getTotal())
                .containsOnly(Map.entry(Rating.C, Money.from(10)), Map.entry(Rating.D, Money.from(5)));
            softly.assertThat(p.getOverview()
                .getInvested())
                .isEqualTo(Money.from(15));
        });
        p.simulateCharge(1, Rating.D, Money.from(15));
        assertSoftly(softly -> {
            softly.assertThat(p.getTotal())
                .containsOnly(Map.entry(Rating.C, Money.from(10)), Map.entry(Rating.D, Money.from(20)));
            softly.assertThat(p.getOverview()
                .getInvested())
                .isEqualTo(Money.from(30));
        });
        p.simulateCharge(2, Rating.A, Money.from(1));
        assertSoftly(softly -> {
            softly.assertThat(p.getTotal())
                .containsOnly(Map.entry(Rating.C, Money.from(10)), Map.entry(Rating.D, Money.from(20)),
                        Map.entry(Rating.A, Money.from(1)));
            softly.assertThat(p.getOverview()
                .getInvested())
                .isEqualTo(Money.from(31));
        });
        p.simulateCharge(3, Rating.A, Money.from(1));
        assertSoftly(softly -> {
            softly.assertThat(p.getTotal())
                .containsOnly(Map.entry(Rating.C, Money.from(10)), Map.entry(Rating.D, Money.from(20)),
                        Map.entry(Rating.A, Money.from(2)));
            softly.assertThat(p.getOverview()
                .getInvested())
                .isEqualTo(Money.from(32));
        });
    }
}
