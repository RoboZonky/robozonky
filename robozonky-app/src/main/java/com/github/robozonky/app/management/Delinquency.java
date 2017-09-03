/*
 * Copyright 2017 The RoboZonky Project
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

package com.github.robozonky.app.management;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.robozonky.app.portfolio.Delinquents;
import com.github.robozonky.internal.api.Defaults;

class Delinquency implements DelinquencyMBean {

    private final Delinquents source;

    public Delinquency() {
        this(Delinquents.INSTANCE);
    }

    // for testing purposes only
    protected Delinquency(final Delinquents delinquents) {
        this.source = delinquents;
    }

    @Override
    public OffsetDateTime getLatestUpdatedDateTime() {
        return source.getLastUpdateTimestamp();
    }

    @Override
    public Map<Integer, LocalDate> getAll() {
        return getOlderThan(0);
    }

    /**
     * Returns all loans that are presently delinquent.
     * @param days Minimum number of days a loan is in delinquency in order to be reported. 0 returns all delinquents.
     * @return All loans that are delinquent for more than the given number of days.
     */
    private Map<Integer, LocalDate> getOlderThan(final int days) {
        final ZoneId zone = Defaults.ZONE_ID;
        final ZonedDateTime now = LocalDate.now().atStartOfDay(zone);
        return source.getDelinquents().stream()
                .flatMap(d -> d.getActiveDelinquency().map(Stream::of).orElse(Stream.empty()))
                .filter(d -> d.getPaymentMissedDate().atStartOfDay(zone).plusDays(days).isBefore(now))
                .collect(Collectors.toMap(d -> d.getParent().getLoanId(),
                                          com.github.robozonky.app.portfolio.Delinquency::getPaymentMissedDate));
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
