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

import com.github.robozonky.api.remote.entities.Investment;
import com.github.robozonky.api.remote.entities.RiskPortfolio;
import com.github.robozonky.api.remote.entities.Statistics;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.internal.remote.Select;
import com.github.robozonky.internal.remote.Zonky;
import com.github.robozonky.internal.tenant.Tenant;
import com.github.robozonky.test.mock.MockInvestmentBuilder;
import io.vavr.Tuple2;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class UtilTest extends AbstractZonkyLeveragingTest {

    @Test
    void atRisk() {
        final Investment i = MockInvestmentBuilder.fresh()
                .setRating(Rating.D)
                .setRemainingPrincipal(BigDecimal.TEN)
                .setPaidInterest(BigDecimal.ZERO)
                .setPaidPenalty(BigDecimal.ZERO)
                .build();
        final Statistics stats = mock(Statistics.class);
        final RiskPortfolio r = new RiskPortfolio(i.getRating(), 0, 0, i.getRemainingPrincipal().longValue());
        when(stats.getRiskPortfolio()).thenReturn(Collections.singletonList(r));
        final Zonky zonky = harmlessZonky();
        when(zonky.getStatistics()).thenReturn(stats);
        when(zonky.getDelinquentInvestments()).thenReturn(Stream.of(i));
        final Tenant tenant = mockTenant(zonky);
        final Map<Rating, BigDecimal> result = Util.getAmountsAtRisk(tenant);
        assertThat(result).containsOnlyKeys(Rating.D);
        assertThat(result.get(Rating.D).longValue()).isEqualTo(BigDecimal.TEN.longValue());
    }

    @Test
    void sellable() {
        final Investment i = MockInvestmentBuilder.fresh()
                .setRating(Rating.D)
                .setRemainingPrincipal(BigDecimal.TEN)
                .setSmpFee(BigDecimal.ONE)
                .build();
        final Investment i2 = MockInvestmentBuilder.fresh()
                .setRating(Rating.A)
                .setRemainingPrincipal(BigDecimal.ONE)
                .setSmpFee(BigDecimal.ZERO)
                .build();
        final Zonky zonky = harmlessZonky();
        when(zonky.getInvestments((Select)any())).thenReturn(Stream.of(i, i2));
        final Tenant tenant = mockTenant(zonky);
        final Tuple2<Map<Rating, BigDecimal>, Map<Rating, BigDecimal>> result = Util.getAmountsSellable(tenant);
        assertThat(result._1).containsOnlyKeys(Rating.D, Rating.A);
        assertThat(result._2).containsOnlyKeys(Rating.A);
    }

}
