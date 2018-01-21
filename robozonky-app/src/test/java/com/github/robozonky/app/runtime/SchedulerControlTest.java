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

import java.time.OffsetDateTime;
import java.util.UUID;

import com.github.robozonky.util.Scheduler;
import com.github.robozonky.util.Schedulers;
import org.assertj.core.api.Assertions;
import org.junit.Test;

public class SchedulerControlTest {

    @Test
    public void check() {
        final ApiVersion v = new ApiVersion("master", UUID.randomUUID().toString(), UUID.randomUUID().toString(),
                                            OffsetDateTime.now(), "1.0.0");
        try (final Scheduler s = Schedulers.INSTANCE.create()) {
            final SchedulerControl rc = new SchedulerControl();
            rc.valueUnset(null);
            Assertions.assertThat(s.isPaused()).isTrue();
            rc.valueSet(v);
            Assertions.assertThat(s.isPaused()).isFalse();
        }
    }
}
