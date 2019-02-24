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

package com.github.robozonky.common.jobs;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

class JobTest {

    @Test
    void defaults() {
        final Job j = new MyJob();
        assertSoftly(softly -> {
            softly.assertThat(j.startIn()).isGreaterThanOrEqualTo(Duration.ZERO);
            softly.assertThat(j.repeatEvery()).isEqualTo(Duration.ZERO);
            softly.assertThat(j.killIn()).isGreaterThan(Duration.ZERO);
            softly.assertThat(j.prioritize()).isFalse();
        });
    }

    private static final class MyJob implements Job {

        @Override
        public Duration repeatEvery() {
            return Duration.ZERO;
        }
    }

}
