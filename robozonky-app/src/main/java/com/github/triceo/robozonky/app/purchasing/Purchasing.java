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

package com.github.triceo.robozonky.app.purchasing;

import java.time.OffsetDateTime;
import java.time.temporal.TemporalAmount;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.github.triceo.robozonky.api.Refreshable;
import com.github.triceo.robozonky.api.remote.entities.Investment;
import com.github.triceo.robozonky.api.remote.entities.Participation;
import com.github.triceo.robozonky.api.strategies.PurchaseStrategy;
import com.github.triceo.robozonky.app.authentication.Authenticated;
import com.github.triceo.robozonky.app.configuration.daemon.Daemon;
import com.github.triceo.robozonky.app.portfolio.Portfolio;
import com.github.triceo.robozonky.app.util.DaemonRuntimeExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Purchasing implements Daemon {

    private static final Logger LOGGER = LoggerFactory.getLogger(Purchasing.class);

    private final Authenticated authenticated;
    private final AtomicReference<OffsetDateTime> lastRunDateTime = new AtomicReference<>();
    private final Function<Collection<Participation>, Collection<Investment>> investor;

    public Purchasing(final Authenticated auth, final Refreshable<PurchaseStrategy> strategy,
                      final TemporalAmount maximumSleepPeriod, final boolean isDryRun) {
        this.authenticated = auth;
        this.investor = new StrategyExecution(strategy, authenticated, maximumSleepPeriod, isDryRun);
    }

    @Override
    public void run() {
        lastRunDateTime.set(OffsetDateTime.now());
        try {
            if (!Portfolio.INSTANCE.isUpdating()) {
                LOGGER.trace("Starting.");
                authenticated.run(z -> investor.apply(z.getAvailableParticipations().collect(Collectors.toList())));
                LOGGER.trace("Finished.");
            }
        } catch (final Throwable t) {
            /*
             * We catch Throwable so that we can inform users even about errors. Sudden death detection will take
             * care of errors stopping the thread.
             */
            new DaemonRuntimeExceptionHandler().handle(t);
        }
    }

    @Override
    public OffsetDateTime getLastRunDateTime() {
        return lastRunDateTime.get();
    }
}
