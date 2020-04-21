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

import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.github.robozonky.api.remote.entities.ReservationPreference;
import com.github.robozonky.api.remote.entities.ReservationPreferences;
import com.github.robozonky.api.remote.enums.LoanTermInterval;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.internal.util.functional.Memoizer;
import com.github.robozonky.internal.util.functional.Tuple;

public class ReservationPreferencesImpl implements ReservationPreferences {

    public static boolean isEnabled(ReservationPreferences reservationPreferences) {
        return reservationPreferences.isReservationsEnabled() &&
                !reservationPreferences.getReservationPreferences()
                    .isEmpty();
    }

    public static final Supplier<ReservationPreferencesImpl> TOTAL = Memoizer.memoize(() -> {
        final ReservationPreferenceImpl[] prefs = Arrays.stream(Rating.values())
            .flatMap(r -> Arrays.stream(LoanTermInterval.values())
                .map(i -> Tuple.of(r, i)))
            .map(t -> new ReservationPreferenceImpl(t._2, t._1, false))
            .toArray(ReservationPreferenceImpl[]::new);
        return new ReservationPreferencesImpl(prefs);
    });

    private boolean reservationsEnabled;
    private Set<ReservationPreferenceImpl> reservationPreferences;

    private ReservationPreferencesImpl() {
        // fox JAXB
    }

    public ReservationPreferencesImpl(final ReservationPreferenceImpl... reservationPreferences) {
        this.reservationsEnabled = reservationPreferences.length != 0;
        this.reservationPreferences = Arrays.stream(reservationPreferences)
            .collect(Collectors.toSet());
    }

    @Override
    public boolean isReservationsEnabled() {
        return reservationsEnabled;
    }

    @Override
    public Set<ReservationPreference> getReservationPreferences() {
        return Collections.unmodifiableSet(reservationPreferences);
    }

    public void setReservationsEnabled(final boolean reservationsEnabled) {
        this.reservationsEnabled = reservationsEnabled;
    }

    public void setReservationPreferences(final Set<ReservationPreferenceImpl> reservationPreferences) {
        this.reservationPreferences = reservationPreferences;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !Objects.equals(getClass(), o.getClass())) {
            return false;
        }
        final ReservationPreferencesImpl that = (ReservationPreferencesImpl) o;
        return reservationsEnabled == that.reservationsEnabled &&
                Objects.equals(reservationPreferences, that.reservationPreferences);
    }

    @Override
    public int hashCode() {
        return Objects.hash(reservationsEnabled, reservationPreferences);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ReservationPreferencesImpl.class.getSimpleName() + "[", "]")
            .add("reservationPreferences=" + reservationPreferences)
            .add("reservationsEnabled=" + reservationsEnabled)
            .toString();
    }
}
