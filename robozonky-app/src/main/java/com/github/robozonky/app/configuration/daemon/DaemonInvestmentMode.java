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

package com.github.robozonky.app.configuration.daemon;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ThreadFactory;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import com.github.robozonky.app.ReturnCode;
import com.github.robozonky.app.authentication.Tenant;
import com.github.robozonky.app.configuration.InvestmentMode;
import com.github.robozonky.app.investing.Investor;
import com.github.robozonky.app.runtime.Lifecycle;
import com.github.robozonky.internal.api.Defaults;
import com.github.robozonky.util.RoboZonkyThreadFactory;
import com.github.robozonky.util.Scheduler;
import com.github.robozonky.util.Schedulers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DaemonInvestmentMode implements InvestmentMode {

    private static final Logger LOGGER = LoggerFactory.getLogger(DaemonInvestmentMode.class);
    private static final ThreadFactory THREAD_FACTORY = new RoboZonkyThreadFactory(newThreadGroup("rzDaemon"));
    private final DaemonOperation[] daemons;
    private final PortfolioUpdater portfolioUpdater;
    private final Runnable transactionsUpdate;

    public DaemonInvestmentMode(final Consumer<Throwable> shutdownCall, final Tenant tenant,
                                final Investor investor, final StrategyProvider strategyProvider,
                                final Duration primaryMarketplaceCheckPeriod,
                                final Duration secondaryMarketplaceCheckPeriod) {
        this.portfolioUpdater = PortfolioUpdater.create(shutdownCall, tenant, strategyProvider::getToSell);
        this.transactionsUpdate = () -> portfolioUpdater.get().ifPresent(folio -> folio.updateTransactions(tenant));
        this.daemons = new DaemonOperation[]{
                new InvestingDaemon(shutdownCall, tenant, investor, strategyProvider::getToInvest, portfolioUpdater,
                                    primaryMarketplaceCheckPeriod),
                new PurchasingDaemon(shutdownCall, tenant, strategyProvider::getToPurchase, portfolioUpdater,
                                     secondaryMarketplaceCheckPeriod)
        };
    }

    private static ThreadGroup newThreadGroup(final String name) {
        final ThreadGroup threadGroup = new ThreadGroup(name);
        threadGroup.setMaxPriority(Thread.NORM_PRIORITY + 1); // these threads should be a bit more important
        threadGroup.setDaemon(true); // no thread from this group shall block shutdown
        return threadGroup;
    }

    private static Duration getUntilNextOddHour() {
        return getUntilNextOddHour(Instant.now().atZone(Defaults.ZONE_ID));
    }

    static Duration getUntilNextOddHour(final ZonedDateTime now) {
        final int hourDifference = (now.getHour() % 2) + 1; // 2 hours if odd, 1 hour if even
        final ZonedDateTime nextOddHour = now.plusHours(hourDifference).truncatedTo(ChronoUnit.HOURS);
        return Duration.between(now, nextOddHour).abs();
    }

    private void executeDaemons(final Scheduler executor) {
        executor.run(portfolioUpdater); // first run the update
        // then schedule a refresh once per day, after the Zonky refresh
        executor.submit(portfolioUpdater, Duration.ofHours(2), getUntilNextOddHour());
        // also run transactions update every now and then to detect changes made outside of the robot
        final Duration oneHour = Duration.ofHours(1);
        executor.submit(transactionsUpdate, oneHour, oneHour);
        // run investing and purchasing daemons
        IntStream.range(0, daemons.length).forEach(daemonId -> {
            final DaemonOperation d = daemons[daemonId];
            final long initialDelay = daemonId * 250L; // quarter second apart
            final Runnable task = new Skippable(d, portfolioUpdater::isUpdating);
            executor.submit(task, d.getRefreshInterval(), Duration.ofMillis(initialDelay));
        });
    }

    @Override
    public ReturnCode apply(final Lifecycle lifecycle) {
        try (final Scheduler executor = Schedulers.INSTANCE.create(2, THREAD_FACTORY)) {
            // schedule the tasks
            executeDaemons(executor);
            // block until request to stop the app is received
            lifecycle.suspend();
            LOGGER.trace("Request to stop received.");
            // signal the end of standard operation
            return ReturnCode.OK;
        }
    }
}
