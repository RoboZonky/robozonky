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

package com.github.triceo.robozonky.app.configuration.daemon;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAmount;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.triceo.robozonky.api.ReturnCode;
import com.github.triceo.robozonky.api.marketplaces.Marketplace;
import com.github.triceo.robozonky.app.authentication.Authenticated;
import com.github.triceo.robozonky.app.configuration.InvestmentMode;
import com.github.triceo.robozonky.app.investing.Investing;
import com.github.triceo.robozonky.app.investing.Investor;
import com.github.triceo.robozonky.app.portfolio.Portfolio;
import com.github.triceo.robozonky.app.purchasing.Purchasing;
import com.github.triceo.robozonky.app.selling.Selling;
import com.github.triceo.robozonky.util.RoboZonkyThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DaemonInvestmentMode implements InvestmentMode {

    private static final Logger LOGGER = LoggerFactory.getLogger(DaemonInvestmentMode.class);

    private static final ThreadFactory THREAD_FACTORY = new RoboZonkyThreadFactory(new ThreadGroup("rzDaemon"));
    public static final AtomicReference<CountDownLatch> BLOCK_UNTIL_ZERO =
            new AtomicReference<>(new CountDownLatch(1));

    private static TemporalAmount getSuddenDeathTimeout(final TemporalAmount periodBetweenChecks) {
        final long secondBetweenChecks = periodBetweenChecks.get(ChronoUnit.SECONDS);
        return Duration.ofMinutes(secondBetweenChecks); // multiply by 60
    }

    private final String username;
    private final boolean faultTolerant, dryRun;
    private final Marketplace marketplace;
    private final ScheduledExecutorService executor =
            Executors.newScheduledThreadPool(2, DaemonInvestmentMode.THREAD_FACTORY);
    private final TemporalAmount periodBetweenChecks;
    private final SuddenDeathDetection suddenDeath;
    private final CountDownLatch circuitBreaker;
    private final Runnable marketplaceCheck;
    private final Thread shutdownHook;

    public DaemonInvestmentMode(final Authenticated auth, final Investor.Builder builder, final boolean isFaultTolerant,
                                final Marketplace marketplace, final String strategyLocaion,
                                final TemporalAmount maximumSleepPeriod, final TemporalAmount periodBetweenChecks) {
        this.username = auth.getSecretProvider().getUsername();
        this.dryRun = builder.isDryRun();
        this.faultTolerant = isFaultTolerant;
        this.circuitBreaker =
                DaemonInvestmentMode.BLOCK_UNTIL_ZERO.updateAndGet(l -> l.getCount() == 0 ? new CountDownLatch(1) : l);
        this.shutdownHook = new DaemonShutdownHook(circuitBreaker);
        Runtime.getRuntime().addShutdownHook(shutdownHook);
        this.marketplace = marketplace;
        this.periodBetweenChecks = periodBetweenChecks;
        this.suddenDeath = new SuddenDeathDetection(circuitBreaker, getSuddenDeathTimeout(periodBetweenChecks));
        final Runnable investing = new Investing(auth, builder, marketplace,
                                                 RefreshableInvestmentStrategy.create(strategyLocaion),
                                                 maximumSleepPeriod);
        final Runnable purchasing = new Purchasing(auth, dryRun, RefreshablePurchaseStrategy.create(strategyLocaion),
                                                   maximumSleepPeriod);
        Portfolio.INSTANCE.registerUpdater(new Selling(RefreshableSellStrategy.create(strategyLocaion), dryRun));
        this.marketplaceCheck = () -> { // FIXME separate
            investing.run();
            purchasing.run();
            suddenDeath.registerMarketplaceCheck();
        };
    }

    Thread getShutdownHook() {
        return shutdownHook;
    }

    @Override
    public ReturnCode get() {
        try {
            final long checkPeriodInSeconds = this.periodBetweenChecks.get(ChronoUnit.SECONDS);
            LOGGER.debug("Scheduling marketplace checks {} seconds apart.", checkPeriodInSeconds);
            executor.scheduleWithFixedDelay(marketplaceCheck, 0, checkPeriodInSeconds, TimeUnit.SECONDS);
            executor.scheduleAtFixedRate(suddenDeath, 0, checkPeriodInSeconds, TimeUnit.SECONDS);
            LOGGER.trace("Will wait for request to stop on {}.", circuitBreaker);
            circuitBreaker.await();
            LOGGER.trace("Request to stop received.");
            if (suddenDeath.isSuddenDeath()) {
                throw new SuddenDeathException();
            }
            return ReturnCode.OK;
        } catch (final SuddenDeathException ex) {
            LOGGER.error("Thread stack traces:");
            Thread.getAllStackTraces().forEach((key, value) -> {
                LOGGER.error("Stack trace for thread {}: {}", key, Stream.of(value)
                        .map(StackTraceElement::toString)
                        .collect(Collectors.joining(System.lineSeparator())));
            });
            throw new IllegalStateException(ex);
        } catch (final Exception ex) {
            LOGGER.error("Failed executing investments.", ex);
            return ReturnCode.ERROR_UNEXPECTED;
        }
    }

    @Override
    public boolean isFaultTolerant() {
        return faultTolerant;
    }

    @Override
    public boolean isDryRun() {
        return dryRun;
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
