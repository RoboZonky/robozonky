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
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.robozonky.api.ReturnCode;
import com.github.robozonky.api.marketplaces.Marketplace;
import com.github.robozonky.app.authentication.Authenticated;
import com.github.robozonky.app.configuration.InvestmentMode;
import com.github.robozonky.app.investing.Investor;
import com.github.robozonky.app.portfolio.Portfolio;
import com.github.robozonky.app.portfolio.Selling;
import com.github.robozonky.util.RoboZonkyThreadFactory;
import com.github.robozonky.util.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DaemonInvestmentMode implements InvestmentMode {

    public static final AtomicReference<CountDownLatch> BLOCK_UNTIL_ZERO = new AtomicReference<>(new CountDownLatch(1));
    private static final Logger LOGGER = LoggerFactory.getLogger(DaemonInvestmentMode.class);
    private static final ThreadFactory THREAD_FACTORY = new RoboZonkyThreadFactory(new ThreadGroup("rzDaemon"));
    private final String username;
    private final boolean faultTolerant;
    private final Marketplace marketplace;
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2, THREAD_FACTORY);
    private final Collection<DaemonOperation> daemons;
    private final CountDownLatch circuitBreaker;

    static StrategyProvider initStrategy(final String strategyLocation) {
        final RefreshableStrategy strategy = new RefreshableStrategy(strategyLocation);
        final StrategyProvider sp = new StrategyProvider(); // will always have the latest parsed strategies
        strategy.registerListener(sp);
        Scheduler.inBackground().submit(strategy); // start strategy refresh after the listener was registered
        return sp;
    }

    public DaemonInvestmentMode(final Authenticated auth, final Investor.Builder builder, final boolean isFaultTolerant,
                                final Marketplace marketplace, final String strategyLocation,
                                final Duration maximumSleepPeriod, final Duration primaryMarketplaceCheckPeriod,
                                final Duration secondaryMarketplaceCheckPeriod) {
        this.username = auth.getSecretProvider().getUsername();
        this.faultTolerant = isFaultTolerant;
        this.circuitBreaker = BLOCK_UNTIL_ZERO.updateAndGet(l -> l.getCount() == 0 ? new CountDownLatch(1) : l);
        Runtime.getRuntime().addShutdownHook(new DaemonShutdownHook(circuitBreaker));
        this.marketplace = marketplace;
        final boolean dryRun = builder.isDryRun();
        final StrategyProvider sp = initStrategy(strategyLocation);
        Portfolio.INSTANCE.registerUpdater(new Selling(sp::getToSell, dryRun));
        final Supplier<Optional<Portfolio>> p = () -> Optional.of(Portfolio.INSTANCE);
        this.daemons = Arrays.asList(new InvestingDaemon(auth, builder, marketplace, sp::getToInvest, p,
                                                         maximumSleepPeriod, primaryMarketplaceCheckPeriod),
                                     new PurchasingDaemon(auth, sp::getToPurchase, p, maximumSleepPeriod,
                                                          secondaryMarketplaceCheckPeriod, dryRun));
    }

    @Override
    public ReturnCode get() {
        try {
            // schedule the tasks some time apart so that the CPU is evenly utilized
            final LongAdder daemonCount = new LongAdder();
            daemons.forEach(d -> {
                final long refreshInterval = d.getRefreshInterval().getSeconds() * 1000;
                final long initialDelay = daemonCount.sum() * 250; // schedule daemons quarter second apart
                LOGGER.trace("Scheduling {} every {} ms, starting in {} ms.", d, refreshInterval, initialDelay);
                executor.scheduleWithFixedDelay(d, initialDelay, refreshInterval, TimeUnit.MILLISECONDS);
                daemonCount.increment(); // increment the counter so that the next daemon is scheduled with
            });
            // block until request to stop the app is received
            LOGGER.trace("Will wait for request to stop on {}.", circuitBreaker);
            circuitBreaker.await();
            LOGGER.trace("Request to stop received.");
            return ReturnCode.OK;
        } catch (final InterruptedException ex) { // handle unexpected runtime error
            LOGGER.error("Thread stack traces:");
            Thread.getAllStackTraces().forEach((key, value) -> {
                LOGGER.error("Stack trace for thread {}: {}", key, Stream.of(value)
                        .map(StackTraceElement::toString)
                        .collect(Collectors.joining(System.lineSeparator())));
            });
            throw new IllegalStateException(ex);
        }
    }

    @Override
    public boolean isFaultTolerant() {
        return faultTolerant;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public void close() throws Exception {
        LOGGER.trace("Shutting down executor.");
        this.executor.shutdownNow();
        LOGGER.trace("Closing marketplace.");
        this.marketplace.close();
    }
}
