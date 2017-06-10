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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

import com.github.triceo.robozonky.api.Refreshable;
import com.github.triceo.robozonky.api.marketplaces.Marketplace;
import com.github.triceo.robozonky.api.remote.entities.Investment;
import com.github.triceo.robozonky.api.remote.entities.Loan;
import com.github.triceo.robozonky.api.strategies.InvestmentStrategy;
import com.github.triceo.robozonky.api.strategies.LoanDescriptor;
import com.github.triceo.robozonky.app.ShutdownEnabler;
import com.github.triceo.robozonky.app.authentication.AuthenticationHandler;
import com.github.triceo.robozonky.common.remote.ApiProvider;
import com.github.triceo.robozonky.util.RoboZonkyThreadFactory;

public class DaemonInvestmentMode extends AbstractInvestmentMode {

    private static final ThreadFactory THREAD_FACTORY = new RoboZonkyThreadFactory(new ThreadGroup("rzMarketplace"));

    private final Refreshable<InvestmentStrategy> refreshableStrategy;
    private final Marketplace marketplace;
    private final ScheduledExecutorService executor =
            Executors.newScheduledThreadPool(2, DaemonInvestmentMode.THREAD_FACTORY);
    private final TemporalAmount maximumSleepPeriod, periodBetweenChecks;
    private final SuddenDeathDetection suddenDeath =
            new SuddenDeathDetection(DaemonInvestmentMode.BLOCK_UNTIL_RELEASED, 300);
    public static final Semaphore BLOCK_UNTIL_RELEASED = new Semaphore(1);

    public DaemonInvestmentMode(final AuthenticationHandler auth, final Investor.Builder builder,
                                final boolean isFaultTolerant, final Marketplace marketplace,
                                final Refreshable<InvestmentStrategy> strategy, final TemporalAmount maximumSleepPeriod,
                                final TemporalAmount periodBetweenChecks) {
        super(auth, builder, isFaultTolerant);
        this.refreshableStrategy = strategy;
        this.marketplace = marketplace;
        this.maximumSleepPeriod = maximumSleepPeriod;
        this.periodBetweenChecks = periodBetweenChecks;
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.debug("Shutdown requested.");
            // will release the main thread and thus terminate the daemon
            DaemonInvestmentMode.BLOCK_UNTIL_RELEASED.release();
            // only allow to shut down after the daemon has been closed by the app
            ShutdownEnabler.DAEMON_ALLOWED_TO_TERMINATE.acquireUninterruptibly();
            LOGGER.debug("Shutdown allowed.");
        }));
    }

    public DaemonInvestmentMode(final AuthenticationHandler auth, final Investor.Builder builder,
                                final boolean isFaultTolerant, final Marketplace marketplace,
                                final Refreshable<InvestmentStrategy> strategy) {
        this(auth, builder, isFaultTolerant, marketplace, strategy, Duration.ofMinutes(60), Duration.ofSeconds(1));
    }

    @Override
    protected Optional<Collection<Investment>> execute(final ApiProvider apiProvider) {
        /*
         * in tests, the number of available permits may get over 1 as the semaphore is released multiple times.
         * let's make sure we always acquire all the available permits, no matter what the actual number is.
         */
        final int permitCount = Math.max(1, DaemonInvestmentMode.BLOCK_UNTIL_RELEASED.availablePermits());
        DaemonInvestmentMode.BLOCK_UNTIL_RELEASED.acquireUninterruptibly(permitCount);
        return execute(apiProvider, DaemonInvestmentMode.BLOCK_UNTIL_RELEASED);
    }

    @Override
    protected void openMarketplace(final Consumer<Collection<Loan>> target) {
        marketplace.registerListener(target);
        final Runnable marketplaceCheck = () -> {
            try {
                marketplace.run();
            } catch (final Throwable t) {
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
                executor.scheduleWithFixedDelay(this.suddenDeath, 0, checkPeriodInSeconds, TimeUnit.SECONDS);
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
    protected Function<Collection<LoanDescriptor>, Collection<Investment>> getInvestor(final ApiProvider apiProvider) {
        return new StrategyExecution(apiProvider, getInvestorBuilder(), refreshableStrategy, getAuthenticationHandler(),
                maximumSleepPeriod);
    }

    @Override
    protected boolean wasSuddenDeath() {
        return suddenDeath.isSuddenDeath();
    }

    @Override
    public void close() throws Exception {
        DaemonInvestmentMode.BLOCK_UNTIL_RELEASED.release(); // just in case
        LOGGER.trace("Shutting down executor.");
        this.executor.shutdownNow();
        LOGGER.trace("Closing marketplace.");
        this.marketplace.close();
    }
}
