/*
 * Copyright 2018 The RoboZonky Project
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

package com.github.robozonky.internal.util;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;

import com.github.robozonky.internal.api.Defaults;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DateUtilTest extends AbstractMinimalRoboZonkyTest {

    private static final Instant INSTANT = Instant.EPOCH;

    @BeforeEach
    void replaceClock() {
        setClock(Clock.fixed(INSTANT, Defaults.ZONE_ID)); // all dates will be based on this instant
    }

    @Test
    void replacesWithSynthetic() {
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(DateUtil.now()).isEqualTo(INSTANT);
            softly.assertThat(DateUtil.localNow()).isEqualTo(LocalDateTime.ofInstant(INSTANT, Defaults.ZONE_ID));
            softly.assertThat(DateUtil.offsetNow()).isEqualTo(OffsetDateTime.ofInstant(INSTANT, Defaults.ZONE_ID));
            softly.assertThat(DateUtil.zonedNow()).isEqualTo(ZonedDateTime.ofInstant(INSTANT, Defaults.ZONE_ID));
        });
    }

}
