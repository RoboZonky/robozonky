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

package com.github.robozonky.app.purchasing;

import java.time.temporal.TemporalAmount;
import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Stream;

import com.github.robozonky.api.Refreshable;
import com.github.robozonky.api.remote.entities.Investment;
import com.github.robozonky.api.remote.entities.Participation;
import com.github.robozonky.api.strategies.PurchaseStrategy;
import com.github.robozonky.app.authentication.Authenticated;
import com.github.robozonky.app.portfolio.Portfolio;
import com.github.robozonky.app.util.DaemonRuntimeExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Purchasing implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(Purchasing.class);

    private final Authenticated api;
    private final Function<Stream<Participation>, Collection<Investment>> investor;

    public Purchasing(final Authenticated auth, final Refreshable<PurchaseStrategy> strategy,
                      final TemporalAmount maximumSleepPeriod, final boolean isDryRun) {
        this.api = auth;
        this.investor = new StrategyExecution(strategy, api, maximumSleepPeriod, isDryRun);
    }

    @Override
    public void run() {
        if (Portfolio.INSTANCE.isUpdating()) { // don't update while reading information about portfolio
            return;
        }
        try {
            LOGGER.trace("Starting.");
            final Collection<Investment> bought = api.call(z -> investor.apply(z.getAvailableParticipations()));
            LOGGER.trace("Finished; investments made: {}.", bought);
        } catch (final Throwable t) {
            // We catch Throwable so that we can inform users even about errors.
            new DaemonRuntimeExceptionHandler().handle(t);
        }
    }
}
