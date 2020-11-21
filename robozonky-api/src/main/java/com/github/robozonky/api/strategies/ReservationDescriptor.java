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

package com.github.robozonky.api.strategies;

import java.util.Objects;
import java.util.function.Supplier;

import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.api.remote.entities.Reservation;

/**
 * Carries metadata regarding a {@link Reservation}.
 */
public final class ReservationDescriptor implements Descriptor<Reservation> {

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
    public Loan related() {
        return loanSupplier.get();
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
