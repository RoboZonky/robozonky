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

package com.github.robozonky.app.daemon;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.api.remote.entities.MyReservation;
import com.github.robozonky.api.remote.entities.Reservation;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.ReservationDescriptor;
import com.github.robozonky.api.strategies.ReservationStrategy;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.app.tenant.PowerTenant;
import com.github.robozonky.internal.remote.Zonky;
import com.github.robozonky.internal.tenant.RemotePortfolio;
import com.github.robozonky.test.mock.MockLoanBuilder;
import com.github.robozonky.test.mock.MockReservationBuilder;

class ReservationSessionTest extends AbstractZonkyLeveragingTest {

    private static Reservation mockReservation() {
        final MyReservation mr = mock(MyReservation.class);
        when(mr.getReservedAmount()).thenReturn(Money.from(200));
        return new MockReservationBuilder()
            .setMyReservation(mr)
            .build();
    }

    @Test
    void empty() {
        final Zonky z = harmlessZonky();
        final PowerTenant auth = mockTenant(z);
        final Collection<Reservation> i = ReservationSession.process(auth, Collections.emptyList(), null);
        assertThat(i).isEmpty();
    }

    @Test
    void properReal() {
        final Loan l = new MockLoanBuilder()
            .setAmount(200)
            .setRating(Rating.D)
            .setNonReservedRemainingInvestment(200)
            .setMyInvestment(mockMyInvestment())
            .build();
        final int loanId = l.getId();
        final Reservation p = mockReservation();
        final ReservationStrategy s = mock(ReservationStrategy.class);
        when(s.recommend(any(), any(), any()))
            .thenAnswer(i -> {
                final Collection<ReservationDescriptor> reservations = i.getArgument(0);
                return reservations.stream()
                    .map(r -> r.recommend(Money.from(200)))
                    .flatMap(Optional::stream);
            });
        final Zonky z = harmlessZonky();
        when(z.getLoan(eq(l.getId()))).thenReturn(l);
        final PowerTenant auth = mockTenant(z, false);
        final ReservationDescriptor pd = new ReservationDescriptor(p, () -> l);
        final Collection<Reservation> i = ReservationSession.process(auth, Collections.singleton(pd), s);
        assertThat(i).hasSize(1);
        assertThat(getEventsRequested()).hasSize(4);
        verify(z).accept(eq(p));
        final RemotePortfolio rp = auth.getPortfolio();
        final Rating rating = l.getRating();
        verify(rp).simulateCharge(eq(loanId), eq(rating), any());
    }

    @Test
    void properDry() {
        final Loan l = new MockLoanBuilder()
            .setAmount(200)
            .setRating(Rating.D)
            .setNonReservedRemainingInvestment(200)
            .setMyInvestment(mockMyInvestment())
            .build();
        final int loanId = l.getId();
        final Reservation p = mockReservation();
        final ReservationStrategy s = mock(ReservationStrategy.class);
        when(s.recommend(any(), any(), any()))
            .thenAnswer(i -> {
                final Collection<ReservationDescriptor> reservations = i.getArgument(0);
                return reservations.stream()
                    .map(r -> r.recommend(Money.from(200)))
                    .flatMap(Optional::stream);
            });
        final Zonky z = harmlessZonky();
        when(z.getLoan(eq(loanId))).thenReturn(l);
        final PowerTenant auth = mockTenant(z);
        final ReservationDescriptor pd = new ReservationDescriptor(p, () -> l);
        final Collection<Reservation> i = ReservationSession.process(auth, Collections.singleton(pd), s);
        assertThat(i).hasSize(1);
        assertThat(getEventsRequested()).hasSize(4);
        verify(z, never()).accept(eq(p));
        final RemotePortfolio rp = auth.getPortfolio();
        Rating rating = l.getRating();
        verify(rp).simulateCharge(eq(loanId), eq(rating), any());
        verify(auth).setKnownBalanceUpperBound(eq(Money.from(Integer.MAX_VALUE - 200)));
    }

    @Test
    void properFail() {
        final Loan l = new MockLoanBuilder()
            .setAmount(200)
            .setRating(Rating.D)
            .setNonReservedRemainingInvestment(200)
            .setMyInvestment(mockMyInvestment())
            .build();
        final int loanId = l.getId();
        final Reservation p = mockReservation();
        final ReservationStrategy s = mock(ReservationStrategy.class);
        when(s.recommend(any(), any(), any()))
            .thenAnswer(i -> {
                final Collection<ReservationDescriptor> reservations = i.getArgument(0);
                return reservations.stream()
                    .map(r -> r.recommend(Money.from(200)))
                    .flatMap(Optional::stream);
            });
        final Zonky z = harmlessZonky();
        when(z.getLoan(eq(loanId))).thenReturn(l);
        doThrow(IllegalStateException.class).when(z)
            .accept(any());
        final PowerTenant auth = mockTenant(z, false);
        final ReservationDescriptor pd = new ReservationDescriptor(p, () -> l);
        final Collection<Reservation> i = ReservationSession.process(auth, Collections.singleton(pd), s);
        assertThat(i).isEmpty();
        assertThat(getEventsRequested()).hasSize(3);
        verify(z).accept(eq(p));
        final RemotePortfolio rp = auth.getPortfolio();
        final Rating rating = l.getRating();
        verify(rp, never()).simulateCharge(eq(loanId), eq(rating), any());
        verify(auth).setKnownBalanceUpperBound(eq(Money.from(199)));
    }
}
