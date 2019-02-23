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

package com.github.robozonky.app.runtime;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import com.github.robozonky.app.events.AbstractEventLeveragingTest;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class MainControlTest extends AbstractEventLeveragingTest {

    @Test
    void operation() throws InterruptedException, ExecutionException {
        final ExecutorService e = Executors.newFixedThreadPool(1);
        final MainControl mainControl = new MainControl();
        final AtomicBoolean started = new AtomicBoolean(false);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(mainControl.getApiVersion()).isEmpty();
            softly.assertThat(mainControl.getTimestamp()).isBeforeOrEqualTo(OffsetDateTime.now());
        });
        final Future<?> f = e.submit(() -> {
            started.set(true);
            try {
                mainControl.waitUntilTriggered();
            } catch (final InterruptedException e1) {
                throw new IllegalStateException(e1);
            }
        });
        Assertions.assertTimeout(Duration.ofSeconds(5), () -> {
            while (!started.get()) {
                Thread.sleep(1);
            }
        });
        final OffsetDateTime before = OffsetDateTime.now();
        mainControl.valueUnset(null);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(mainControl.getApiVersion()).isEmpty();
            softly.assertThat(mainControl.getTimestamp()).isAfterOrEqualTo(before);
            softly.assertThatThrownBy(() -> f.get(1, TimeUnit.SECONDS))
                    .isInstanceOf(TimeoutException.class);  // nothing will happen
        });
        final OffsetDateTime before2 = OffsetDateTime.now();
        mainControl.valueSet("1.0.0");
        f.get(); // make sure task finished
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(mainControl.getApiVersion()).contains("1.0.0");
            softly.assertThat(mainControl.getTimestamp()).isAfterOrEqualTo(before2);
        });
    }
}
