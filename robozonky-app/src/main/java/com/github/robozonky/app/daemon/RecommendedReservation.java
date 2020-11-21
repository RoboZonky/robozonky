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

import java.util.Objects;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.remote.entities.Reservation;
import com.github.robozonky.api.strategies.ReservationDescriptor;
import com.github.robozonky.api.strategies.ReservationStrategy;

/**
 * Represents the decision of the {@link ReservationStrategy} to recommend a {@link Reservation} for investing.
 */
final class RecommendedReservation implements Recommended<ReservationDescriptor, Reservation> {

    private final ReservationDescriptor reservationDescriptor;

    RecommendedReservation(final ReservationDescriptor reservationDescriptor) {
        this.reservationDescriptor = reservationDescriptor;
    }

    @Override
    public ReservationDescriptor descriptor() {
        return reservationDescriptor;
    }

    @Override
    public Money amount() {
        return reservationDescriptor.item()
            .getMyReservation()
            .getReservedAmount();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !Objects.equals(getClass(), o.getClass())) {
            return false;
        }
        final RecommendedReservation that = (RecommendedReservation) o;
        return Objects.equals(reservationDescriptor, that.reservationDescriptor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(reservationDescriptor);
    }

    @Override
    public String toString() {
        return "RecommendedReservation{" +
                "reservationDescriptor=" + reservationDescriptor +
                '}';
    }
}
