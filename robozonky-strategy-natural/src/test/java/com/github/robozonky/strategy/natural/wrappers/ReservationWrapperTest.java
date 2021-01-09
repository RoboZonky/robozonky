/*
 * Copyright 2021 The RoboZonky Project
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

package com.github.robozonky.strategy.natural.wrappers;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.Ratio;
import com.github.robozonky.api.remote.entities.Reservation;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.remote.enums.Region;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.api.strategies.ReservationDescriptor;
import com.github.robozonky.internal.remote.entities.ReservationImpl;
import com.github.robozonky.test.mock.MockReservationBuilder;

class ReservationWrapperTest {

    private static final PortfolioOverview FOLIO = mock(PortfolioOverview.class);

    @Test
    void fromReservation() {
        final Reservation reservation = new MockReservationBuilder()
            .set(ReservationImpl::setInsuranceActive, true)
            .set(ReservationImpl::setAnnuity, Money.from(BigDecimal.ONE))
            .set(ReservationImpl::setAmount, Money.from(100_000))
            .set(ReservationImpl::setRegion, Region.JIHOCESKY)
            .set(ReservationImpl::setRevenueRate, Ratio.ZERO)
            .set(ReservationImpl::setInterestRate, Rating.D.getInterestRate())
            .build();
        final Wrapper<ReservationDescriptor> w = Wrapper.wrap(new ReservationDescriptor(reservation, () -> null),
                FOLIO);
        assertSoftly(softly -> {
            softly.assertThat(w.getId())
                .isNotEqualTo(0);
            softly.assertThat(w.getStory())
                .isEqualTo(reservation.getStory());
            softly.assertThat(w.getRegion())
                .isEqualTo(reservation.getRegion());
            softly.assertThat(w.getOriginalAmount())
                .isEqualTo(reservation.getAmount()
                    .getValue()
                    .intValue());
            softly.assertThat(w.getInterestRate())
                .isEqualTo(Rating.D.getInterestRate());
            softly.assertThat(w.getRevenueRate())
                .isEqualTo(Ratio.ZERO);
            softly.assertThat(w.getOriginalAnnuity())
                .isEqualTo(reservation.getAnnuity()
                    .getValue()
                    .intValue());
            softly.assertThat(w.getRemainingTermInMonths())
                .isEqualTo(reservation.getTermInMonths());
            softly.assertThat(w.getRemainingPrincipal())
                .isEqualTo(BigDecimal.valueOf(w.getOriginalAmount()));
            softly.assertThat(w.getSellFee())
                .isEmpty();
            softly.assertThat(w.getCurrentDpd())
                .isEmpty();
            softly.assertThat(w.getLongestDpd())
                .isEmpty();
            softly.assertThat(w.getDaysSinceDpd())
                .isEmpty();
            softly.assertThat(w.isInsuranceActive())
                .isTrue();
            softly.assertThat(w.toString())
                .isNotNull();
        });
    }

    @Test
    void fromReservationWithoutRevenueRate() {
        final Reservation reservation = new MockReservationBuilder()
            .set(ReservationImpl::setInterestRate, Rating.B.getInterestRate())
            .set(ReservationImpl::setInsuranceActive, false)
            .build();
        final Wrapper<ReservationDescriptor> w = Wrapper.wrap(new ReservationDescriptor(reservation, () -> null),
                FOLIO);
        when(FOLIO.getInvested()).thenReturn(Money.ZERO);
        assertThat(w.getRevenueRate()).isEqualTo(Ratio.fromPercentage("9.99"));
        assertThat(w.isInsuranceActive()).isFalse();
    }

}
