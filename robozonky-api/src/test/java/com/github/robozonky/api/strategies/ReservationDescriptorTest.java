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

package com.github.robozonky.api.strategies;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.api.remote.entities.MyReservation;
import com.github.robozonky.api.remote.entities.Reservation;
import com.github.robozonky.api.remote.enums.Rating;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReservationDescriptorTest {

    private static Reservation mockReservation() {
        return mockReservation(Rating.D);
    }

    private static Reservation mockReservation(final Rating r) {
        final MyReservation mr = mock(MyReservation.class);
        when(mr.getReservedAmount()).thenReturn(Money.from(1_000));
        final Reservation rs = mock(Reservation.class);
        when(rs.getId()).thenReturn(1);
        when(rs.getRating()).thenReturn(r);
        when(rs.getAmount()).thenReturn(Money.from(2_000));
        when(rs.getNonReservedRemainingInvestment()).thenReturn(Money.from(1_000));
        when(rs.getDatePublished()).thenReturn(OffsetDateTime.now());
        when(rs.getMyReservation()).thenReturn(mr);
        return rs;
    }

    @Test
    void constructor() {
        final Loan l = mock(Loan.class);
        final Reservation mockedReservation = ReservationDescriptorTest.mockReservation(Rating.AAAAA);
        final ReservationDescriptor ld = new ReservationDescriptor(mockedReservation, () -> l);
        assertThat(ld.item()).isSameAs(mockedReservation);
        assertThat(ld.related()).isSameAs(l);
    }

    @Test
    void equalsSelf() {
        final Reservation mockedReservation = ReservationDescriptorTest.mockReservation();
        final ReservationDescriptor ld = new ReservationDescriptor(mockedReservation, () -> null);
        assertThat(ld)
                .isNotEqualTo(null)
                .isEqualTo(ld);
        final ReservationDescriptor ld2 = new ReservationDescriptor(mockedReservation, () -> null);
        assertThat(ld).isEqualTo(ld2);
    }

    @Test
    void recommendAmount() {
        final Reservation mockedReservation = ReservationDescriptorTest.mockReservation();
        final ReservationDescriptor ld = new ReservationDescriptor(mockedReservation, () -> null);
        final Optional<RecommendedReservation> r = ld.recommend(Money.from(1_000));
        assertThat(r).isPresent();
        final RecommendedReservation recommendation = r.get();
        assertSoftly(softly -> {
            softly.assertThat(recommendation.descriptor()).isSameAs(ld);
            softly.assertThat(recommendation.amount()).isEqualTo(Money.from(1_000));
        });
    }

    @Test
    void recommendWrongAmount() {
        final Reservation mockedReservation = ReservationDescriptorTest.mockReservation();
        final ReservationDescriptor ld = new ReservationDescriptor(mockedReservation, () -> null);
        final Optional<RecommendedReservation> r = ld.recommend(Money.from(1));
        assertThat(r).isEmpty();
    }
}
