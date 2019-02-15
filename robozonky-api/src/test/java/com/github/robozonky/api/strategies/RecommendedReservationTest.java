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

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.github.robozonky.api.remote.entities.sanitized.Reservation;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class RecommendedReservationTest {

    private static Reservation mockReservation() {
        return Reservation.custom().setDatePublished(OffsetDateTime.now()).build();
    }

    private static ReservationDescriptor mockDescriptor() {
        final Reservation reservation = mockReservation();
        return new ReservationDescriptor(reservation, () -> null);
    }

    @Test
    void constructor() {
        final ReservationDescriptor ld = mockDescriptor();
        final int amount = 200;
        final RecommendedReservation r = new RecommendedReservation(ld, amount);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(r.descriptor()).isSameAs(ld);
            softly.assertThat(r.amount()).isEqualTo(BigDecimal.valueOf(amount));
        });
    }

    @Test
    void constructorNoLoanDescriptor() {
        final int amount = 200;
        assertThatThrownBy(() -> new RecommendedReservation(null, amount))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void equalsSame() {
        final ReservationDescriptor ld = mockDescriptor();
        final int amount = 200;
        final RecommendedReservation r1 = new RecommendedReservation(ld, amount);
        assertThat(r1).isEqualTo(r1);
        final RecommendedReservation r2 = new RecommendedReservation(ld, amount);
        assertThat(r1).isEqualTo(r2);
    }

    @Test
    void notEqualsDifferentLoanDescriptor() {
        final int amount = 200;
        final RecommendedReservation r1 = new RecommendedReservation(mockDescriptor(), amount);
        final RecommendedReservation r2 = new RecommendedReservation(mockDescriptor(), amount);
        assertThat(r1).isNotEqualTo(r2);
    }

    @Test
    void notEqualsDifferentAmount() {
        final ReservationDescriptor ld = mockDescriptor();
        final int amount = 200;
        final RecommendedReservation r1 = new RecommendedReservation(ld, amount);
        final RecommendedReservation r2 = new RecommendedReservation(ld, amount + 1);
        assertThat(r1).isNotEqualTo(r2);
    }

    @Test
    void notEqualsDifferentJavaType() {
        final RecommendedReservation r1 = new RecommendedReservation(mockDescriptor(), 200);
        assertThat(r1).isNotEqualTo(r1.toString());
    }
}
