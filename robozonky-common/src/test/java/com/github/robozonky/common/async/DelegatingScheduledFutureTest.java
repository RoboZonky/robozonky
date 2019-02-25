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

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class DelegatingScheduledFutureTest {

    @Test
    void getters() {
        final DelegatingScheduledFuture<?> f = new DelegatingScheduledFuture<>();
        final ScheduledFuture<?> f2 = mock(ScheduledFuture.class);
        f.setCurrent(f2);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(f.getDelay(TimeUnit.NANOSECONDS)).isEqualTo(0);
            softly.assertThat(f.isCancelled()).isFalse();
            softly.assertThat(f.isDone()).isFalse();
            softly.assertThat(f.cancel(true)).isFalse();
            softly.assertThat(f.compareTo(f2)).isEqualTo(0);
        });
    }
}
