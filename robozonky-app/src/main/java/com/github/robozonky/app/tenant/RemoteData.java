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

package com.github.robozonky.app.tenant;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.remote.entities.Statistics;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.internal.Defaults;
import com.github.robozonky.internal.remote.Select;
import com.github.robozonky.internal.remote.Zonky;
import com.github.robozonky.internal.tenant.Tenant;
import com.github.robozonky.internal.test.DateUtil;
import com.github.robozonky.internal.util.functional.Tuple;
import com.github.robozonky.internal.util.functional.Tuple2;

final class RemoteData {

    private static final Logger LOGGER = LogManager.getLogger(RemoteData.class);

    private final Statistics statistics;
    private final Map<Integer, Tuple2<Rating, Money>> blocked;
    private final OffsetDateTime retrievedOn = DateUtil.offsetNow();

    private RemoteData(final Statistics statistics, final Map<Integer, Tuple2<Rating, Money>> blocked) {
        this.statistics = statistics;
        this.blocked = blocked;
    }

    public static RemoteData load(final Tenant tenant) {
        LOGGER.debug("Loading the latest Zonky portfolio information.");
        final Statistics statistics = tenant.call(Zonky::getStatistics);
        final Map<Integer, Tuple2<Rating, Money>> blocked = getAmountsBlocked(tenant);
        LOGGER.debug("Finished.");
        return new RemoteData(statistics, blocked);
    }

    static Map<Integer, Tuple2<Rating, Money>> getAmountsBlocked(final Tenant tenant) {
        final Select select = new Select()
            .lessThanOrNull("activeFrom", Instant.EPOCH.atZone(Defaults.ZONE_ID)
                .toOffsetDateTime());
        return tenant.call(zonky -> zonky.getInvestments(select))
            .peek(investment -> LOGGER.debug("Found: {}.", investment))
            .collect(Collectors.toMap(i -> i.getLoan()
                .getId(),
                    i -> Tuple.of(i.getLoan()
                        .getRating(),
                            i.getPrincipal()
                                .getUnpaid())));
    }

    public OffsetDateTime getRetrievedOn() {
        return retrievedOn;
    }

    public Statistics getStatistics() {
        return statistics;
    }

    public Map<Integer, Tuple2<Rating, Money>> getBlocked() {
        return blocked;
    }

    @Override
    public String toString() {
        return "RemoteData{" +
                "blocked=" + blocked +
                ", retrievedOn=" + retrievedOn +
                ", statistics=" + statistics +
                '}';
    }
}
