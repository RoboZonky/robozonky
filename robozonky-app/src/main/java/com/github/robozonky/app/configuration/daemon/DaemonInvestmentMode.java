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
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadFactory;
import java.util.stream.IntStream;

import com.github.robozonky.api.ReturnCode;
import com.github.robozonky.api.marketplaces.Marketplace;
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
    private final Marketplace marketplace;
    private final List<DaemonOperation> daemons;
    private final PortfolioUpdater portfolioUpdater;
    private final Runnable blockedAmountsUpdater;

    public DaemonInvestmentMode(final Authenticated auth, final PortfolioUpdater p, final Investor.Builder builder,
                                final Marketplace marketplace,
                                final StrategyProvider strategyProvider, final Runnable blockedAmountsUpdater,
                                final Duration maximumSleepPeriod, final Duration primaryMarketplaceCheckPeriod,
                                final Duration secondaryMarketplaceCheckPeriod) {
        this.marketplace = marketplace;
        this.portfolioUpdater = p;
        this.blockedAmountsUpdater = blockedAmountsUpdater;
        this.daemons = Arrays.asList(new InvestingDaemon(auth, builder, marketplace, strategyProvider::getToInvest, p,
                                                         maximumSleepPeriod, primaryMarketplaceCheckPeriod),
                                     new PurchasingDaemon(auth, strategyProvider::getToPurchase, p, maximumSleepPeriod,
                                                          secondaryMarketplaceCheckPeriod, builder.isDryRun()));
    }

    private static ThreadGroup newThreadGroup(final String name) {
        final ThreadGroup threadGroup = new ThreadGroup(name);
        threadGroup.setMaxPriority(Thread.NORM_PRIORITY + 1); // these threads should be a bit more important
        threadGroup.setDaemon(true); // no thread from this group shall block shutdown
        return threadGroup;
    }

    private static LocalDateTime getNextFourAM(final LocalDateTime now) {
        final LocalDateTime fourAM = LocalTime.of(4, 0).atDate(now.toLocalDate());
        if (fourAM.isAfter(now)) {
            return fourAM;
        }
        return fourAM.plusDays(1);
    }

    private static Duration timeUntil4am(final LocalDateTime now) {
        final LocalDateTime nextFourAm = getNextFourAM(now);
        return Duration.between(now, nextFourAm);
    }

    private void executeDaemons(final Scheduler executor) {
        // run portfolio update twice a day
        executor.submit(portfolioUpdater, Duration.ofHours(12));
        // also run blocked amounts update every now and then to detect changes made outside of the robot
        final Duration oneHour = Duration.ofHours(1);
        executor.submit(blockedAmountsUpdater, oneHour, oneHour);
        // run investing and purchasing daemons
        IntStream.range(0, daemons.size()).boxed().forEach(daemonId -> {
            final DaemonOperation d = daemons.get(daemonId);
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

    @Override
    public void close() throws Exception {
        LOGGER.trace("Closing marketplace.");
        this.marketplace.close();
    }
}
