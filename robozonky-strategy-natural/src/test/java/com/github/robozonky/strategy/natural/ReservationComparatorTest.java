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

package com.github.robozonky.strategy.natural;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.util.Comparator;

import org.junit.jupiter.api.Test;

import com.github.robozonky.api.remote.entities.Reservation;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.ReservationDescriptor;
import com.github.robozonky.internal.remote.entities.ReservationImpl;
import com.github.robozonky.test.mock.MockReservationBuilder;

class ReservationComparatorTest {

    private final Comparator<ReservationDescriptor> c = new ReservationComparator(Rating::compareTo);

    @Test
    void sortByRating() {
        final Reservation l1 = new MockReservationBuilder()
            .set(ReservationImpl::setRating, Rating.D)
            .build();
        final Reservation l2 = new MockReservationBuilder()
            .set(ReservationImpl::setRating, Rating.A)
            .build();
        final ReservationDescriptor ld1 = new ReservationDescriptor(l1, () -> null),
                ld2 = new ReservationDescriptor(l2, () -> null);
        assertSoftly(softly -> {
            softly.assertThat(c.compare(ld1, ld2))
                .isGreaterThan(0);
            softly.assertThat(c.compare(ld2, ld1))
                .isLessThan(0);
            softly.assertThat(c.compare(ld1, ld1))
                .isEqualTo(0);
        });
    }

}
