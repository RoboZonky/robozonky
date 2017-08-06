/*
 * Copyright 2017 Lukáš Petrovický
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

package com.github.triceo.robozonky.app.portfolio;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;

import com.github.triceo.robozonky.internal.api.Defaults;

class Util {

    public static LocalDate getYesterdayIfAfter(final Duration timeFromMidnightToday) {
        final LocalDate now = LocalDate.now();
        final ZonedDateTime targetTimeToday = now.atStartOfDay(Defaults.ZONE_ID).plus(timeFromMidnightToday);
        return Instant.now().isAfter(targetTimeToday.toInstant()) ? now : now.minusDays(1);
    }
}
