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
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import com.github.robozonky.api.remote.entities.RawReservation;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.remote.entities.sanitized.MarketplaceLoan;
import com.github.robozonky.api.remote.entities.sanitized.Reservation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Carries metadata regarding a {@link RawReservation}.
 */
public final class ReservationDescriptor
        implements Descriptor<RecommendedReservation, ReservationDescriptor, Reservation> {

    private static final Logger LOGGER = LogManager.getLogger(ReservationDescriptor.class);

    private final Reservation reservation;
    private final Supplier<Loan> loanSupplier;

    public ReservationDescriptor(final Reservation reservation, final Supplier<Loan> loanSupplier) {
        this.reservation = reservation;
        this.loanSupplier = loanSupplier;
    }

    @Override
    public Reservation item() {
        return reservation;
    }

    @Override
    public MarketplaceLoan related() {
        return loanSupplier.get();
    }

    @Override
    public Optional<RecommendedReservation> recommend(final BigDecimal amount) {
        final int actual = reservation.getMyReservation().getReservedAmount();
        if (amount.intValue() == actual) {
            return Optional.of(new RecommendedReservation(this, amount.intValue()));
        } else {
            LOGGER.warn("Requested reservation of {} CZK while only worth {} CZK. ", amount, actual);
            return Optional.empty();
        }
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !Objects.equals(getClass(), o.getClass())) {
            return false;
        }
        final ReservationDescriptor that = (ReservationDescriptor) o;
        return Objects.equals(reservation, that.reservation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(reservation);
    }

    @Override
    public String toString() {
        return "ReservationDescriptor{" +
                "reservation=" + reservation +
                '}';
    }
}
