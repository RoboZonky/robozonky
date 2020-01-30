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

package com.github.robozonky.internal.remote;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import com.github.robozonky.internal.Defaults;
import com.github.robozonky.internal.test.DateUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

class RequestCounterImplTest {

    @BeforeEach
    @AfterEach
    void resetClock() {
        DateUtil.resetSystemClock();
    }

    @Test
    void marking() {
        DateUtil.setSystemClock(Clock.fixed(Instant.EPOCH, Defaults.ZONE_ID));
        final RequestCounter counter = new RequestCounterImpl();
        assertThat(counter.count()).isEqualTo(0);
        assertThat(counter.count(Duration.ZERO)).isEqualTo(0);
        assertThat(counter.mark()).isEqualTo(0);
        DateUtil.setSystemClock(Clock.fixed(Instant.EPOCH.plus(Duration.ofMillis(1)), Defaults.ZONE_ID));
        assertThat(counter.mark()).isEqualTo(1);
        assertSoftly(softly -> {
            softly.assertThat(counter.current()).isEqualTo(1);
            softly.assertThat(counter.count()).isEqualTo(2);
            softly.assertThat(counter.count(Duration.ofMinutes(1))).isEqualTo(2);
            softly.assertThat(counter.count(Duration.ZERO)).isEqualTo(1);
        });
        counter.keepOnly(Duration.ZERO); // clear everything
        assertSoftly(softly -> {
            softly.assertThat(counter.mark()).isEqualTo(2); // id still continues where it started
            softly.assertThat(counter.count()).isEqualTo(1);
        });
        counter.cut(1);
        assertThat(counter.count()).isEqualTo(0);
    }

}
