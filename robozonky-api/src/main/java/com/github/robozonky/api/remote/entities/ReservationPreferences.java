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

package com.github.robozonky.api.remote.entities;

import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.xml.bind.annotation.XmlElement;

import com.github.robozonky.api.remote.enums.LoanTermInterval;
import com.github.robozonky.api.remote.enums.Rating;
import io.vavr.Lazy;
import io.vavr.Tuple;

public class ReservationPreferences extends BaseEntity {

    public static final Supplier<ReservationPreferences> TOTAL = Lazy.of(() -> {
        final ReservationPreference[] prefs = Arrays.stream(Rating.values())
                .flatMap(r -> Arrays.stream(LoanTermInterval.values()).map(i -> Tuple.of(r, i)))
                .map(t -> new ReservationPreference(t._2, t._1, false))
                .toArray(ReservationPreference[]::new);
        return new ReservationPreferences(prefs);
    });

    private boolean reservationsEnabled;
    private Set<ReservationPreference> reservationPreferences;

    private ReservationPreferences() { // fox JAXB
    }

    public ReservationPreferences(final ReservationPreference... reservationPreferences) {
        this.reservationsEnabled = reservationPreferences.length != 0;
        this.reservationPreferences = Arrays.stream(reservationPreferences).collect(Collectors.toSet());
    }

    public static boolean isEnabled(final ReservationPreferences reservationPreferences) {
        return reservationPreferences.isReservationsEnabled() &&
                !reservationPreferences.getReservationPreferences().isEmpty();
    }

    @XmlElement
    public boolean isReservationsEnabled() {
        return reservationsEnabled;
    }

    @XmlElement
    public Set<ReservationPreference> getReservationPreferences() {
        return reservationPreferences;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !Objects.equals(getClass(), o.getClass())) {
            return false;
        }
        final ReservationPreferences that = (ReservationPreferences) o;
        return reservationsEnabled == that.reservationsEnabled &&
                Objects.equals(reservationPreferences, that.reservationPreferences);
    }

    @Override
    public int hashCode() {
        return Objects.hash(reservationsEnabled, reservationPreferences);
    }
}
