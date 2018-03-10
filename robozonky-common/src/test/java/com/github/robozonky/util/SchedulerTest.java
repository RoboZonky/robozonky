/*
 * Copyright 2017 The RoboZonky Project
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

package com.github.robozonky.util;

import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class SchedulerTest {

    private static final Refreshable<String> REFRESHABLE = Refreshable.createImmutable("");

    @Test
    void lifecycle() {
        try (final Scheduler s = Schedulers.INSTANCE.create()) {
            assertThat(s.isSubmitted(REFRESHABLE)).isFalse();
            final ScheduledFuture<?> f = s.submit(REFRESHABLE);
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(Scheduler.inBackground()).isNotNull();
                softly.assertThat((Future) f).isNotNull();
                softly.assertThat(s.isSubmitted(REFRESHABLE)).isTrue();
            });
        }
    }
}
