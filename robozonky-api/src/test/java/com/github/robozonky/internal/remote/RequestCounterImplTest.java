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

import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

class RequestCounterImplTest {

    @Test
    void markSingle() {
        final RequestCounter counter = new RequestCounterImpl();
        Assertions.assertThat(counter.count()).isEqualTo(0);
        Assertions.assertThat(counter.count(Duration.ZERO)).isEqualTo(0);
        Assertions.assertThat(counter.mark()).isEqualTo(0);
        Assertions.assertThat(counter.mark()).isEqualTo(1);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(counter.current()).isEqualTo(1);
            softly.assertThat(counter.count()).isEqualTo(2);
            softly.assertThat(counter.count(Duration.ofMinutes(1))).isEqualTo(2);
            softly.assertThat(counter.count(Duration.ZERO)).isEqualTo(0);
        });
        counter.keepOnly(Duration.ZERO); // clear everything
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(counter.mark()).isEqualTo(2); // id still continues where it started
            softly.assertThat(counter.count()).isEqualTo(1);
        });
    }

    @Test
    void markMultiple() {
        final RequestCounter counter = new RequestCounterImpl();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(counter.mark(10)).isEqualTo(9);
            softly.assertThat(counter.count()).isEqualTo(10);
            softly.assertThat(counter.count(Duration.ofSeconds(10))).isEqualTo(10);
        });
    }

}
