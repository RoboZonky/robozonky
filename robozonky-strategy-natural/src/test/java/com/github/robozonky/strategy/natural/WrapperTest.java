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

import java.math.BigDecimal;

import com.github.robozonky.api.Ratio;
import com.github.robozonky.api.remote.entities.Participation;
import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.remote.entities.sanitized.Reservation;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.remote.enums.Region;
import com.github.robozonky.api.strategies.InvestmentDescriptor;
import com.github.robozonky.api.strategies.LoanDescriptor;
import com.github.robozonky.api.strategies.ParticipationDescriptor;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.api.strategies.ReservationDescriptor;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.*;

class WrapperTest {

    private static final PortfolioOverview FOLIO = mock(PortfolioOverview.class);

    @Test
    void fromInvestment() {
        final Loan loan = Loan.custom()
                .setId(1)
                .setAmount(100_000)
                .setRevenueRate(Ratio.ZERO)
                .setInterestRate(Ratio.ONE)
                .setAnnuity(BigDecimal.ONE)
                .build();
        final int invested = 200;
        final Investment investment = Investment.fresh(loan, invested)
                .setSmpFee(BigDecimal.ONE)
                .build();
        final Wrapper<InvestmentDescriptor> w = Wrapper.wrap(new InvestmentDescriptor(investment, () -> loan), FOLIO);
        assertSoftly(softly -> {
            softly.assertThat(w.getStory()).isEqualTo(loan.getStory());
            softly.assertThat(w.getRegion()).isEqualTo(loan.getRegion());
            softly.assertThat(w.getRating()).isEqualTo(loan.getRating());
            softly.assertThat(w.getOriginalAmount()).isEqualTo(loan.getAmount());
            softly.assertThat(w.getInterestRate()).isEqualTo(Ratio.ONE);
            softly.assertThat(w.getRevenueRate()).isEqualTo(Ratio.ZERO);
            softly.assertThat(w.getOriginalAnnuity()).isEqualTo(loan.getAnnuity().intValue());
            softly.assertThat(w.getRemainingTermInMonths()).isEqualTo(investment.getRemainingMonths());
            softly.assertThat(w.getRemainingPrincipal()).isEqualTo(BigDecimal.valueOf(invested));
            softly.assertThat(w.saleFee()).contains(BigDecimal.ONE);
            softly.assertThat(w.toString()).isNotNull();
        });
    }

    @Test
    void fromReservation() {
        final Reservation reservation = Reservation.custom()
                .setAnnuity(BigDecimal.ONE)
                .setRating(Rating.D)
                .setRegion(Region.JIHOCESKY)
                .setRevenueRate(Ratio.ZERO)
                .setInterestRate(Ratio.ONE)
                .build();
        final Wrapper<ReservationDescriptor> w = Wrapper.wrap(new ReservationDescriptor(reservation, () -> null), FOLIO);
        assertSoftly(softly -> {
            softly.assertThat(w.getStory()).isEqualTo(reservation.getStory());
            softly.assertThat(w.getRegion()).isEqualTo(reservation.getRegion());
            softly.assertThat(w.getRating()).isEqualTo(reservation.getRating());
            softly.assertThat(w.getOriginalAmount()).isEqualTo(reservation.getAmount());
            softly.assertThat(w.getInterestRate()).isEqualTo(Ratio.ONE);
            softly.assertThat(w.getRevenueRate()).isEqualTo(Ratio.ZERO);
            softly.assertThat(w.getOriginalAnnuity()).isEqualTo(reservation.getAnnuity().intValue());
            softly.assertThat(w.getRemainingTermInMonths()).isEqualTo(reservation.getTermInMonths());
            softly.assertThatThrownBy(w::getRemainingPrincipal).isInstanceOf(UnsupportedOperationException.class);
            softly.assertThat(w.saleFee()).isEmpty();
            softly.assertThat(w.toString()).isNotNull();
        });
    }

    @Test
    void fromLoan() {
        final Loan loan = Loan.custom()
                .setId(1)
                .setAmount(100_000)
                .setRevenueRate(Ratio.ZERO)
                .setInterestRate(Ratio.ONE)
                .setAnnuity(BigDecimal.ONE)
                .build();
        final Wrapper<LoanDescriptor> w = Wrapper.wrap(new LoanDescriptor(loan), FOLIO);
        assertSoftly(softly -> {
            softly.assertThat(w.getStory()).isEqualTo(loan.getStory());
            softly.assertThat(w.getRegion()).isEqualTo(loan.getRegion());
            softly.assertThat(w.getRating()).isEqualTo(loan.getRating());
            softly.assertThat(w.getOriginalAmount()).isEqualTo(loan.getAmount());
            softly.assertThat(w.getInterestRate()).isEqualTo(Ratio.ONE);
            softly.assertThat(w.getRevenueRate()).isEqualTo(Ratio.ZERO);
            softly.assertThat(w.getOriginalAnnuity()).isEqualTo(loan.getAnnuity().intValue());
            softly.assertThat(w.getRemainingTermInMonths()).isEqualTo(loan.getTermInMonths());
            softly.assertThatThrownBy(w::getRemainingPrincipal).isInstanceOf(UnsupportedOperationException.class);
            softly.assertThat(w.saleFee()).isEmpty();
            softly.assertThat(w.toString()).isNotNull();
        });
    }

    @Test
    void fromParticipation() {
        final Loan loan = Loan.custom()
                .setId(1)
                .setAmount(100_000)
                .setRevenueRate(Ratio.ZERO)
                .setInterestRate(Ratio.ONE)
                .setAnnuity(BigDecimal.ONE)
                .build();
        final int invested = 200;
        final Participation p = mock(Participation.class);
        when(p.getInterestRate()).thenReturn(Ratio.ONE);
        when(p.getRemainingPrincipal()).thenReturn(BigDecimal.valueOf(invested));
        final Wrapper<ParticipationDescriptor> w = Wrapper.wrap(new ParticipationDescriptor(p, () -> loan), FOLIO);
        assertSoftly(softly -> {
            softly.assertThat(w.getStory()).isEqualTo(loan.getStory());
            softly.assertThat(w.getRegion()).isEqualTo(loan.getRegion());
            softly.assertThat(w.getRating()).isEqualTo(loan.getRating());
            softly.assertThat(w.getOriginalAmount()).isEqualTo(loan.getAmount());
            softly.assertThat(w.getInterestRate()).isEqualTo(Ratio.ONE);
            softly.assertThat(w.getRevenueRate()).isEqualTo(Ratio.ZERO);
            softly.assertThat(w.getOriginalAnnuity()).isEqualTo(loan.getAnnuity().intValue());
            softly.assertThat(w.getRemainingTermInMonths()).isEqualTo(loan.getTermInMonths());
            softly.assertThat(w.getRemainingPrincipal()).isEqualTo(BigDecimal.valueOf(invested));
            softly.assertThat(w.saleFee()).isEmpty();
            softly.assertThat(w.toString()).isNotNull();
        });
    }

    @Test
    void equality() {
        final Loan loan = Loan.custom()
                .setId(1)
                .setAmount(2)
                .build();
        final Wrapper<LoanDescriptor> w = Wrapper.wrap(new LoanDescriptor(loan), FOLIO);
        assertSoftly(softly -> {
            softly.assertThat(w).isEqualTo(w);
            softly.assertThat(w).isNotEqualTo(null);
            softly.assertThat(w).isNotEqualTo("");
        });
        final Wrapper<LoanDescriptor> w2 = Wrapper.wrap(new LoanDescriptor(loan), FOLIO);
        assertSoftly(softly -> {
            softly.assertThat(w).isEqualTo(w2);
            softly.assertThat(w2).isEqualTo(w);
        });
    }
}
