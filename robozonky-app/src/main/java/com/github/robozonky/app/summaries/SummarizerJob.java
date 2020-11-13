/*
 * Copyright 2020 The RoboZonky Project
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
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;

import com.github.robozonky.internal.jobs.TenantJob;
import com.github.robozonky.internal.jobs.TenantPayload;
import com.github.robozonky.internal.test.DateUtil;
import com.github.robozonky.internal.test.RandomUtil;

final class SummarizerJob implements TenantJob {

    @Override
    public TenantPayload payload() {
        return new Summarizer();
    }

    @Override
    public Duration startIn() {
        /*
         * Trigger next Sunday, around 6am.
         * Sunday is chosen as there are no large amounts of transactions expected.
         * 6am is important, since it's long after midnight, when Zonky recalculates.
         * Triggering any such code during recalculation is likely to bring some strange inconsistent values.
         */
        var date = DateUtil.zonedNow()
            .with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
            .withHour(6)
            .truncatedTo(ChronoUnit.HOURS);
        var untilSunday6am = Duration.between(DateUtil.zonedNow(), date);
        var untilNextSunday6am = untilSunday6am.isNegative() ? untilSunday6am.plusDays(7) : untilSunday6am;
        var loadBalancingRandomMinutesOffset = RandomUtil.getNextInt(30) - 15;
        return untilNextSunday6am.plusMinutes(loadBalancingRandomMinutesOffset);
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
