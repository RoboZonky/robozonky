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

package com.github.robozonky.strategy.natural;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.OffsetDateTime;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.Ratio;
import com.github.robozonky.api.remote.entities.Reservation;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.api.strategies.ReservationDescriptor;
import com.github.robozonky.api.strategies.ReservationStrategy;
import com.github.robozonky.internal.remote.entities.MyReservationImpl;
import com.github.robozonky.internal.remote.entities.ReservationImpl;
import com.github.robozonky.strategy.natural.conditions.MarketplaceFilter;
import com.github.robozonky.strategy.natural.conditions.MarketplaceFilterCondition;
import com.github.robozonky.test.AbstractMinimalRoboZonkyTest;
import com.github.robozonky.test.mock.MockReservationBuilder;

class NaturalLanguageReservationStrategyTest extends AbstractMinimalRoboZonkyTest {

    private static Reservation mockReservation(final int amount) {
        final MyReservationImpl r = mock(MyReservationImpl.class);
        when(r.getReservedAmount()).thenReturn(Money.from(amount));
        return new MockReservationBuilder()
            .set(ReservationImpl::setAmount, Money.from(amount))
            .set(ReservationImpl::setRemainingInvestment, Money.from(amount))
            .set(ReservationImpl::setReservedAmount, Money.from(0))
            .set(ReservationImpl::setDatePublished, OffsetDateTime.now())
            .set(ReservationImpl::setMyReservation, r)
            .set(ReservationImpl::setInterestRate, Rating.A.getInterestRate())
            .build();
    }

    @Test
    void unacceptablePortfolioDueToOverInvestment() {
        final DefaultValues v = new DefaultValues(DefaultPortfolio.EMPTY);
        v.setTargetPortfolioSize(1000);
        final ParsedStrategy p = new ParsedStrategy(v, Collections.emptyList(), Collections.emptyMap(),
                Collections.emptyMap());
        final ReservationStrategy s = new NaturalLanguageReservationStrategy(p);
        final PortfolioOverview portfolio = mock(PortfolioOverview.class);
        when(portfolio.getInvested()).thenReturn(p.getMaximumInvestmentSize());
        final boolean result = s.recommend(new ReservationDescriptor(mockReservation(200), () -> null), () -> portfolio,
                mockSessionInfo());
        assertThat(result).isFalse();
    }

    @Test
    void noReservationsApplicable() {
        final MarketplaceFilter filter = MarketplaceFilter.of(MarketplaceFilterCondition.alwaysAccepting());
        final ParsedStrategy p = new ParsedStrategy(DefaultPortfolio.PROGRESSIVE, Collections.singleton(filter));
        final ReservationStrategy s = new NaturalLanguageReservationStrategy(p);
        final PortfolioOverview portfolio = mock(PortfolioOverview.class);
        when(portfolio.getShareOnInvestment(any())).thenReturn(Ratio.ZERO);
        when(portfolio.getInvested()).thenReturn(p.getMaximumInvestmentSize()
            .subtract(1));
        final boolean result = s.recommend(new ReservationDescriptor(mockReservation(200), () -> null), () -> portfolio,
                mockSessionInfo());
        assertThat(result).isFalse();
    }

    @Test
    void nothingRecommendedDueToRatingOverinvested() {
        final ParsedStrategy p = new ParsedStrategy(DefaultPortfolio.EMPTY, Collections.emptySet());
        final ReservationStrategy s = new NaturalLanguageReservationStrategy(p);
        final PortfolioOverview portfolio = mock(PortfolioOverview.class);
        when(portfolio.getInvested()).thenReturn(p.getMaximumInvestmentSize()
            .subtract(1));
        when(portfolio.getShareOnInvestment(any())).thenReturn(Ratio.ZERO);
        final boolean result = s.recommend(new ReservationDescriptor(mockReservation(200), () -> null), () -> portfolio,
                mockSessionInfo());
        assertThat(result).isFalse();
        assertThatThrownBy(s::getMode).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void recommendationIsMade() {
        final ParsedStrategy p = new ParsedStrategy(DefaultPortfolio.PROGRESSIVE, Collections.emptySet());
        final ReservationStrategy s = new NaturalLanguageReservationStrategy(p);
        final PortfolioOverview portfolio = mock(PortfolioOverview.class);
        when(portfolio.getInvested()).thenReturn(p.getMaximumInvestmentSize()
            .subtract(1));
        when(portfolio.getShareOnInvestment(any())).thenReturn(Ratio.ZERO);
        final Reservation l = mockReservation(200);
        final ReservationDescriptor ld = new ReservationDescriptor(l, () -> null);
        final boolean result = s.recommend(ld, () -> portfolio, mockSessionInfo());
        assertThat(result).isTrue();
    }
}
