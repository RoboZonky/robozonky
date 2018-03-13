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
import java.util.concurrent.ThreadFactory;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import com.github.robozonky.api.ReturnCode;
import com.github.robozonky.app.authentication.Authenticated;
import com.github.robozonky.app.configuration.InvestmentMode;
import com.github.robozonky.app.investing.Investor;
import com.github.robozonky.app.runtime.Lifecycle;
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

    public DaemonInvestmentMode(final Consumer<Throwable> shutdownCall, final Authenticated auth,
                                final PortfolioUpdater p, final Investor.Builder builder,
                                final StrategyProvider strategyProvider, final Duration primaryMarketplaceCheckPeriod,
                                final Duration secondaryMarketplaceCheckPeriod) {
        this.portfolioUpdater = p;
        this.daemons = new DaemonOperation[]{
                new InvestingDaemon(shutdownCall, auth, builder, strategyProvider::getToInvest, p,
                                    primaryMarketplaceCheckPeriod),
                new PurchasingDaemon(shutdownCall, auth, strategyProvider::getToPurchase, p,
                                     secondaryMarketplaceCheckPeriod, builder.isDryRun())
        };
    }

    private static ThreadGroup newThreadGroup(final String name) {
        final ThreadGroup threadGroup = new ThreadGroup(name);
        threadGroup.setMaxPriority(Thread.NORM_PRIORITY + 1); // these threads should be a bit more important
        threadGroup.setDaemon(true); // no thread from this group shall block shutdown
        return threadGroup;
    }

    private void executeDaemons(final Scheduler executor) {
        // run portfolio update twice a day
        executor.submit(portfolioUpdater, Duration.ofHours(12));
        // also run blocked amounts update every now and then to detect changes made outside of the robot
        final Duration oneHour = Duration.ofHours(1);
        executor.submit(portfolioUpdater.getBlockedAmountsUpdater(), oneHour, oneHour);
        // run investing and purchasing daemons
        IntStream.range(0, daemons.length).forEach(daemonId -> {
            final DaemonOperation d = daemons[daemonId];
            final long initialDelay = daemonId * 250; // quarter second apart
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
