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

package com.github.robozonky.app.portfolio;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAmount;

import com.github.robozonky.common.AbstractStateLeveragingTest;
import com.github.robozonky.internal.api.Defaults;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;

public class DelinquencyTest extends AbstractStateLeveragingTest {

    @Test
    public void endless() {
        final ZoneId zone = Defaults.ZONE_ID;
        final LocalDate epoch = OffsetDateTime.ofInstant(Instant.EPOCH, zone).toLocalDate();
        final LocalDate now = OffsetDateTime.now(zone).toLocalDate();
        final Duration duration = Duration.between(epoch.atStartOfDay(zone), now.atStartOfDay(zone));
        final Delinquency d = new Delinquency(null, epoch);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(d.getPaymentMissedDate()).isEqualTo(epoch);
            softly.assertThat(d.getFixedOn()).isEmpty();
            softly.assertThat(d.getParent()).isNull();
            softly.assertThat(d.getDuration()).isLessThanOrEqualTo(duration);
        });
        final LocalDate fixed = now.minusDays(1);
        final TemporalAmount newDuration = Duration.between(epoch.atStartOfDay(zone), fixed.atStartOfDay(zone));
        d.setFixedOn(fixed);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(d.getFixedOn()).contains(fixed);
            softly.assertThat(d.getDuration()).isEqualTo(newDuration);
        });
    }

    @Test
    public void ended() {
        final ZoneId zone = Defaults.ZONE_ID;
        final LocalDate epoch = OffsetDateTime.ofInstant(Instant.EPOCH, zone).toLocalDate();
        final LocalDate fixed = LocalDate.now();
        final Duration daysUntilFixed = Duration.between(epoch.atStartOfDay(zone), fixed.atStartOfDay(zone));
        final Delinquency d = new Delinquency(null, epoch, fixed);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(d.getPaymentMissedDate()).isEqualTo(epoch);
            softly.assertThat(d.getFixedOn()).contains(fixed);
            softly.assertThat(d.getParent()).isNull();
            softly.assertThat(d.getDuration()).isEqualTo(daysUntilFixed);
        });
    }

    @Test
    public void equalsDifferentParent() {
        final LocalDate now = LocalDate.now();
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
        final Delinquency d1 = new Delinquency(d, LocalDate.now());
        final Delinquency d2 = new Delinquency(d, LocalDate.now().minusDays(1));
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(d1).isNotEqualTo(d2);
            softly.assertThat(d2).isNotEqualTo(d1);
        });
    }

    @Test
    public void equalsDifferentFixed() {
        final Delinquent d = new Delinquent(1);
        final LocalDate since = OffsetDateTime.ofInstant(Instant.EPOCH, Defaults.ZONE_ID).toLocalDate();
        final Delinquency d1 = new Delinquency(d, since);
        final Delinquency d2 = new Delinquency(d, since, LocalDate.now());
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(d1).isNotEqualTo(null);
            softly.assertThat(d1).isEqualTo(d1);
            softly.assertThat(d1).isEqualTo(d2);
            softly.assertThat(d2).isEqualTo(d1);
        });
    }

    @Test
    public void failsOnReset() {
        final Delinquency d = new Delinquency(new Delinquent(1), LocalDate.now(), LocalDate.now());
        Assertions.assertThatThrownBy(() -> d.setFixedOn(LocalDate.now()))
                .isInstanceOf(IllegalStateException.class);
    }
}
