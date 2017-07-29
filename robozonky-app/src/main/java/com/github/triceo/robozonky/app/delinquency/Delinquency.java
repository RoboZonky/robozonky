/*
 * Copyright 2017 Lukáš Petrovický
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

package com.github.triceo.robozonky.app.delinquency;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.TemporalAmount;
import java.util.Objects;
import java.util.Optional;

public class Delinquency {

    private static TemporalAmount difference(final OffsetDateTime start, final OffsetDateTime end) {
        final long startInstant = Instant.from(start).toEpochMilli();
        final long endInstant = Instant.from(end).toEpochMilli();
        final long difference = endInstant - startInstant;
        return Duration.ofMillis(difference);
    }

    private final OffsetDateTime detectedOn;
    private OffsetDateTime fixedOn;
    private final Delinquent parent;

    Delinquency(final Delinquent d, final OffsetDateTime detectedOn) {
        this(d, detectedOn, null);
    }

    Delinquency(final Delinquent d, final OffsetDateTime detectedOn, final OffsetDateTime fixedOn) {
        this.parent = d;
        this.detectedOn = detectedOn;
        this.fixedOn = fixedOn;
    }

    public Delinquent getParent() {
        return parent;
    }

    public OffsetDateTime getDetectedOn() {
        return detectedOn;
    }

    public Optional<OffsetDateTime> getFixedOn() {
        return Optional.ofNullable(fixedOn);
    }

    public void setFixedOn(final OffsetDateTime fixedOn) {
        this.fixedOn = fixedOn;
    }

    public TemporalAmount getDuration() {
        return getFixedOn()
                .map(fixedOn -> difference(detectedOn, fixedOn))
                .orElse(difference(detectedOn, OffsetDateTime.now()));
    }

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
