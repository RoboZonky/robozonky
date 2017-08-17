/*
 * Copyright 2017 The RoboZonky Project
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

package com.github.triceo.robozonky.app.portfolio;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Objects;
import java.util.Optional;

import com.github.triceo.robozonky.internal.api.Defaults;

/**
 * Represents one occasion on which a loan, represented by {@link #getParent()}, was overdue.
 */
public final class Delinquency {

    private final LocalDate detectedOn;
    private final Delinquent parent;
    private LocalDate fixedOn;

    /**
     * Create a delinquency which is active, ie. instalment is currently overdue.
     * @param d The delinquent loan in question.
     * @param detectedOn The day that this delinquency was noticed.
     */
    Delinquency(final Delinquent d, final LocalDate detectedOn) {
        this(d, detectedOn, null);
    }

    /**
     * Create a delinquency which is inactive, repaid.
     * @param d The delinquent loan in question.
     * @param detectedOn The day that this delinquency was noticed.
     * @param fixedOn The day that the outstanding instalment was paid back.
     */
    Delinquency(final Delinquent d, final LocalDate detectedOn, final LocalDate fixedOn) {
        this.parent = d;
        this.detectedOn = detectedOn;
        this.fixedOn = fixedOn;
    }

    /**
     * @return The loan that this delinquent instalment was part of.
     */
    public Delinquent getParent() {
        return parent;
    }

    /**
     * @return The day that this delinquency was first noticed.
     */
    public LocalDate getDetectedOn() {
        return detectedOn;
    }

    /**
     * @return The day that the outstanding instalment was paid back, or empty if delinquency is active.
     */
    public Optional<LocalDate> getFixedOn() {
        return Optional.ofNullable(fixedOn);
    }

    /**
     * De-activate the delinquency.
     * @param fixedOn The day that the outstanding instalment was paid back.
     */
    public void setFixedOn(final LocalDate fixedOn) {
        this.fixedOn = fixedOn;
    }

    /**
     * @return How long it took for the loan to be delinquent for this instalment. If active, the end date is
     * calculated as today.
     */
    public Duration getDuration() {
        final ZoneId zone = Defaults.ZONE_ID;
        return getFixedOn()
                .map(fixedOn -> Duration.between(detectedOn.atStartOfDay(zone), fixedOn.atStartOfDay(zone)))
                .orElse(Duration.between(detectedOn.atStartOfDay(zone), LocalDate.now().atStartOfDay(zone)));
    }

    /**
     * See {@link Object#equals(Object)}
     * @param o Other delinquency.
     * @return Deliquencies are considered equal when they share {@link #getParent()} and {@link #getDetectedOn()}..
     */
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final Delinquency that = (Delinquency) o;
        return Objects.equals(detectedOn, that.detectedOn) &&
                Objects.equals(parent, that.parent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(detectedOn, parent);
    }

    @Override
    public String toString() {
        return "Delinquency{" +
                "detectedOn=" + detectedOn +
                ", fixedOn=" + fixedOn +
                '}';
    }
}
