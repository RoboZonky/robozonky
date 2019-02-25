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

package com.github.robozonky.common.async;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

class FailedScheduledFutureTest {

    @Test
    void getters() {
        final ScheduledFuture<?> f = new FailedScheduledFuture<>(new IllegalStateException());
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(f.getDelay(TimeUnit.NANOSECONDS)).isEqualTo(0);
            softly.assertThat(f.isCancelled()).isFalse();
            softly.assertThat(f.isDone()).isTrue();
            softly.assertThat(f.cancel(true)).isFalse();
            softly.assertThatThrownBy(f::get)
                    .isInstanceOf(ExecutionException.class)
                    .hasCauseInstanceOf(IllegalStateException.class);
            softly.assertThatThrownBy(() -> f.get(0, TimeUnit.NANOSECONDS))
                    .isInstanceOf(ExecutionException.class)
                    .hasCauseInstanceOf(IllegalStateException.class);
        });
    }
}
