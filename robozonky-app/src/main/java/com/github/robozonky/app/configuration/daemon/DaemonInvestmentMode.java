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

import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAmount;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.robozonky.api.ReturnCode;
import com.github.robozonky.api.marketplaces.Marketplace;
import com.github.robozonky.app.authentication.Authenticated;
import com.github.robozonky.app.configuration.InvestmentMode;
import com.github.robozonky.app.investing.Investing;
import com.github.robozonky.app.investing.Investor;
import com.github.robozonky.app.portfolio.Portfolio;
import com.github.robozonky.app.portfolio.Selling;
import com.github.robozonky.app.purchasing.Purchasing;
import com.github.robozonky.util.RoboZonkyThreadFactory;
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
    private final TemporalAmount periodBetweenChecks;
    private final Collection<Runnable> daemons;
    private final CountDownLatch circuitBreaker;

    public DaemonInvestmentMode(final Authenticated auth, final Investor.Builder builder, final boolean isFaultTolerant,
                                final Marketplace marketplace, final String strategyLocaion,
                                final TemporalAmount maximumSleepPeriod, final TemporalAmount periodBetweenChecks) {
        this.username = auth.getSecretProvider().getUsername();
        this.faultTolerant = isFaultTolerant;
        this.circuitBreaker = BLOCK_UNTIL_ZERO.updateAndGet(l -> l.getCount() == 0 ? new CountDownLatch(1) : l);
        Runtime.getRuntime().addShutdownHook(new DaemonShutdownHook(circuitBreaker));
        this.marketplace = marketplace;
        this.periodBetweenChecks = periodBetweenChecks;
        final boolean dryRun = builder.isDryRun();
        Portfolio.INSTANCE.registerUpdater(new Selling(RefreshableSellStrategy.create(strategyLocaion), dryRun));
        this.daemons = Arrays.asList(new Investing(auth, builder,
                                                   marketplace, RefreshableInvestmentStrategy.create(strategyLocaion),
                                                   maximumSleepPeriod),
                                     new Purchasing(auth, RefreshablePurchaseStrategy.create(strategyLocaion),
                                                    maximumSleepPeriod, dryRun));
    }

    static Map<Runnable, Long> getDelays(final Collection<Runnable> daemons, final long checkPeriodInSeconds) {
        final Map<Runnable, Long> result = new LinkedHashMap<>(daemons.size());
        final long delay = (checkPeriodInSeconds * 1000) / daemons.size();
        long currentDelay = checkPeriodInSeconds * 1000;
        for (final Runnable d : daemons) {
            result.put(d, currentDelay);
            currentDelay -= delay;
        }
        return result;
    }

    @Override
    public ReturnCode get() {
        try {
            final long checkPeriodInSeconds = this.periodBetweenChecks.get(ChronoUnit.SECONDS);
            LOGGER.debug("Scheduling marketplace checks {} seconds apart.", checkPeriodInSeconds);
            // schedule the tasks some time apart so that the CPU is evenly utilized
            getDelays(daemons, checkPeriodInSeconds).forEach((daemon, delayInMillis) -> {
                LOGGER.trace("Scheduling {}.", daemon);
                executor.scheduleWithFixedDelay(daemon, delayInMillis, checkPeriodInSeconds * 1000,
                                                TimeUnit.MILLISECONDS);
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
