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

package com.github.triceo.robozonky.app.investing;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAmount;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

import com.github.triceo.robozonky.api.Refreshable;
import com.github.triceo.robozonky.api.marketplaces.Marketplace;
import com.github.triceo.robozonky.api.remote.entities.Investment;
import com.github.triceo.robozonky.api.remote.entities.Loan;
import com.github.triceo.robozonky.api.strategies.InvestmentStrategy;
import com.github.triceo.robozonky.api.strategies.LoanDescriptor;
import com.github.triceo.robozonky.app.authentication.Authenticated;
import com.github.triceo.robozonky.app.investing.delinquency.DelinquencyUpdater;
import com.github.triceo.robozonky.util.RoboZonkyThreadFactory;
import com.github.triceo.robozonky.util.Scheduler;

public class DaemonInvestmentMode extends AbstractInvestmentMode {

    private static final ThreadFactory THREAD_FACTORY = new RoboZonkyThreadFactory(new ThreadGroup("rzDaemon"));
    public static final AtomicReference<CountDownLatch> BLOCK_UNTIL_ZERO =
            new AtomicReference<>(new CountDownLatch(1));

    private static TemporalAmount getSuddenDeathTimeout(final TemporalAmount periodBetweenChecks) {
        final long secondBetweenChecks = periodBetweenChecks.get(ChronoUnit.SECONDS);
        return Duration.ofMinutes(secondBetweenChecks); // multiply by 60
    }

    private final Refreshable<InvestmentStrategy> refreshableStrategy;
    private final Marketplace marketplace;
    private final ScheduledExecutorService executor =
            Executors.newScheduledThreadPool(2, DaemonInvestmentMode.THREAD_FACTORY);
    private final TemporalAmount maximumSleepPeriod, periodBetweenChecks;
    private final SuddenDeathDetection suddenDeath;
    private final CountDownLatch blockUntilZero;
    private final Thread shutdownHook;

    public DaemonInvestmentMode(final Authenticated auth, final Investor.Builder builder, final boolean isFaultTolerant,
                                final Marketplace marketplace, final Refreshable<InvestmentStrategy> strategy,
                                final TemporalAmount maximumSleepPeriod, final TemporalAmount periodBetweenChecks) {
        super(auth, builder, isFaultTolerant);
        this.blockUntilZero =
                DaemonInvestmentMode.BLOCK_UNTIL_ZERO.updateAndGet(l -> l.getCount() == 0 ? new CountDownLatch(1) : l);
        this.shutdownHook = new DaemonShutdownHook(blockUntilZero);
        Runtime.getRuntime().addShutdownHook(shutdownHook);
        this.refreshableStrategy = strategy;
        this.marketplace = marketplace;
        this.maximumSleepPeriod = maximumSleepPeriod;
        this.periodBetweenChecks = periodBetweenChecks;
        this.suddenDeath = new SuddenDeathDetection(blockUntilZero,
                                                    DaemonInvestmentMode.getSuddenDeathTimeout(periodBetweenChecks));
        // FIXME move to a more appropriate place
        Scheduler.BACKGROUND_SCHEDULER.submit(new DelinquencyUpdater(auth), Duration.ofHours(1));
    }

    Thread getShutdownHook() {
        return shutdownHook;
    }

    @Override
    public Optional<Collection<Investment>> get() {
        /*
         * in tests, the number of available permits may get over 1 as the semaphore is released multiple times.
         * let's make sure we always acquire all the available permits, no matter what the actual number is.
         */
        return execute(blockUntilZero);
    }

    @Override
    protected void openMarketplace(final Consumer<Collection<Loan>> target) {
        marketplace.registerListener(target);
        final Runnable marketplaceCheck = () -> {
            try {
                marketplace.run();
            } catch (final Throwable t) {
                /*
                 * We catch Throwable so that we can inform users even about errors. Sudden death detection will take
                 * care of errors stopping the thread.
                 */
                new DaemonRuntimeExceptionHandler().handle(t);
            } finally {
                suddenDeath.registerMarketplaceCheck(); // sudden death averted for now
            }
        };
        switch (marketplace.specifyExpectedTreatment()) {
            case POLLING:
                final long checkPeriodInSeconds = this.periodBetweenChecks.get(ChronoUnit.SECONDS);
                LOGGER.debug("Scheduling marketplace checks {} seconds apart.", checkPeriodInSeconds);
                executor.scheduleWithFixedDelay(marketplaceCheck, 0, checkPeriodInSeconds, TimeUnit.SECONDS);
                executor.scheduleAtFixedRate(this.suddenDeath, 0, checkPeriodInSeconds, TimeUnit.SECONDS);
                break;
            case LISTENING:
                LOGGER.debug("Starting marketplace listener.");
                executor.submit(marketplaceCheck);
                break;
            default:
                throw new IllegalStateException("Impossible.");
        }
    }

    @Override
    protected Function<Collection<LoanDescriptor>, Collection<Investment>> getInvestor() {
        return new StrategyExecution(getInvestorBuilder(), refreshableStrategy, getAuthenticated(), maximumSleepPeriod);
    }

    @Override
    protected boolean wasSuddenDeath() {
        return suddenDeath.isSuddenDeath();
    }

    @Override
    public void close() throws Exception {
        LOGGER.trace("Shutting down executor.");
        this.executor.shutdownNow();
        LOGGER.trace("Closing marketplace.");
        this.marketplace.close();
        // reinitialize so that new daemons don't end right away
    }
}
