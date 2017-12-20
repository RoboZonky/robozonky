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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import com.github.robozonky.api.ReturnCode;
import com.github.robozonky.api.marketplaces.Marketplace;
import com.github.robozonky.app.authentication.Authenticated;
import com.github.robozonky.app.configuration.InvestmentMode;
import com.github.robozonky.app.investing.Investor;
import com.github.robozonky.util.RoboZonkyThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DaemonInvestmentMode implements InvestmentMode {

    public static final AtomicReference<CountDownLatch> BLOCK_UNTIL_ZERO = new AtomicReference<>(null);
    private static final Logger LOGGER = LoggerFactory.getLogger(DaemonInvestmentMode.class);
    private static final ThreadFactory THREAD_FACTORY = new RoboZonkyThreadFactory(newThreadGroup("rzDaemon"));
    private final boolean faultTolerant;
    private final Marketplace marketplace;
    private final List<DaemonOperation> daemons;
    private final PortfolioUpdater portfolioUpdater;
    private final Runnable blockedAmountsUpdater;
    public DaemonInvestmentMode(final Authenticated auth, final PortfolioUpdater p, final Investor.Builder builder,
                                final boolean isFaultTolerant, final Marketplace marketplace,
                                final StrategyProvider strategyProvider, final Runnable blockedAmountsUpdater,
                                final Duration maximumSleepPeriod,
                                final Duration primaryMarketplaceCheckPeriod,
                                final Duration secondaryMarketplaceCheckPeriod) {
        this.faultTolerant = isFaultTolerant;
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

    private static CountDownLatch setupCircuitBreaker() {
        final CountDownLatch circuitBreaker = BLOCK_UNTIL_ZERO.updateAndGet(l -> new CountDownLatch(1));
        Runtime.getRuntime().addShutdownHook(new DaemonShutdownHook(circuitBreaker));
        return circuitBreaker;
    }

    private void executeDaemons(final ScheduledExecutorService executor) {
        // run investing and purchasing daemons
        IntStream.range(0, daemons.size()).boxed().forEach(daemonId -> {
            final DaemonOperation d = daemons.get(daemonId);
            final long initialDelay = daemonId * 250; // quarter second apart
            final long refreshInterval = d.getRefreshInterval().getSeconds() * 1000;
            final Runnable task = new Skippable(d, portfolioUpdater::isUpdating);
            LOGGER.trace("Scheduling {} every {} ms, starting in {} ms.", task, refreshInterval, initialDelay);
            executor.scheduleWithFixedDelay(task, initialDelay, refreshInterval, TimeUnit.MILLISECONDS);
        });
        // run portfolio update twice a day; start @ 4 a.m., add random time so that not all robots hit Zonky at once
        final long secondsUntil4am = timeUntil4am(LocalDateTime.now()).getSeconds();
        final long secondsToStartIn = secondsUntil4am + (int) (Math.random() * 1000);
        final long secondsIn12hours = Duration.ofHours(12).getSeconds();
        LOGGER.trace("Scheduling portfolio update every {} seconds, starting in {} seconds.", secondsIn12hours,
                     secondsUntil4am);
        executor.scheduleAtFixedRate(portfolioUpdater, secondsToStartIn, secondsIn12hours, TimeUnit.SECONDS);
        // also run blocked amounts update every now and then to detect changes made outside of the robot
        final long oneHourInSeconds = Duration.ofHours(1).getSeconds();
        LOGGER.trace("Scheduling blocked amounts update every {} seconds, starting in {} seconds.", oneHourInSeconds,
                     oneHourInSeconds);
        executor.scheduleAtFixedRate(blockedAmountsUpdater, oneHourInSeconds, oneHourInSeconds, TimeUnit.SECONDS);
    }

    @Override
    public ReturnCode get() {
        final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2, THREAD_FACTORY);
        try {
            // register shutdown hook that will kill the daemon threads when app shutdown is requested
            final CountDownLatch circuitBreaker = setupCircuitBreaker();
            // schedule the tasks
            executeDaemons(executor);
            // block until request to stop the app is received
            LOGGER.trace("Will wait for request to stop on {}.", circuitBreaker);
            circuitBreaker.await();
            LOGGER.trace("Request to stop received.");
            // signal the end of standard operation
            return ReturnCode.OK;
        } catch (final InterruptedException ex) { // handle unexpected runtime error
            LOGGER.error("Daemon interrupted.", ex);
            return ReturnCode.ERROR_UNEXPECTED;
        } finally {
            executor.shutdownNow();
        }
    }

    @Override
    public boolean isFaultTolerant() {
        return faultTolerant;
    }

    @Override
    public void close() throws Exception {
        LOGGER.trace("Closing marketplace.");
        this.marketplace.close();
    }
}
