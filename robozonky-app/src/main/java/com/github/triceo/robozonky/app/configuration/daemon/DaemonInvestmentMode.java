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

import com.github.triceo.robozonky.api.ReturnCode;
import com.github.triceo.robozonky.api.marketplaces.Marketplace;
import com.github.triceo.robozonky.app.authentication.Authenticated;
import com.github.triceo.robozonky.app.configuration.InvestmentMode;
import com.github.triceo.robozonky.app.investing.Investing;
import com.github.triceo.robozonky.app.investing.Investor;
import com.github.triceo.robozonky.app.portfolio.Portfolio;
import com.github.triceo.robozonky.app.portfolio.Selling;
import com.github.triceo.robozonky.app.purchasing.Purchasing;
import com.github.triceo.robozonky.internal.api.Settings;
import com.github.triceo.robozonky.util.RoboZonkyThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DaemonInvestmentMode implements InvestmentMode {

    private static final Logger LOGGER = LoggerFactory.getLogger(DaemonInvestmentMode.class);
    private static final ThreadFactory THREAD_FACTORY = new RoboZonkyThreadFactory(new ThreadGroup("rzDaemon"));
    public static final AtomicReference<CountDownLatch> BLOCK_UNTIL_ZERO = new AtomicReference<>(new CountDownLatch(1));

    private static TemporalAmount getSuddenDeathTimeout() {
        final long socketTimeoutSeconds = Settings.INSTANCE.getSocketTimeout().get(ChronoUnit.SECONDS);
        final long connectionTimeoutSeconds = Settings.INSTANCE.getConnectionTimeout().get(ChronoUnit.SECONDS);
        final long max = Math.max(socketTimeoutSeconds, connectionTimeoutSeconds);
        return Duration.ofSeconds(max * 2);
    }

    private final String username;
    private final boolean faultTolerant, dryRun;
    private final Marketplace marketplace;
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(3, THREAD_FACTORY);
    private final TemporalAmount periodBetweenChecks;
    private final SuddenDeathDetection suddenDeath;
    private final CountDownLatch circuitBreaker;
    private final Thread shutdownHook;

    public DaemonInvestmentMode(final Authenticated auth, final Investor.Builder builder, final boolean isFaultTolerant,
                                final Marketplace marketplace, final String strategyLocaion,
                                final TemporalAmount maximumSleepPeriod, final TemporalAmount periodBetweenChecks) {
        this.username = auth.getSecretProvider().getUsername();
        this.dryRun = builder.isDryRun();
        this.faultTolerant = isFaultTolerant;
        this.circuitBreaker = BLOCK_UNTIL_ZERO.updateAndGet(l -> l.getCount() == 0 ? new CountDownLatch(1) : l);
        this.shutdownHook = new DaemonShutdownHook(circuitBreaker);
        Runtime.getRuntime().addShutdownHook(shutdownHook);
        this.marketplace = marketplace;
        this.periodBetweenChecks = periodBetweenChecks;
        Portfolio.INSTANCE.registerUpdater(new Selling(RefreshableSellStrategy.create(strategyLocaion), dryRun));
        final Daemon investing = new Investing(auth, builder, marketplace,
                                                 RefreshableInvestmentStrategy.create(strategyLocaion),
                                                 maximumSleepPeriod);
        final Daemon purchasing = new Purchasing(auth, dryRun, RefreshablePurchaseStrategy.create(strategyLocaion),
                                                 maximumSleepPeriod);
        this.suddenDeath = new SuddenDeathDetection(circuitBreaker, getSuddenDeathTimeout(), investing, purchasing);
    }

    private Map<Daemon, Long> getDelays(final Collection<Daemon> daemons, final long checkPeriod) {
        final Map<Daemon, Long> result = new LinkedHashMap<>(daemons.size());
        final long delay = checkPeriod / daemons.size();
        long currentDelay = checkPeriod;
        for (final Daemon d : suddenDeath.getDaemonsToWatch()) {
            result.put(d, currentDelay);
            currentDelay -= delay;
        }
        return result;
    }

    @Override
    public ReturnCode get() {
        try {
            final long checkPeriod = this.periodBetweenChecks.get(ChronoUnit.SECONDS) * 1000;
            LOGGER.debug("Scheduling marketplace checks {} milliseconds apart.", checkPeriod);
            // schedule the tasks some time apart so that the CPU is evenly utilized
            getDelays(suddenDeath.getDaemonsToWatch(), checkPeriod).forEach((daemon, delay) -> {
                final Runnable r = () -> {
                    if (Portfolio.INSTANCE.isUpdating()) {
                        LOGGER.debug("Paused to allow for update of internal structures: {}.", daemon.getClass());
                        return;
                    }
                    daemon.run();
                };
                executor.scheduleWithFixedDelay(r, delay, checkPeriod, TimeUnit.MILLISECONDS);
            });
            executor.scheduleAtFixedRate(suddenDeath, 0, checkPeriod, TimeUnit.MILLISECONDS);
            // block until request to stop the app is received
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
