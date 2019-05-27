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

package com.github.robozonky.app.summaries;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;

import com.github.robozonky.internal.api.jobs.TenantJob;
import com.github.robozonky.internal.api.jobs.TenantPayload;
import com.github.robozonky.internal.test.DateUtil;

final class SummarizerJob implements TenantJob {

    @Override
    public TenantPayload payload() {
        return new Summarizer();
    }

    @Override
    public Duration startIn() {
        /*
         * trigger next tuesday, 6am; tuesday as it's the first day after Zonky's weekend recalc; 6am is important,
         * since it's long after midnight, when Zonky recalculates. triggering any such code during recalculation is
         * likely to bring some strange inconsistent values.
         */
        final LocalDateTime date = DateUtil.localNow()
                .withHour(6)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)
                .with(TemporalAdjusters.nextOrSame(DayOfWeek.TUESDAY));
        final Duration d = Duration.between(DateUtil.localNow(), date);
        return d.isNegative() ? d.plusDays(7) : d;
    }

    @Override
    public Duration killIn() {
        return Duration.ofHours(1); // could potentially run for a very long time
    }

    @Override
    public Duration repeatEvery() {
        return Duration.ofDays(7);
    }
}
