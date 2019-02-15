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
import java.util.List;
import javax.xml.bind.annotation.XmlElement;

public class ReservationPreferences extends BaseEntity {

    private boolean reservationsEnabled;
    private List<ReservationPreference> reservationPreferences;
    private ReservationPreferences() { // fox JAXB
    }

    public ReservationPreferences(final ReservationPreference... reservationPreferences) {
        this.reservationsEnabled = reservationPreferences.length != 0;
        this.reservationPreferences = Arrays.asList(reservationPreferences);
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
    public List<ReservationPreference> getReservationPreferences() {
        return reservationPreferences;
    }
}
