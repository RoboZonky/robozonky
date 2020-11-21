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

import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.api.remote.entities.Reservation;
import com.github.robozonky.api.remote.enums.LoanTermInterval;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.api.strategies.ReservationDescriptor;
import com.github.robozonky.api.strategies.ReservationMode;
import com.github.robozonky.api.strategies.ReservationStrategy;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.internal.jobs.TenantPayload;
import com.github.robozonky.internal.remote.Zonky;
import com.github.robozonky.internal.remote.entities.MyReservationImpl;
import com.github.robozonky.internal.remote.entities.ReservationImpl;
import com.github.robozonky.internal.remote.entities.ReservationPreferenceImpl;
import com.github.robozonky.internal.remote.entities.ReservationPreferencesImpl;
import com.github.robozonky.internal.tenant.Tenant;
import com.github.robozonky.test.mock.MockLoanBuilder;
import com.github.robozonky.test.mock.MockReservationBuilder;

class ReservationsProcessingTest extends AbstractZonkyLeveragingTest {

    private static final ReservationStrategy ALL_ACCEPTING_STRATEGY = new ReservationStrategy() {
        @Override
        public ReservationMode getMode() {
            return null;
        }

        @Override
        public boolean recommend(ReservationDescriptor reservationDescriptor,
                Supplier<PortfolioOverview> portfolioOverviewSupplier, SessionInfo sessionInfo) {
            return true;
        }
    };
    private static final ReservationPreferenceImpl SOME_PREFERENCE = new ReservationPreferenceImpl(
            LoanTermInterval.FROM_0_TO_12, Rating.AAAAA, false);

    private static MyReservationImpl mockMyReservation() {
        final MyReservationImpl r = mock(MyReservationImpl.class);
        when(r.getReservedAmount()).thenReturn(Money.from(200));
        when(r.getId()).thenReturn((long) (Math.random() * 1000));
        return r;
    }

    @Test
    void noStrategy() {
        final Zonky z = harmlessZonky();
        final Tenant t = mockTenant(z);
        final TenantPayload p = new ReservationsProcessing();
        p.accept(t);
        verify(z, never()).getPendingReservations();
    }

    @Test
    void disabledOnline() {
        final Zonky z = harmlessZonky();
        when(z.getReservationPreferences()).thenReturn(new ReservationPreferencesImpl()); // disabled by default
        final Tenant t = mockTenant(z);
        when(t.getReservationStrategy()).thenReturn(Optional.of(ALL_ACCEPTING_STRATEGY));
        final TenantPayload p = new ReservationsProcessing();
        p.accept(t);
        verify(z).getReservationPreferences();
        verify(z, never()).getPendingReservations();
    }

    @Test
    void enabled() {
        final Zonky z = harmlessZonky();
        final Reservation simple = new MockReservationBuilder()
            .set(ReservationImpl::setMyReservation, mockMyReservation())
            .build();
        final Loan fresh = MockLoanBuilder.fresh();
        when(z.getLoan(eq(simple.getId()))).thenReturn(fresh);
        when(z.getReservationPreferences()).thenReturn(new ReservationPreferencesImpl(SOME_PREFERENCE));
        when(z.getPendingReservations()).thenReturn(Stream.of(simple));
        final Tenant t = mockTenant(z, false);
        when(t.getReservationStrategy()).thenReturn(Optional.of(ALL_ACCEPTING_STRATEGY));
        final TenantPayload p = new ReservationsProcessing();
        p.accept(t);
        verify(z).accept(eq(simple));
        assertThat(getEventsRequested()).hasSize(3);
    }

    @Test
    void skipsInvestmentsProperly() { // simulate skipping investments by introducing one of them twice
        final Zonky z = harmlessZonky();
        final Reservation simple = new MockReservationBuilder()
            .set(ReservationImpl::setMyReservation, mockMyReservation())
            .build();
        final Reservation simple2 = new MockReservationBuilder()
            .set(ReservationImpl::setMyReservation, mockMyReservation())
            .build();
        final Loan fresh1 = MockLoanBuilder.fresh();
        final Loan fresh2 = MockLoanBuilder.fresh();
        when(z.getLoan(eq(simple.getId()))).thenReturn(fresh1);
        when(z.getLoan(eq(simple2.getId()))).thenReturn(fresh2);
        when(z.getReservationPreferences()).thenReturn(new ReservationPreferencesImpl(SOME_PREFERENCE));
        when(z.getPendingReservations()).thenReturn(Stream.of(simple, simple, simple2));
        final Tenant t = mockTenant(z, false);
        when(t.getReservationStrategy()).thenReturn(Optional.of(ALL_ACCEPTING_STRATEGY));
        final TenantPayload p = new ReservationsProcessing();
        p.accept(t);
        verify(z, times(1)).accept(eq(simple));
        verify(z, times(1)).accept(eq(simple2));
        assertThat(getEventsRequested()).hasSize(4);
    }
}
