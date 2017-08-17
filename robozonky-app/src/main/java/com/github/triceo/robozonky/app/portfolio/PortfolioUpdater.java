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

package com.github.triceo.robozonky.app.portfolio;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.function.Supplier;

import com.github.triceo.robozonky.api.Refreshable;
import com.github.triceo.robozonky.app.authentication.Authenticated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PortfolioUpdater extends Refreshable<OffsetDateTime> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PortfolioUpdater.class);

    private final Authenticated authenticated;

    public PortfolioUpdater(final Authenticated authenticated) {
        this.authenticated = authenticated;
    }

    /**
     * Will only update once a day, after 4am. This is to make sure that the updates happen when all the overnight
     * transactions on Zonky have cleared.
     * @return
     */
    @Override
    protected Supplier<Optional<String>> getLatestSource() {
        return () -> Optional.of(Util.getYesterdayIfAfter(Duration.ofHours(4)).toString());
    }

    @Override
    protected Optional<OffsetDateTime> transform(final String source) {
        LOGGER.info("Pausing RoboZonky in order to update internal data structures.");
        authenticated.run(Portfolio.INSTANCE::update);
        LOGGER.info("RoboZonky resumed.");
        return Optional.of(OffsetDateTime.now());
    }
}
