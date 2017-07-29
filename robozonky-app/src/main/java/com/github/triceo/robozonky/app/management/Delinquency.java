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

package com.github.triceo.robozonky.app.management;

import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.github.triceo.robozonky.api.notifications.Event;
import com.github.triceo.robozonky.api.notifications.LoanNoLongerDelinquentEvent;
import com.github.triceo.robozonky.api.notifications.LoanNowDelinquentEvent;
import com.github.triceo.robozonky.internal.api.Defaults;

class Delinquency implements DelinquencyMBean {

    private final SortedMap<Integer, LocalDate> delinquents = new TreeMap<>();
    private OffsetDateTime lastInvestmentRunTimestamp;

    @Override
    public OffsetDateTime getLatestUpdatedDateTime() {
        return this.lastInvestmentRunTimestamp;
    }

    void registerRun(final LoanNowDelinquentEvent event) {
        delinquents.put(event.getLoan().getId(), event.getDelinquentSince());
        registerRun((Event) event);
    }

    void registerRun(final LoanNoLongerDelinquentEvent event) {
        delinquents.remove(event.getLoan().getId());
        registerRun((Event) event);
    }

    private void registerRun(final Event event) {
        this.lastInvestmentRunTimestamp = event.getCreatedOn();
    }

    @Override
    public Map<Integer, LocalDate> getAll() {
        return getOlderThan(0);
    }

    private Map<Integer, LocalDate> getOlderThan(final int thresholdInDays) {
        final ZoneId zone = Defaults.ZONE_ID;
        final ZonedDateTime now = LocalDate.now().atStartOfDay(zone);
        final Duration expected = Duration.ofDays(thresholdInDays);
        return delinquents.entrySet().stream()
                .filter(e -> {
                    final ZonedDateTime since = e.getValue().atStartOfDay(zone);
                    final Duration actual = Duration.between(since, now);
                    return (actual.compareTo(expected) >= 0);
                }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public Map<Integer, LocalDate> get10Plus() {
        return getOlderThan(10);
    }

    @Override
    public Map<Integer, LocalDate> get30Plus() {
        return getOlderThan(30);
    }

    @Override
    public Map<Integer, LocalDate> get60Plus() {
        return getOlderThan(60);
    }

    @Override
    public Map<Integer, LocalDate> get90Plus() {
        return getOlderThan(90);
    }
}
