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
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.github.robozonky.app.authentication.Authenticated;
import com.github.robozonky.app.util.DaemonRuntimeExceptionHandler;
import com.github.robozonky.util.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PortfolioUpdater implements Runnable,
                                         Supplier<Optional<Portfolio>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PortfolioUpdater.class);
    private final Authenticated authenticated;
    private final BlockedAmountsUpdater blockedAmountsUpdater;
    private final AtomicReference<Portfolio> portfolio = new AtomicReference<>();
    private final Set<PortfolioBased> updaters = new CopyOnWriteArraySet<>();

    public PortfolioUpdater(final Authenticated authenticated) {
        this.authenticated = authenticated;
        // register periodic blocked amounts update, so that we catch Zonky operations performed outside of the robot
        this.blockedAmountsUpdater = new BlockedAmountsUpdater(authenticated, this);
        final TemporalAmount oneHour = Duration.ofHours(1);
        Scheduler.inBackground().submit(blockedAmountsUpdater, oneHour, oneHour);
    }

    private Stream<PortfolioBased> getUpdaters() {
        /*
         * core updaters to get the full picture. checks for blocked amounts immediately after every update of the
         * portfolio, also checks for delinquencies.
         */
        final Stream<PortfolioBased> core = Stream.of(BlockedAmounts.INSTANCE, Delinquents.INSTANCE);
        final Stream<PortfolioBased> external = updaters.stream();
        return Stream.concat(core, external);
    }

    public void registerUpdater(final PortfolioBased updater) {
        LOGGER.debug("Registering dependent: {}.", updater);
        updaters.add(updater);
    }

    @Override
    public void run() {
        try {
            // don't execute blocked amounts update while the core portfolio is updating
            blockedAmountsUpdater.pauseFor(x -> {
                LOGGER.info("Pausing RoboZonky in order to update internal data structures.");
                portfolio.set(authenticated.call(z -> Portfolio.create(z, getUpdaters())).orElse(null));
                LOGGER.info("RoboZonky resumed.");
                return OffsetDateTime.now();
            });
        } catch (final Throwable t) {
            new DaemonRuntimeExceptionHandler().handle(t);
        }
    }

    @Override
    public Optional<Portfolio> get() {
        return Optional.ofNullable(portfolio.get());
    }
}
