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

import java.util.Objects;
import java.util.StringJoiner;

import javax.xml.bind.annotation.XmlElement;

import com.github.robozonky.api.remote.entities.ReservationPreference;
import com.github.robozonky.api.remote.enums.LoanTermInterval;
import com.github.robozonky.api.remote.enums.Rating;

public class ReservationPreferenceImpl extends BaseEntity implements ReservationPreference {

    private LoanTermInterval loanTermInterval;
    private Rating ratingType;
    private boolean insuredOnly;

    private ReservationPreferenceImpl() {
        // for JAXB
    }

    public ReservationPreferenceImpl(final LoanTermInterval loanTermInterval, final Rating rating,
                                     final boolean insuredOnly) {
        this.loanTermInterval = loanTermInterval;
        this.ratingType = rating;
        this.insuredOnly = insuredOnly;
    }

    @Override
    @XmlElement
    public LoanTermInterval getLoanTermInterval() {
        return loanTermInterval;
    }

    @Override
    @XmlElement
    public Rating getRatingType() {
        return ratingType;
    }

    @Override
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
        final ReservationPreferenceImpl that = (ReservationPreferenceImpl) o;
        return Objects.equals(loanTermInterval, that.loanTermInterval) &&
                ratingType == that.ratingType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(loanTermInterval, ratingType);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ReservationPreferenceImpl.class.getSimpleName() + "[", "]")
            .add("insuredOnly=" + insuredOnly)
            .add("loanTermInterval=" + loanTermInterval)
            .add("ratingType=" + ratingType)
            .toString();
    }
}
