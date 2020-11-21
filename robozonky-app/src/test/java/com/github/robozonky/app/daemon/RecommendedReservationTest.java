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

import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;

import com.github.robozonky.api.remote.entities.Reservation;
import com.github.robozonky.api.strategies.ReservationDescriptor;
import com.github.robozonky.internal.remote.entities.ReservationImpl;

class RecommendedReservationTest {

    private static Reservation mockReservation() {
        final Reservation reservation = mock(ReservationImpl.class);
        when(reservation.getDatePublished()).thenReturn(OffsetDateTime.now());
        return reservation;
    }

    private static ReservationDescriptor mockDescriptor() {
        final Reservation reservation = mockReservation();
        return new ReservationDescriptor(reservation, () -> null);
    }

    @Test
    void constructor() {
        final ReservationDescriptor ld = mockDescriptor();
        final RecommendedReservation r = new RecommendedReservation(ld);
        assertThat(r.descriptor())
            .isSameAs(ld);
    }

    @Test
    void equalsSame() {
        final ReservationDescriptor ld = mockDescriptor();
        final int amount = 200;
        final RecommendedReservation r1 = new RecommendedReservation(ld);
        assertThat(r1).isEqualTo(r1);
        final RecommendedReservation r2 = new RecommendedReservation(ld);
        assertThat(r1).isEqualTo(r2);
    }

    @Test
    void notEqualsDifferentLoanDescriptor() {
        final int amount = 200;
        final RecommendedReservation r1 = new RecommendedReservation(mockDescriptor());
        final RecommendedReservation r2 = new RecommendedReservation(mockDescriptor());
        assertThat(r1).isNotEqualTo(r2);
    }

    @Test
    void notEqualsDifferentJavaType() {
        final RecommendedReservation r1 = new RecommendedReservation(mockDescriptor());
        assertThat(r1).isNotEqualTo(r1.toString());
    }
}
