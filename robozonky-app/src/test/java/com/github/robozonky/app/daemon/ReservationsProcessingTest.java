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

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

import com.github.robozonky.api.remote.entities.MyInvestment;
import com.github.robozonky.api.remote.entities.MyReservation;
import com.github.robozonky.api.remote.entities.ReservationPreference;
import com.github.robozonky.api.remote.entities.ReservationPreferences;
import com.github.robozonky.api.remote.entities.Restrictions;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.remote.entities.sanitized.Reservation;
import com.github.robozonky.api.remote.enums.LoanTermInterval;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.api.strategies.RecommendedReservation;
import com.github.robozonky.api.strategies.ReservationDescriptor;
import com.github.robozonky.api.strategies.ReservationMode;
import com.github.robozonky.api.strategies.ReservationStrategy;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.common.jobs.TenantPayload;
import com.github.robozonky.common.remote.Zonky;
import com.github.robozonky.common.tenant.Tenant;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReservationsProcessingTest extends AbstractZonkyLeveragingTest {

    private static final ReservationStrategy ALL_ACCEPTING_STRATEGY = new ReservationStrategy() {
        @Override
        public ReservationMode getMode() {
            return null;
        }

        @Override
        public Stream<RecommendedReservation> recommend(final Collection<ReservationDescriptor> available,
                                                        final PortfolioOverview portfolio,
                                                        final Restrictions restrictions) {
            return available.stream().map(r -> {
                final BigDecimal amount = BigDecimal.valueOf(r.item().getMyReservation().getReservedAmount());
                return r.recommend(amount);
            }).flatMap(o -> o.map(Stream::of).orElse(Stream.empty()));
        }
    };
    private static final ReservationPreference SOME_PREFERENCE =
            new ReservationPreference(LoanTermInterval.FROM_0_TO_12, Rating.AAAAA, false);

    private static MyReservation mockMyReservation() {
        final MyReservation r = mock(MyReservation.class);
        when(r.getReservedAmount()).thenReturn(200);
        when(r.getId()).thenReturn((long) (Math.random() * 1000));
        return r;
    }

    @Test
    void noStrategy() {
        final Zonky z = harmlessZonky(10_000);
        final Tenant t = mockTenant(z);
        final TenantPayload p = new ReservationsProcessing();
        p.accept(t);
        verify(z, never()).getPendingReservations();
    }

    @Test
    void disabledOnline() {
        final Zonky z = harmlessZonky(10_000);
        when(z.getReservationPreferences()).thenReturn(new ReservationPreferences()); // disabled by default
        final Tenant t = mockTenant(z);
        when(t.getReservationStrategy()).thenReturn(Optional.of(ALL_ACCEPTING_STRATEGY));
        final TenantPayload p = new ReservationsProcessing();
        p.accept(t);
        verify(z).getReservationPreferences();
        verify(z, never()).getPendingReservations();
    }

    @Test
    void enabled() {
        final Zonky z = harmlessZonky(399);
        final Reservation simple = Reservation.custom().setId(1).setMyReservation(mockMyReservation()).build();
        final Reservation withInvestment = Reservation.custom().setId(2).setMyReservation(mockMyReservation()).build();
        final Reservation simple2 = Reservation.custom().setId(3).setMyReservation(mockMyReservation()).build();
        final MyInvestment i = mockMyInvestment();
        final Loan loanWithInvestment = Loan.custom().setMyInvestment(i).build();
        when(z.getLoan(eq(simple.getId()))).thenReturn(Loan.custom().build());
        when(z.getLoan(eq(simple2.getId()))).thenReturn(Loan.custom().build());
        when(z.getLoan(eq(withInvestment.getId()))).thenReturn(loanWithInvestment);
        when(z.getReservationPreferences()).thenReturn(new ReservationPreferences(SOME_PREFERENCE));
        when(z.getPendingReservations()).thenReturn(Stream.of(withInvestment, simple, simple2));
        final Tenant t = mockTenant(z, false);
        when(t.getReservationStrategy()).thenReturn(Optional.of(ALL_ACCEPTING_STRATEGY));
        final TenantPayload p = new ReservationsProcessing();
        p.accept(t);
        verify(z).accept(eq(simple));
        verify(z, never()).accept(eq(withInvestment)); // already had investment
        verify(z, never()).accept(eq(simple2)); // was under balance by then
        assertThat(getEventsRequested()).hasSize(4);
    }

    @Test
    void skipsInvestmentsProperly() { // simulate skipping investments by introducing one of them twice
        final Zonky z = harmlessZonky(599);
        final Reservation simple = Reservation.custom().setId(1).setMyReservation(mockMyReservation()).build();
        final Reservation simple2 = Reservation.custom().setId(2).setMyReservation(mockMyReservation()).build();
        when(z.getLoan(eq(simple.getId()))).thenReturn(Loan.custom().build());
        when(z.getLoan(eq(simple2.getId()))).thenReturn(Loan.custom().build());
        when(z.getReservationPreferences()).thenReturn(new ReservationPreferences(SOME_PREFERENCE));
        when(z.getPendingReservations()).thenReturn(Stream.of(simple, simple, simple2));
        final Tenant t = mockTenant(z, false);
        when(t.getReservationStrategy()).thenReturn(Optional.of(ALL_ACCEPTING_STRATEGY));
        final TenantPayload p = new ReservationsProcessing();
        p.accept(t);
        verify(z, times(1)).accept(eq(simple));
        verify(z, times(1)).accept(eq(simple2));
        assertThat(getEventsRequested()).hasSize(6);
    }
}
