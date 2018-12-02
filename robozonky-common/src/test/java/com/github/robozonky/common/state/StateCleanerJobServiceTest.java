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

package com.github.robozonky.common.state;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import com.github.robozonky.common.jobs.TenantJob;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StateCleanerJobServiceTest {

    @Test
    void provide() {
        final StateCleanerJobService s = new StateCleanerJobService();
        final List<TenantJob> jobs = new ArrayList<>(s.getTenantJobs());
        assertThat(jobs).hasSize(1);
        assertThat(s.getSimpleJobs()).isEmpty();
        final TenantJob job = jobs.get(0);
        assertThat(job.startIn()).isEqualTo(Duration.ZERO);
        assertThat(job.repeatEvery()).isEqualTo(Duration.ofDays(1));
        assertThat(job.payload()).isInstanceOf(StateCleaner.class);
    }

}
