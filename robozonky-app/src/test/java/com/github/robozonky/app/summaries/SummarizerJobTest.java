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

package com.github.robozonky.app.summaries;

import static org.assertj.core.api.Assertions.*;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

import com.github.robozonky.internal.Defaults;
import com.github.robozonky.internal.jobs.TenantJob;
import com.github.robozonky.test.AbstractMinimalRoboZonkyTest;

class SummarizerJobTest extends AbstractMinimalRoboZonkyTest {

    private final TenantJob summarizer = new SummarizerJob();

    @Test
    void basics() {
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(summarizer.payload())
                .isInstanceOf(Summarizer.class);
            softly.assertThat(summarizer.killIn())
                .isGreaterThanOrEqualTo(Duration.ofHours(1));
            softly.assertThat(summarizer.repeatEvery())
                .isEqualTo(Duration.ofDays(7));
        });
    }

    @Test
    void beforeSunday6am() {
        final Instant sundayEarlyMorning = LocalDateTime.of(2020, 1, 5, 5, 0)
            .atZone(Defaults.ZONE_ID)
            .toInstant();
        setClock(Clock.fixed(sundayEarlyMorning, Defaults.ZONE_ID));
        final Duration untilSundayAround6am = summarizer.startIn();
        assertThat(untilSundayAround6am).isBetween(Duration.ofMinutes(-45), Duration.ofMinutes(75));
    }

    @Test
    void afterSunday6am() {
        final Instant sundayLaterMorning = LocalDateTime.of(2020, 1, 5, 7, 0)
            .atZone(Defaults.ZONE_ID)
            .toInstant();
        setClock(Clock.fixed(sundayLaterMorning, Defaults.ZONE_ID));
        final Duration untilNextSundayAround6am = summarizer.startIn();
        assertThat(untilNextSundayAround6am)
            .isBetween(Duration.ofDays(7)
                .minusMinutes(75),
                    Duration.ofDays(7)
                        .minusMinutes(45));
    }
}
