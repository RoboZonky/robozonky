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

package com.github.triceo.robozonky.app.configuration.daemon;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.concurrent.CountDownLatch;

import com.github.triceo.robozonky.internal.api.Defaults;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;

public class SuddenDeathDetectionTest {

    @Test
    public void nothingTracked() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final int seconds = 2;
        final SuddenDeathDetection workaround = new SuddenDeathDetection(latch, Duration.ofSeconds(2));
        Assertions.assertThat(workaround.isSuddenDeath()).isFalse();
        workaround.run();
        Assertions.assertThat(workaround.isSuddenDeath()).isFalse();
        Thread.sleep(seconds * 2 * 1000);
        workaround.run();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(workaround.isSuddenDeath()).isFalse();
            softly.assertThat(latch.getCount()).isEqualTo(1);
        });
    }

    @Test
    public void immediateDetection() throws InterruptedException {
        final Daemon d = new Daemon() {
            @Override
            public OffsetDateTime getLastRunDateTime() {
                return OffsetDateTime.ofInstant(Instant.EPOCH, Defaults.ZONE_ID);
            }

            @Override
            public void run() {
                // nothing to do
            }
        };
        final CountDownLatch latch = new CountDownLatch(1);
        final int seconds = 2;
        final SuddenDeathDetection workaround = new SuddenDeathDetection(latch, Duration.ofSeconds(seconds), d);
        Assertions.assertThatThrownBy(workaround::run).isInstanceOf(IllegalStateException.class);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(workaround.isSuddenDeath()).isTrue();
            softly.assertThat(latch.getCount()).isEqualTo(0);
        });
    }

    @Test
    public void delayedDetection() throws InterruptedException {
        final Daemon d = new Daemon() {

            private final OffsetDateTime lastCheck = OffsetDateTime.now();

            @Override
            public OffsetDateTime getLastRunDateTime() {
                return lastCheck;
            }

            @Override
            public void run() {
                // nothing to do
            }
        };
        final CountDownLatch latch = new CountDownLatch(1);
        final int seconds = 2;
        final SuddenDeathDetection workaround = new SuddenDeathDetection(latch, Duration.ofSeconds(2), d);
        Assertions.assertThat(workaround.isSuddenDeath()).isFalse();
        workaround.run();
        Assertions.assertThat(workaround.isSuddenDeath()).isFalse();
        Thread.sleep(seconds * 2 * 1000);
        Assertions.assertThatThrownBy(workaround::run).isInstanceOf(IllegalStateException.class);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(workaround.isSuddenDeath()).isTrue();
            softly.assertThat(latch.getCount()).isEqualTo(0);
        });
    }
}
