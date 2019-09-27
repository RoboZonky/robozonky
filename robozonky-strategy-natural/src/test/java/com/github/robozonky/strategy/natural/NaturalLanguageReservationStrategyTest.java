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

package com.github.robozonky.strategy.natural;

import com.github.robozonky.api.Ratio;
import com.github.robozonky.api.remote.entities.MyReservation;
import com.github.robozonky.api.remote.entities.Reservation;
import com.github.robozonky.api.remote.entities.Restrictions;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.api.strategies.RecommendedReservation;
import com.github.robozonky.api.strategies.ReservationDescriptor;
import com.github.robozonky.api.strategies.ReservationStrategy;
import com.github.robozonky.strategy.natural.conditions.MarketplaceFilter;
import com.github.robozonky.strategy.natural.conditions.MarketplaceFilterCondition;
import com.github.robozonky.test.mock.MockReservationBuilder;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.*;

class NaturalLanguageReservationStrategyTest {

    private static Reservation mockReservation(final int amount) {
        final MyReservation r = mock(MyReservation.class);
        when(r.getReservedAmount()).thenReturn(amount);
        return new MockReservationBuilder()
                .setAmount(amount)
                .setDatePublished(OffsetDateTime.now())
                .setNonReservedRemainingInvestment(amount)
                .setMyReservation(r)
                .setRating(Rating.A)
                .build();
    }

    @Test
    void unacceptablePortfolioDueToOverInvestment() {
        final DefaultValues v = new DefaultValues(DefaultPortfolio.EMPTY);
        v.setTargetPortfolioSize(1000);
        final ParsedStrategy p = new ParsedStrategy(v, Collections.emptyList(), Collections.emptyMap());
        final ReservationStrategy s = new NaturalLanguageReservationStrategy(p);
        final PortfolioOverview portfolio = mock(PortfolioOverview.class);
        when(portfolio.getCzkInvested()).thenReturn(BigDecimal.valueOf(p.getMaximumInvestmentSizeInCzk()));
        final Stream<RecommendedReservation> result =
                s.recommend(Collections.singletonList(new ReservationDescriptor(mockReservation(200), () -> null)),
                            portfolio, new Restrictions());
        assertThat(result).isEmpty();
    }

    @Test
    void noReservationsApplicable() {
        final MarketplaceFilter filter = MarketplaceFilter.of(MarketplaceFilterCondition.alwaysAccepting());
        final ParsedStrategy p = new ParsedStrategy(DefaultPortfolio.PROGRESSIVE, Collections.singleton(filter));
        final ReservationStrategy s = new NaturalLanguageReservationStrategy(p);
        final PortfolioOverview portfolio = mock(PortfolioOverview.class);
        when(portfolio.getCzkInvested()).thenReturn(BigDecimal.valueOf(p.getMaximumInvestmentSizeInCzk() - 1));
        final Stream<RecommendedReservation> result =
                s.recommend(Collections.singletonList(new ReservationDescriptor(mockReservation(200), () -> null)),
                            portfolio, new Restrictions());
        assertThat(result).isEmpty();
    }

    @Test
    void nothingRecommendedDueToRatingOverinvested() {
        final ParsedStrategy p = new ParsedStrategy(DefaultPortfolio.EMPTY, Collections.emptySet());
        final ReservationStrategy s = new NaturalLanguageReservationStrategy(p);
        final PortfolioOverview portfolio = mock(PortfolioOverview.class);
        when(portfolio.getCzkInvested()).thenReturn(BigDecimal.valueOf(p.getMaximumInvestmentSizeInCzk() - 1));
        when(portfolio.getShareOnInvestment(any())).thenReturn(Ratio.ZERO);
        final Stream<RecommendedReservation> result =
                s.recommend(Collections.singletonList(new ReservationDescriptor(mockReservation(200), () -> null)),
                            portfolio, new Restrictions());
        assertThat(result).isEmpty();
    }

    @Test
    void recommendationIsMade() {
        final ParsedStrategy p = new ParsedStrategy(DefaultPortfolio.PROGRESSIVE, Collections.emptySet());
        final ReservationStrategy s = new NaturalLanguageReservationStrategy(p);
        final PortfolioOverview portfolio = mock(PortfolioOverview.class);
        when(portfolio.getCzkInvested()).thenReturn(BigDecimal.valueOf(p.getMaximumInvestmentSizeInCzk() - 1));
        when(portfolio.getShareOnInvestment(any())).thenReturn(Ratio.ZERO);
        final Reservation l = mockReservation(200);
        final ReservationDescriptor ld = new ReservationDescriptor(l, () -> null);
        final List<RecommendedReservation> result =
                s.recommend(Collections.singleton(ld), portfolio, new Restrictions()).collect(Collectors.toList());
        assertThat(result).hasSize(1);
        final RecommendedReservation r = result.get(0);
        assertSoftly(softly -> {
            softly.assertThat(r.descriptor()).isEqualTo(ld);
            softly.assertThat(r.amount()).isEqualTo(BigDecimal.valueOf(200));
        });
    }
}
