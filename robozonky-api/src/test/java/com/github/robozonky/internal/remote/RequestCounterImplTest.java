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

package com.github.robozonky.internal.remote;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

class RequestCounterImplTest {

    @Test
    void marking() throws InterruptedException {
        final RequestCounter counter = new RequestCounterImpl();
        assertThat(counter.count()).isEqualTo(0);
        assertThat(counter.count(Duration.ZERO)).isEqualTo(0);
        assertThat(counter.mark()).isEqualTo(0);
        Thread.sleep(0, 1); // so that the two marks don't fall in the same nano
        assertThat(counter.mark()).isEqualTo(1);
        assertSoftly(softly -> {
            softly.assertThat(counter.current()).isEqualTo(1);
            softly.assertThat(counter.count()).isEqualTo(2);
            softly.assertThat(counter.count(Duration.ofMinutes(1))).isEqualTo(2);
            softly.assertThat(counter.count(Duration.ZERO)).isEqualTo(0);
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
