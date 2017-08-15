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

package com.github.triceo.robozonky.app.investing;

import java.time.OffsetDateTime;
import java.time.temporal.TemporalAmount;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import com.github.triceo.robozonky.api.Refreshable;
import com.github.triceo.robozonky.api.marketplaces.Marketplace;
import com.github.triceo.robozonky.api.remote.entities.Investment;
import com.github.triceo.robozonky.api.strategies.InvestmentStrategy;
import com.github.triceo.robozonky.api.strategies.LoanDescriptor;
import com.github.triceo.robozonky.app.authentication.Authenticated;
import com.github.triceo.robozonky.app.configuration.daemon.Daemon;
import com.github.triceo.robozonky.app.portfolio.Portfolio;
import com.github.triceo.robozonky.app.util.DaemonRuntimeExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Investing implements Daemon {

    private static final Logger LOGGER = LoggerFactory.getLogger(Investing.class);

    private final Authenticated authenticated;
    private final Refreshable<InvestmentStrategy> refreshableStrategy;
    private final Marketplace marketplace;
    private final AtomicReference<OffsetDateTime> lastRunDateTime = new AtomicReference<>(null);
    private final ResultTracker buffer = new ResultTracker();

    public Investing(final Authenticated auth, final Investor.Builder builder, final Marketplace marketplace,
                     final Refreshable<InvestmentStrategy> strategy, final TemporalAmount maximumSleepPeriod) {
        this.authenticated = auth;
        this.refreshableStrategy = strategy;
        this.marketplace = marketplace;
        marketplace.registerListener((loans) -> {
            final Collection<LoanDescriptor> descriptors = buffer.acceptLoansFromMarketplace(loans);
            final Function<Collection<LoanDescriptor>, Collection<Investment>> investor =
                    new StrategyExecution(builder, refreshableStrategy, authenticated, maximumSleepPeriod);
            final Collection<Investment> result = investor.apply(descriptors);
            buffer.acceptInvestmentsFromRobot(result);
        });
    }

    @Override
    public void run() {
        lastRunDateTime.set(OffsetDateTime.now());
        try {
            if (!Portfolio.INSTANCE.isUpdating()) {
                LOGGER.trace("Starting.");
                marketplace.run();
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
