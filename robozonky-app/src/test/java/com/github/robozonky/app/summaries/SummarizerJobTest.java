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

package com.github.robozonky.app.summaries;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;

import com.github.robozonky.internal.api.Defaults;
import com.github.robozonky.internal.api.jobs.TenantJob;
import com.github.robozonky.test.AbstractMinimalRoboZonkyTest;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class SummarizerJobTest extends AbstractMinimalRoboZonkyTest {

    private final TenantJob summarizer = new SummarizerJob();

    @Test
    void basics() {
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(summarizer.payload()).isInstanceOf(Summarizer.class);
            softly.assertThat(summarizer.prioritize()).isFalse();
            softly.assertThat(summarizer.killIn()).isGreaterThanOrEqualTo(Duration.ofHours(1));
            softly.assertThat(summarizer.repeatEvery()).isEqualTo(Duration.ofDays(7));
        });
    }

    @Test
    void beforeTuesday6am() {
        final Instant tuesdayEarlyMorning = LocalDateTime.of(2019, 1, 1, 5, 0)
                .atZone(Defaults.ZONE_ID)
                .toInstant();
        setClock(Clock.fixed(tuesdayEarlyMorning, Defaults.ZONE_ID));
        final Duration untilTuesday6am = summarizer.startIn();
        assertThat(untilTuesday6am).isEqualTo(Duration.ofHours(1));
    }

    @Test
    void afterTuesday6am() {
        final Instant tuesdayLaterMorning = LocalDateTime.of(2019, 1, 1, 7, 0)
                .atZone(Defaults.ZONE_ID)
                .toInstant();
        setClock(Clock.fixed(tuesdayLaterMorning, Defaults.ZONE_ID));
        final Duration untilNextTuesday6am = summarizer.startIn();
        assertThat(untilNextTuesday6am).isEqualTo(Duration.ofDays(7).minusHours(1));
    }
}
