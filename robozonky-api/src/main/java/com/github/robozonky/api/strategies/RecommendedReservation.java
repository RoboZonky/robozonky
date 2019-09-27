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
import com.github.robozonky.api.remote.entities.Reservation;

import java.util.Objects;

/**
 * Represents the decision of the {@link ReservationStrategy} to recommend a {@link Reservation} for investing.
 */
public final class RecommendedReservation
        implements Recommended<RecommendedReservation, ReservationDescriptor, Reservation> {

    private final ReservationDescriptor reservationDescriptor;
    private final Money recommendedInvestment;

    RecommendedReservation(final ReservationDescriptor reservationDescriptor, final Money amount) {
        if (reservationDescriptor == null) {
            throw new IllegalArgumentException("Reservation descriptor must not be null.");
        }
        this.reservationDescriptor = reservationDescriptor;
        this.recommendedInvestment = amount;
    }

    @Override
    public ReservationDescriptor descriptor() {
        return reservationDescriptor;
    }

    @Override
    public Money amount() {
        return recommendedInvestment;
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
        return Objects.equals(recommendedInvestment, that.recommendedInvestment) &&
                Objects.equals(reservationDescriptor, that.reservationDescriptor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(reservationDescriptor, recommendedInvestment);
    }

    @Override
    public String toString() {
        return "RecommendedReservation{" +
                "recommendedInvestmentAmount=" + recommendedInvestment +
                ", reservationDescriptor=" + reservationDescriptor +
                '}';
    }
}
