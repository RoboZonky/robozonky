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

import java.util.Objects;
import javax.xml.bind.annotation.XmlElement;

import com.github.robozonky.api.remote.enums.LoanTermInterval;
import com.github.robozonky.api.remote.enums.Rating;

public class ReservationPreference extends BaseEntity {

    private LoanTermInterval loanTermInterval;
    private Rating ratingType;
    private boolean insuredOnly;

    private ReservationPreference() {
        // for JAXB
    }

    public ReservationPreference(final LoanTermInterval loanTermInterval, final Rating rating,
                                 final boolean insuredOnly) {
        this.loanTermInterval = loanTermInterval;
        this.ratingType = rating;
        this.insuredOnly = insuredOnly;
    }

    @XmlElement
    public LoanTermInterval getLoanTermInterval() {
        return loanTermInterval;
    }

    @XmlElement
    public Rating getRatingType() {
        return ratingType;
    }

    @XmlElement
    public boolean isInsuredOnly() {
        return insuredOnly;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !Objects.equals(getClass(), o.getClass())) {
            return false;
        }
        final ReservationPreference that = (ReservationPreference) o;
        return Objects.equals(loanTermInterval, that.loanTermInterval) &&
                ratingType == that.ratingType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(loanTermInterval, ratingType);
    }
}
