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

package com.github.robozonky.app.runtime;

import com.github.robozonky.util.Scheduler;
import com.github.robozonky.util.Schedulers;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SchedulerControlTest {

    @Test
    void check() {
        try (final Scheduler s = Schedulers.INSTANCE.create()) {
            final SchedulerControl rc = new SchedulerControl();
            rc.valueUnset(null);
            assertThat(s.isPaused()).isTrue();
            rc.valueSet("1.0.0");
            assertThat(s.isPaused()).isFalse();
        }
    }
}
