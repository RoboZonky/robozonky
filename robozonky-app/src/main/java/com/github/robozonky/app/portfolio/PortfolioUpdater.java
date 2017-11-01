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
import java.time.OffsetDateTime;
import java.time.temporal.TemporalAmount;
import java.util.Optional;
import java.util.function.Supplier;

import com.github.robozonky.api.Refreshable;
import com.github.robozonky.app.authentication.Authenticated;
import com.github.robozonky.util.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PortfolioUpdater extends Refreshable<OffsetDateTime> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PortfolioUpdater.class);
    private static final TemporalAmount FOUR_HOURS = Duration.ofHours(4);
    private final Authenticated authenticated;
    private final BlockedAmountsUpdater blockedAmountsUpdater;

    public PortfolioUpdater(final Authenticated authenticated) {
        this.authenticated = authenticated;
        // register periodic blocked amounts update, so that we catch Zonky operations performed outside of the robot
        this.blockedAmountsUpdater = new BlockedAmountsUpdater(authenticated);
        final TemporalAmount oneHour = Duration.ofHours(1);
        Scheduler.inBackground().submit(blockedAmountsUpdater, oneHour, oneHour);
    }

    /**
     * Will only update once a day, after 4am. This is to make sure that the updates happen when all the overnight
     * transactions on Zonky have cleared.
     * @return
     */
    @Override
    protected Supplier<Optional<String>> getLatestSource() {
        return () -> Optional.of(Util.getYesterdayIfAfter(FOUR_HOURS).toString());
    }

    @Override
    protected Optional<OffsetDateTime> transform(final String source) {
        // don't execute blocked amounts update while the core portfolio is updating
        return Optional.of(blockedAmountsUpdater.pauseFor(x -> {
            LOGGER.info("Pausing RoboZonky in order to update internal data structures.");
            authenticated.run(Portfolio.INSTANCE::update);
            LOGGER.info("RoboZonky resumed.");
            return OffsetDateTime.now();
        }));
    }
}
