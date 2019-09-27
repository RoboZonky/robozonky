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

package com.github.robozonky.app.daemon;

import com.github.robozonky.api.remote.entities.Investment;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.InvestmentDescriptor;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.api.strategies.RecommendedInvestment;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.test.mock.MockInvestmentBuilder;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;

class SelingThrottleTest extends AbstractZonkyLeveragingTest {

    @Test
    void picksSmallestOneIfAllOverThreshold() {
        final Rating rating = Rating.A;
        final Investment i1 = MockInvestmentBuilder.fresh()
                .setRating(rating)
                .setRemainingPrincipal(BigDecimal.TEN)
                .build();
        final Investment i2 = MockInvestmentBuilder.fresh()
                .setRating(rating)
                .setRemainingPrincipal(BigDecimal.TEN.pow(2))
                .build();
        final Investment i3 = MockInvestmentBuilder.fresh()
                .setRating(rating)
                .setRemainingPrincipal(BigDecimal.ONE)
                .build();
        final PortfolioOverview portfolioOverview = mockPortfolioOverview();
        when(portfolioOverview.getCzkInvested(eq(rating))).thenReturn(BigDecimal.TEN);
        final Stream<RecommendedInvestment> recommendations = Stream.of(i1, i2, i3)
                .map(i -> new InvestmentDescriptor(i, () -> null))
                .map(d -> d.recommend(d.item().getRemainingPrincipal()))
                .flatMap(o -> o.map(Stream::of).orElse(Stream.empty()));
        final SellingThrottle t = new SellingThrottle();
        final Stream<RecommendedInvestment> throttled = t.apply(recommendations, portfolioOverview);
        assertThat(throttled)
                .extracting(r -> r.descriptor().item())
                .containsOnly(i3);
    }

    @Test
    void picksAllBelowThreshold() {
        final Rating rating = Rating.A;
        final Investment i1 = MockInvestmentBuilder.fresh()
                .setRating(rating)
                .setRemainingPrincipal(BigDecimal.TEN)
                .build();
        final Investment i2 = MockInvestmentBuilder.fresh()
                .setRating(rating)
                .setRemainingPrincipal(BigDecimal.TEN.pow(2))
                .build();
        final Investment i3 = MockInvestmentBuilder.fresh()
                .setRating(rating)
                .setRemainingPrincipal(BigDecimal.ONE)
                .build();
        final PortfolioOverview portfolioOverview = mockPortfolioOverview();
        when(portfolioOverview.getCzkInvested()).thenReturn(BigDecimal.valueOf(2200));
        final Stream<RecommendedInvestment> recommendations = Stream.of(i1, i2, i3)
                .map(i -> new InvestmentDescriptor(i, () -> null))
                .map(d -> d.recommend(d.item().getRemainingPrincipal()))
                .flatMap(o -> o.map(Stream::of).orElse(Stream.empty()));
        final SellingThrottle t = new SellingThrottle();
        final Stream<RecommendedInvestment> throttled = t.apply(recommendations, portfolioOverview);
        assertThat(throttled)
                .extracting(r -> r.descriptor().item())
                .containsOnly(i1, i3);
    }

    @Test
    void noInput() {
        final PortfolioOverview portfolioOverview = mockPortfolioOverview();
        final Stream<RecommendedInvestment> recommendations = Stream.empty();
        final SellingThrottle t = new SellingThrottle();
        final Stream<RecommendedInvestment> throttled = t.apply(recommendations, portfolioOverview);
        assertThat(throttled).isEmpty();
    }
}
