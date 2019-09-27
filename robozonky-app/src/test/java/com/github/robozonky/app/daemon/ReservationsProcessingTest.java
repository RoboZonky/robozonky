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

import com.github.robozonky.api.Money;
import com.github.robozonky.api.remote.entities.*;
import com.github.robozonky.api.remote.enums.LoanTermInterval;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.*;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.internal.jobs.TenantPayload;
import com.github.robozonky.internal.remote.Zonky;
import com.github.robozonky.internal.tenant.Tenant;
import com.github.robozonky.test.mock.MockLoanBuilder;
import com.github.robozonky.test.mock.MockReservationBuilder;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
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
                final Money amount = r.item().getMyReservation().getReservedAmount();
                return r.recommend(amount);
            }).flatMap(Optional::stream);
        }
    };
    private static final ReservationPreference SOME_PREFERENCE =
            new ReservationPreference(LoanTermInterval.FROM_0_TO_12, Rating.AAAAA, false);

    private static MyReservation mockMyReservation() {
        final MyReservation r = mock(MyReservation.class);
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
        final Zonky z = harmlessZonky();
        final Reservation simple = new MockReservationBuilder().setMyReservation(mockMyReservation()).build();
        final Reservation withInvestment = new MockReservationBuilder().setMyReservation(mockMyReservation()).build();
        final MyInvestment i = mockMyInvestment();
        final Loan loanWithInvestment = new MockLoanBuilder().setMyInvestment(i).build();
        final Loan fresh = MockLoanBuilder.fresh();
        when(z.getLoan(eq(simple.getId()))).thenReturn(fresh);
        when(z.getLoan(eq(withInvestment.getId()))).thenReturn(loanWithInvestment);
        when(z.getReservationPreferences()).thenReturn(new ReservationPreferences(SOME_PREFERENCE));
        when(z.getPendingReservations()).thenReturn(Stream.of(withInvestment, simple));
        final Tenant t = mockTenant(z, false);
        when(t.getReservationStrategy()).thenReturn(Optional.of(ALL_ACCEPTING_STRATEGY));
        final TenantPayload p = new ReservationsProcessing();
        p.accept(t);
        verify(z).accept(eq(simple));
        verify(z, never()).accept(eq(withInvestment)); // already had investment
        assertThat(getEventsRequested()).hasSize(4);
    }

    @Test
    void skipsInvestmentsProperly() { // simulate skipping investments by introducing one of them twice
        final Zonky z = harmlessZonky();
        final Reservation simple = new MockReservationBuilder().setMyReservation(mockMyReservation()).build();
        final Reservation simple2 = new MockReservationBuilder().setMyReservation(mockMyReservation()).build();
        final Loan fresh1 = MockLoanBuilder.fresh();
        final Loan fresh2 = MockLoanBuilder.fresh();
        when(z.getLoan(eq(simple.getId()))).thenReturn(fresh1);
        when(z.getLoan(eq(simple2.getId()))).thenReturn(fresh2);
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
