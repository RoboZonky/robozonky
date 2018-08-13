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

package com.github.robozonky.integrations.stonky;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Collections;

import com.github.robozonky.common.jobs.Job;
import com.github.robozonky.common.jobs.JobService;
import com.github.robozonky.common.jobs.Payload;
import com.github.robozonky.internal.api.Defaults;

public class StonkyJobService implements JobService {


    private enum StonkyJob implements Job {

        INSTANCE;

        @Override
        public Duration startIn() {
            final long randomSeconds = (long) (Math.random() * 1000);
            final ZonedDateTime triggerOn =
                    LocalDate.now().plusDays(1).atStartOfDay(Defaults.ZONE_ID).plusSeconds(randomSeconds);
            return Duration.between(ZonedDateTime.now(), triggerOn);
        }

        @Override
        public Duration repeatEvery() {
            return Duration.ofHours(24);
        }

        @Override
        public Payload payload() {
            return new Stonky();
        }

    }

    @Override
    public Collection<Job> getJobs() {
        return Collections.singleton(StonkyJobService.StonkyJob.INSTANCE);
    }
}
