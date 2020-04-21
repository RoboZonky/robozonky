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

package com.github.robozonky.internal.remote.entities;

import java.util.Collection;
import java.util.Collections;
import java.util.StringJoiner;

import com.github.robozonky.api.remote.entities.Reservation;
import com.github.robozonky.api.remote.entities.Reservations;

public class ReservationsImpl implements Reservations {

    private Collection<ReservationImpl> reservations = Collections.emptyList();

    ReservationsImpl() {
        // for JAXB
    }

    @Override
    public Collection<Reservation> getReservations() {
        return Collections.unmodifiableCollection(reservations);
    }

    public void setReservations(final Collection<ReservationImpl> reservations) {
        this.reservations = reservations;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ReservationsImpl.class.getSimpleName() + "[", "]")
            .add("reservations=" + reservations)
            .toString();
    }
}
