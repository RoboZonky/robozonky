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

package com.github.robozonky.app.tenant;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import com.github.robozonky.internal.Defaults;
import com.github.robozonky.internal.remote.ApiProvider;
import com.github.robozonky.internal.remote.RequestCounter;
import com.github.robozonky.test.AbstractRoboZonkyTest;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class QuotaMonitorTest extends AbstractRoboZonkyTest {

    private final ApiProvider api = new ApiProvider();
    private final RequestCounter counter = api.getRequestCounter().orElseThrow();
    private final QuotaMonitor monitor = new QuotaMonitor(counter);

    @Test
    void emptyAtStart() {
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(monitor.threshold50PercentReached()).isFalse();
            softly.assertThat(monitor.threshold75PercentReached()).isFalse();
            softly.assertThat(monitor.threshold90PercentReached()).isFalse();
            softly.assertThat(monitor.threshold99PercentReached()).isFalse();
        });
    }

    @Test
    void runsWhenEmpty() {
        add(0);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(monitor.threshold50PercentReached()).isFalse();
            softly.assertThat(monitor.threshold75PercentReached()).isFalse();
            softly.assertThat(monitor.threshold90PercentReached()).isFalse();
            softly.assertThat(monitor.threshold99PercentReached()).isFalse();
        });
    }

    private void add(final int addend) {
        if (addend > 0) {
            for (int i = 0; i < addend; i++) {
                counter.mark();
                try {
                    Thread.sleep(0, 1); // different Instant, or else marks will be overwritten
                } catch (final InterruptedException ex) {
                    // cannot do anything
                }
            }
        } else {
            counter.cut(-addend);
        }
        monitor.run();
    }

    @Test
    void reaches50() {
        add(1500);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(monitor.threshold50PercentReached()).isTrue();
            softly.assertThat(monitor.threshold75PercentReached()).isFalse();
            softly.assertThat(monitor.threshold90PercentReached()).isFalse();
            softly.assertThat(monitor.threshold99PercentReached()).isFalse();
        });
        add(-1);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(monitor.threshold50PercentReached()).isFalse();
            softly.assertThat(monitor.threshold75PercentReached()).isFalse();
            softly.assertThat(monitor.threshold90PercentReached()).isFalse();
            softly.assertThat(monitor.threshold99PercentReached()).isFalse();
        });
    }

    @Test
    void reaches75() {
        add(2250);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(monitor.threshold50PercentReached()).isTrue();
            softly.assertThat(monitor.threshold75PercentReached()).isTrue();
            softly.assertThat(monitor.threshold90PercentReached()).isFalse();
            softly.assertThat(monitor.threshold99PercentReached()).isFalse();
        });
        add(-1);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(monitor.threshold50PercentReached()).isTrue();
            softly.assertThat(monitor.threshold75PercentReached()).isFalse();
            softly.assertThat(monitor.threshold90PercentReached()).isFalse();
            softly.assertThat(monitor.threshold99PercentReached()).isFalse();
        });
    }

    @Test
    void reaches90() {
        add(2700);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(monitor.threshold50PercentReached()).isTrue();
            softly.assertThat(monitor.threshold75PercentReached()).isTrue();
            softly.assertThat(monitor.threshold90PercentReached()).isTrue();
            softly.assertThat(monitor.threshold99PercentReached()).isFalse();
        });
        add(-1);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(monitor.threshold50PercentReached()).isTrue();
            softly.assertThat(monitor.threshold75PercentReached()).isTrue();
            softly.assertThat(monitor.threshold90PercentReached()).isFalse();
            softly.assertThat(monitor.threshold99PercentReached()).isFalse();
        });
    }

    @Test
    void reaches99() {
        add(2970);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(monitor.threshold50PercentReached()).isTrue();
            softly.assertThat(monitor.threshold75PercentReached()).isTrue();
            softly.assertThat(monitor.threshold90PercentReached()).isTrue();
            softly.assertThat(monitor.threshold99PercentReached()).isTrue();
        });
        add(-1);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(monitor.threshold50PercentReached()).isTrue();
            softly.assertThat(monitor.threshold75PercentReached()).isTrue();
            softly.assertThat(monitor.threshold90PercentReached()).isTrue();
            softly.assertThat(monitor.threshold99PercentReached()).isFalse();
        });
    }

    @Test
    void preventMemoryLeaks() {
        add(10);
        setClock(Clock.fixed(Instant.now().plus(Duration.ofDays(1)), Defaults.ZONE_ID));
        add(1);
        assertThat(counter.count()).isEqualTo(1);
    }
}
