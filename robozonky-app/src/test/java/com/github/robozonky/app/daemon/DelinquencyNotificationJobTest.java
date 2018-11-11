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

package com.github.robozonky.app.daemon;

import java.time.Duration;

import com.github.robozonky.common.jobs.TenantJob;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

class DelinquencyNotificationJobTest {

    @Test
    void test() {
        final TenantJob job = new DelinquencyNotificationJob();
        assertSoftly(softly -> {
            softly.assertThat(job.payload()).isInstanceOf(DelinquencyNotificationPayload.class);
            softly.assertThat(job.repeatEvery()).isEqualTo(Duration.ofHours(12));
        });
    }
}
