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
import com.github.robozonky.app.portfolio.Selling;
import com.github.robozonky.util.RoboZonkyThreadFactory;
import com.github.robozonky.util.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DaemonInvestmentMode implements InvestmentMode {

    public static final AtomicReference<CountDownLatch> BLOCK_UNTIL_ZERO = new AtomicReference<>(null);
    private static final Logger LOGGER = LoggerFactory.getLogger(DaemonInvestmentMode.class);
    private static final ThreadFactory THREAD_FACTORY = new RoboZonkyThreadFactory(new ThreadGroup("rzDaemon"));
    private final boolean faultTolerant;
    private final Marketplace marketplace;
    private final List<DaemonOperation> daemons;
    private final PortfolioUpdater portfolioUpdater;

    public DaemonInvestmentMode(final Authenticated auth, final PortfolioUpdater p, final Investor.Builder builder,
                                final boolean isFaultTolerant, final Marketplace marketplace,
                                final String strategyLocation, final Duration maximumSleepPeriod,
                                final Duration primaryMarketplaceCheckPeriod,
                                final Duration secondaryMarketplaceCheckPeriod) {
        this.faultTolerant = isFaultTolerant;
        this.marketplace = marketplace;
        final boolean dryRun = builder.isDryRun();
        final StrategyProvider sp = initStrategy(strategyLocation);
        this.portfolioUpdater = p;
        p.registerDependant(new Selling(sp::getToSell, dryRun)); // run sell strategy with every portfolio update
        this.daemons = Arrays.asList(new InvestingDaemon(auth, builder, marketplace, sp::getToInvest, p,
                                                         maximumSleepPeriod, primaryMarketplaceCheckPeriod),
                                     new PurchasingDaemon(auth, sp::getToPurchase, p, maximumSleepPeriod,
                                                          secondaryMarketplaceCheckPeriod, dryRun));
    }

    static StrategyProvider initStrategy(final String strategyLocation) {
        final RefreshableStrategy strategy = new RefreshableStrategy(strategyLocation);
        final StrategyProvider sp = new StrategyProvider(); // will always have the latest parsed strategies
        strategy.registerListener(sp);
        Scheduler.inBackground().submit(strategy); // start strategy refresh after the listener was registered
        return sp;
    }

    private void executeDaemons(final ScheduledExecutorService executor) {
        IntStream.range(0, daemons.size()).boxed().forEach(daemonId -> {
            final DaemonOperation d = daemons.get(daemonId);
            final long initialDelay = daemonId * 250; // quarter second apart
            final long refreshInterval = d.getRefreshInterval().getSeconds() * 1000;
            final Runnable task = new Skippable(d, portfolioUpdater::isUpdating);
            LOGGER.trace("Scheduling {} every {} ms, starting in {} ms.", task, refreshInterval, initialDelay);
            executor.scheduleWithFixedDelay(task, initialDelay, refreshInterval, TimeUnit.MILLISECONDS);
        });
    }

    private static CountDownLatch setupCircuitBreaker() {
        final CountDownLatch circuitBreaker = BLOCK_UNTIL_ZERO.updateAndGet(l -> new CountDownLatch(1));
        Runtime.getRuntime().addShutdownHook(new DaemonShutdownHook(circuitBreaker));
        return circuitBreaker;
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
