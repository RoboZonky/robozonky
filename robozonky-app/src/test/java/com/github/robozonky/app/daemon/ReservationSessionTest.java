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
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import com.github.robozonky.api.notifications.Event;
import com.github.robozonky.api.notifications.ReservationAcceptationRecommendedEvent;
import com.github.robozonky.api.remote.entities.MyReservation;
import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.remote.entities.sanitized.Reservation;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.ReservationDescriptor;
import com.github.robozonky.api.strategies.ReservationStrategy;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.app.tenant.PowerTenant;
import com.github.robozonky.common.remote.Zonky;
import com.github.robozonky.common.tenant.RemotePortfolio;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.*;

class ReservationSessionTest extends AbstractZonkyLeveragingTest {

    private static Reservation mockReservation() {
        final MyReservation mr = mock(MyReservation.class);
        when(mr.getReservedAmount()).thenReturn(200);
        final Reservation p = mock(Reservation.class);
        when(p.getMyReservation()).thenReturn(mr);
        return p;
    }

    @Test
    void empty() {
        final Zonky z = harmlessZonky(0);
        final PowerTenant auth = mockTenant(z);
        final Collection<Investment> i = ReservationSession.process(auth, Collections.emptyList(), null);
        assertThat(i).isEmpty();
    }

    @Test
    void underBalance() {
        final Reservation p = mockReservation();
        final Loan l = Loan.custom().build();
        final ReservationStrategy s = mock(ReservationStrategy.class);
        final ReservationDescriptor pd = new ReservationDescriptor(p, () -> l);
        final Zonky z = harmlessZonky(0);
        final PowerTenant auth = mockTenant(z);
        final Collection<Investment> i = ReservationSession.process(auth, Collections.singleton(pd), s);
        assertSoftly(softly -> {
            softly.assertThat(i).isEmpty();
            softly.assertThat(getEventsRequested()).has(new Condition<List<? extends Event>>() {
                @Override
                public boolean matches(final List<? extends Event> events) {
                    return events.stream().noneMatch(e -> e instanceof ReservationAcceptationRecommendedEvent);
                }
            });
        });
        verify(s, never()).recommend(any(), any(), any());
    }

    @Test
    void properReal() {
        final Loan l = Loan.custom()
                .setId(1)
                .setAmount(200)
                .setRating(Rating.D)
                .setNonReservedRemainingInvestment(200)
                .setMyInvestment(mockMyInvestment())
                .build();
        final Reservation p = mockReservation();
        final ReservationStrategy s = mock(ReservationStrategy.class);
        when(s.recommend(any(), any(), any()))
                .thenAnswer(i -> {
                    final Collection<ReservationDescriptor> reservations = i.getArgument(0);
                    return reservations.stream()
                            .map(r -> r.recommend(BigDecimal.valueOf(200)))
                            .flatMap(o -> o.map(Stream::of).orElse(Stream.empty()));
                });
        final Zonky z = harmlessZonky(100_000);
        when(z.getLoan(eq(l.getId()))).thenReturn(l);
        final PowerTenant auth = mockTenant(z, false);
        final ReservationDescriptor pd = new ReservationDescriptor(p, () -> l);
        final Collection<Investment> i = ReservationSession.process(auth, Collections.singleton(pd), s);
        assertThat(i).hasSize(1);
        assertThat(getEventsRequested()).hasSize(4);
        verify(z).accept(eq(p));
        final RemotePortfolio rp = auth.getPortfolio();
        verify(rp).simulateCharge(eq(l.getId()), eq(l.getRating()), any());
    }

    @Test
    void properDry() {
        final Loan l = Loan.custom()
                .setId(1)
                .setAmount(200)
                .setRating(Rating.D)
                .setNonReservedRemainingInvestment(200)
                .setMyInvestment(mockMyInvestment())
                .build();
        final Reservation p = mockReservation();
        final ReservationStrategy s = mock(ReservationStrategy.class);
        when(s.recommend(any(), any(), any()))
                .thenAnswer(i -> {
                    final Collection<ReservationDescriptor> reservations = i.getArgument(0);
                    return reservations.stream()
                            .map(r -> r.recommend(BigDecimal.valueOf(200)))
                            .flatMap(o -> o.map(Stream::of).orElse(Stream.empty()));
                });
        final Zonky z = harmlessZonky(100_000);
        when(z.getLoan(eq(l.getId()))).thenReturn(l);
        final PowerTenant auth = mockTenant(z);
        final ReservationDescriptor pd = new ReservationDescriptor(p, () -> l);
        final Collection<Investment> i = ReservationSession.process(auth, Collections.singleton(pd), s);
        assertThat(i).hasSize(1);
        assertThat(getEventsRequested()).hasSize(4);
        verify(z, never()).accept(eq(p));
        final RemotePortfolio rp = auth.getPortfolio();
        verify(rp).simulateCharge(eq(l.getId()), eq(l.getRating()), any());
    }

    @Test
    void properFail() {
        final Loan l = Loan.custom()
                .setId(1)
                .setAmount(200)
                .setRating(Rating.D)
                .setNonReservedRemainingInvestment(200)
                .setMyInvestment(mockMyInvestment())
                .build();
        final Reservation p = mockReservation();
        final ReservationStrategy s = mock(ReservationStrategy.class);
        when(s.recommend(any(), any(), any()))
                .thenAnswer(i -> {
                    final Collection<ReservationDescriptor> reservations = i.getArgument(0);
                    return reservations.stream()
                            .map(r -> r.recommend(BigDecimal.valueOf(200)))
                            .flatMap(o -> o.map(Stream::of).orElse(Stream.empty()));
                });
        final Zonky z = harmlessZonky(100_000);
        when(z.getLoan(eq(l.getId()))).thenReturn(l);
        doThrow(IllegalStateException.class).when(z).accept(any());
        final PowerTenant auth = mockTenant(z, false);
        final ReservationDescriptor pd = new ReservationDescriptor(p, () -> l);
        final Collection<Investment> i = ReservationSession.process(auth, Collections.singleton(pd), s);
        assertThat(i).isEmpty();
        assertThat(getEventsRequested()).hasSize(3);
        verify(z).accept(eq(p));
        final RemotePortfolio rp = auth.getPortfolio();
        verify(rp, never()).simulateCharge(eq(l.getId()), eq(l.getRating()), any());
    }
}
