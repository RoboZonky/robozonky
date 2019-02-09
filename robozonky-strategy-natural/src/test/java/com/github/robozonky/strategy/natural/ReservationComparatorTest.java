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

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Comparator;

import com.github.robozonky.api.remote.entities.sanitized.Reservation;
import com.github.robozonky.api.strategies.ReservationDescriptor;
import com.github.robozonky.internal.api.Defaults;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

class ReservationComparatorTest {

    private final Comparator<ReservationDescriptor> c = new ReservationComparator();

    private static Reservation mockReservation(final OffsetDateTime published) {
        return mockReservation(published, false);
    }

    private static Reservation mockReservation(final OffsetDateTime published, final boolean insured) {
        return Reservation.custom()
                .setDatePublished(published)
                .setInsuranceActive(insured)
                .build();
    }

    @Test
    void sortByInsurance() {
        final OffsetDateTime first = OffsetDateTime.ofInstant(Instant.EPOCH, Defaults.ZONE_ID);
        final Reservation l1 = mockReservation(first, true);
        final Reservation l2 = mockReservation(first, !l1.isInsuranceActive());
        final ReservationDescriptor ld1 = new ReservationDescriptor(l1, () -> null),
                ld2 = new ReservationDescriptor(l2, () -> null);
        assertSoftly(softly -> {
            softly.assertThat(c.compare(ld1, ld2)).isEqualTo(-1);
            softly.assertThat(c.compare(ld2, ld1)).isEqualTo(1);
            softly.assertThat(c.compare(ld1, ld1)).isEqualTo(0);
        });
    }

    @Test
    void sortByRecencyIfInsured() {
        final OffsetDateTime first = OffsetDateTime.ofInstant(Instant.EPOCH, Defaults.ZONE_ID);
        final OffsetDateTime second = first.plus(Duration.ofMillis(1));
        final Reservation l1 = mockReservation(first);
        final Reservation l2 = mockReservation(second);
        final ReservationDescriptor ld1 = new ReservationDescriptor(l1, () -> null),
                ld2 = new ReservationDescriptor(l2, () -> null);
        assertSoftly(softly -> {
            softly.assertThat(c.compare(ld1, ld2)).isEqualTo(-1);
            softly.assertThat(c.compare(ld2, ld1)).isEqualTo(1);
            softly.assertThat(c.compare(ld1, ld1)).isEqualTo(0);
        });
    }
}
