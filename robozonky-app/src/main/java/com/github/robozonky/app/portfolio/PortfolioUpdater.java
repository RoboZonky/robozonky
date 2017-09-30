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

package com.github.robozonky.app.portfolio;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.TemporalAmount;
import java.util.Optional;
import java.util.function.Supplier;

import com.github.robozonky.api.Refreshable;
import com.github.robozonky.app.authentication.Authenticated;
import com.github.robozonky.internal.api.Defaults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PortfolioUpdater extends Refreshable<OffsetDateTime> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PortfolioUpdater.class);
    private static final TemporalAmount FOUR_HOURS = Duration.ofHours(4);
    private final Authenticated authenticated;

    public PortfolioUpdater(final Authenticated authenticated) {
        this.authenticated = authenticated;
        Portfolio.INSTANCE.registerUpdater(Delinquents.INSTANCE);
    }

    private static LocalDate getYesterdayIfAfter(final TemporalAmount timeFromMidnightToday) {
        final Instant now = Instant.now();
        final LocalDate today = now.atZone(Defaults.ZONE_ID).toLocalDate();
        final Instant targetTimeToday = today.atStartOfDay(Defaults.ZONE_ID).plus(timeFromMidnightToday).toInstant();
        return now.isAfter(targetTimeToday) ? today : today.minusDays(1);
    }

    /**
     * Will only update once a day, after 4am. This is to make sure that the updates happen when all the overnight
     * transactions on Zonky have cleared.
     * @return
     */
    @Override
    protected Supplier<Optional<String>> getLatestSource() {
        return () -> Optional.of(getYesterdayIfAfter(FOUR_HOURS).toString());
    }

    @Override
    protected Optional<OffsetDateTime> transform(final String source) {
        LOGGER.info("Pausing RoboZonky in order to update internal data structures.");
        authenticated.run(Portfolio.INSTANCE::update);
        LOGGER.info("RoboZonky resumed.");
        return Optional.of(OffsetDateTime.now());
    }
}
