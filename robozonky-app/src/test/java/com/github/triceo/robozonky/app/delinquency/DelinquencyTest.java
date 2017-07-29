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

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

import com.github.triceo.robozonky.internal.api.Defaults;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;

public class DelinquencyTest {

    @Test
    public void endless() {
        final OffsetDateTime epoch = OffsetDateTime.ofInstant(Instant.EPOCH, Defaults.ZONE_ID);
        final long secondsUntilNow = epoch.toInstant().until(OffsetDateTime.now(), ChronoUnit.SECONDS);
        final Delinquency d = new Delinquency(null, epoch);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(d.getDetectedOn()).isEqualTo(epoch);
            softly.assertThat(d.getFixedOn()).isEmpty();
            softly.assertThat(d.getParent()).isNull();
            softly.assertThat(d.getDuration().get(ChronoUnit.SECONDS)).isGreaterThanOrEqualTo(secondsUntilNow);
        });
        final OffsetDateTime fixed = LocalDate.now().atStartOfDay(Defaults.ZONE_ID).toOffsetDateTime();
        final long secondsUntilFixed = epoch.toInstant().until(fixed, ChronoUnit.SECONDS);
        d.setFixedOn(fixed);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(d.getFixedOn()).contains(fixed);
            softly.assertThat(d.getDuration().get(ChronoUnit.SECONDS)).isEqualTo(secondsUntilFixed);
        });
    }

    @Test
    public void ended() {
        final OffsetDateTime epoch = OffsetDateTime.ofInstant(Instant.EPOCH, Defaults.ZONE_ID);
        final OffsetDateTime fixed = LocalDate.now().atStartOfDay(Defaults.ZONE_ID).toOffsetDateTime();
        final long secondsUntilFixed = epoch.toInstant().until(fixed, ChronoUnit.SECONDS);
        final Delinquency d = new Delinquency(null, epoch, fixed);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(d.getDetectedOn()).isEqualTo(epoch);
            softly.assertThat(d.getFixedOn()).contains(fixed);
            softly.assertThat(d.getParent()).isNull();
            softly.assertThat(d.getDuration().get(ChronoUnit.SECONDS)).isEqualTo(secondsUntilFixed);
        });
    }

    @Test
    public void equalsDifferentParent() {
        final OffsetDateTime now = OffsetDateTime.now();
        final Delinquency d1 = new Delinquency(null, now);
        final Delinquency d2 = new Delinquency(new Delinquent(1), now);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(d1).isNotEqualTo(d2);
            softly.assertThat(d2).isNotEqualTo(d1);
        });
    }

    @Test
    public void equalsDifferentSince() {
        final Delinquent d = new Delinquent(1);
        final Delinquency d1 = new Delinquency(d, OffsetDateTime.now());
        final Delinquency d2 = new Delinquency(d, LocalDate.now().atStartOfDay(Defaults.ZONE_ID).toOffsetDateTime());
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(d1).isNotEqualTo(d2);
            softly.assertThat(d2).isNotEqualTo(d1);
        });
    }

    @Test
    public void equalsDifferentFixed() {
        final Delinquent d = new Delinquent(1);
        final OffsetDateTime since = OffsetDateTime.ofInstant(Instant.EPOCH, Defaults.ZONE_ID);
        final Delinquency d1 = new Delinquency(d, since);
        final Delinquency d2 = new Delinquency(d, since, OffsetDateTime.now());
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(d1).isNotEqualTo(null);
            softly.assertThat(d1).isEqualTo(d1);
            softly.assertThat(d1).isEqualTo(d2);
            softly.assertThat(d2).isEqualTo(d1);
        });
    }
}
