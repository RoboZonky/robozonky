/*
 * Copyright 2018 The RoboZonky Project
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

import com.github.robozonky.app.ReturnCode;
import com.github.robozonky.app.authentication.Tenant;
import com.github.robozonky.app.configuration.InvestmentMode;
import com.github.robozonky.app.investing.Investor;
import com.github.robozonky.app.runtime.Lifecycle;
import com.github.robozonky.common.extensions.JobServiceLoader;
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
    private final Tenant tenant;

    public DaemonInvestmentMode(final Consumer<Throwable> shutdownCall, final Tenant tenant,
                                final Investor investor, final StrategyProvider strategyProvider,
                                final Duration primaryMarketplaceCheckPeriod,
                                final Duration secondaryMarketplaceCheckPeriod) {
        this.portfolioUpdater = PortfolioUpdater.create(shutdownCall, tenant, strategyProvider::getToSell);
        this.tenant = tenant;
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

    private void scheduleDaemons(final Scheduler executor) {
        executor.run(portfolioUpdater); // first run the update
        // schedule hourly refresh
        executor.submit(portfolioUpdater, Duration.ofHours(1), Duration.ofHours(1));
        // run investing and purchasing daemons
        IntStream.range(0, daemons.length).forEach(daemonId -> {
            final DaemonOperation d = daemons[daemonId];
            final long initialDelay = daemonId * 250L; // quarter second apart
            final Runnable task = new Skippable(d, portfolioUpdater::isUpdating);
            executor.submit(task, d.getRefreshInterval(), Duration.ofMillis(initialDelay));
        });
    }

    private void scheduleJobs(final Scheduler executor) {
        JobServiceLoader.load().forEach(job -> {
            LOGGER.debug("Scheduling {}.", job);
            final Runnable payload = () -> job.payload().accept(tenant.getSecrets());
            executor.submit(payload, job.repeatEvery(), job.startIn());
        });
    }

    @Override
    public ReturnCode apply(final Lifecycle lifecycle) {
        scheduleJobs(Scheduler.inBackground());
        try (final Scheduler executor = Schedulers.INSTANCE.create(2, THREAD_FACTORY)) {
            // schedule the tasks
            scheduleDaemons(executor);
            // block until request to stop the app is received
            lifecycle.suspend();
            LOGGER.trace("Request to stop received.");
            // signal the end of standard operation
            return ReturnCode.OK;
        }
    }
}
