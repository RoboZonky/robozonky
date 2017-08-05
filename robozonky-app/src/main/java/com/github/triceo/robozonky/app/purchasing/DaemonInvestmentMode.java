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

package com.github.triceo.robozonky.app.purchasing;

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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.triceo.robozonky.api.Refreshable;
import com.github.triceo.robozonky.api.remote.entities.Investment;
import com.github.triceo.robozonky.api.remote.entities.Participation;
import com.github.triceo.robozonky.api.strategies.ParticipationDescriptor;
import com.github.triceo.robozonky.api.strategies.PurchaseStrategy;
import com.github.triceo.robozonky.app.authentication.Authenticated;
import com.github.triceo.robozonky.app.commons.DaemonRuntimeExceptionHandler;
import com.github.triceo.robozonky.app.commons.DaemonShutdownHook;
import com.github.triceo.robozonky.app.commons.InvestmentMode;
import com.github.triceo.robozonky.app.commons.SuddenDeathDetection;
import com.github.triceo.robozonky.app.commons.SuddenDeathException;
import com.github.triceo.robozonky.util.RoboZonkyThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DaemonInvestmentMode implements InvestmentMode {

    private static final Logger LOGGER = LoggerFactory.getLogger(
            DaemonInvestmentMode.class);

    private static final ThreadFactory THREAD_FACTORY = new RoboZonkyThreadFactory(new ThreadGroup("rzPurchase"));
    public static final AtomicReference<CountDownLatch> BLOCK_UNTIL_ZERO =
            new AtomicReference<>(new CountDownLatch(1));

    private static TemporalAmount getSuddenDeathTimeout(final TemporalAmount periodBetweenChecks) {
        final long secondBetweenChecks = periodBetweenChecks.get(ChronoUnit.SECONDS);
        return Duration.ofMinutes(secondBetweenChecks); // multiply by 60
    }

    private final Authenticated authenticated;
    private final boolean isFaultTolerant, isDryRun;
    private final Refreshable<PurchaseStrategy> refreshableStrategy;
    private final ScheduledExecutorService executor =
            Executors.newScheduledThreadPool(2, DaemonInvestmentMode.THREAD_FACTORY);
    private final TemporalAmount maximumSleepPeriod, periodBetweenChecks;
    private final SuddenDeathDetection suddenDeath;
    private final CountDownLatch circuitBreaker;
    private final Thread shutdownHook;

    public DaemonInvestmentMode(final Authenticated auth, final boolean isDryRun, final boolean isFaultTolerant,
                                final Refreshable<PurchaseStrategy> strategy, final TemporalAmount maximumSleepPeriod,
                                final TemporalAmount periodBetweenChecks) {
        this.authenticated = auth;
        this.isFaultTolerant = isFaultTolerant;
        this.isDryRun = isDryRun;
        this.circuitBreaker =
                DaemonInvestmentMode.BLOCK_UNTIL_ZERO.updateAndGet(l -> l.getCount() == 0 ? new CountDownLatch(1) : l);
        this.shutdownHook = new DaemonShutdownHook(circuitBreaker);
        Runtime.getRuntime().addShutdownHook(shutdownHook);
        this.refreshableStrategy = strategy;
        this.maximumSleepPeriod = maximumSleepPeriod;
        this.periodBetweenChecks = periodBetweenChecks;
        this.suddenDeath = new SuddenDeathDetection(circuitBreaker,
                                                    DaemonInvestmentMode.getSuddenDeathTimeout(periodBetweenChecks));
    }

    Thread getShutdownHook() {
        return shutdownHook;
    }

    @Override
    public Optional<Collection<Investment>> get() {
        LOGGER.trace("Executing.");
        try {
            final ResultTracker buffer = new ResultTracker();
            final Consumer<Collection<Participation>> investor = (items) -> {
                final Collection<ParticipationDescriptor> descriptors = buffer.acceptLoansFromMarketplace(items);
                final Collection<Investment> result = getInvestor().apply(descriptors);
                buffer.acceptInvestmentsFromRobot(result);
            };
            openMarketplace(investor);
            if (circuitBreaker != null) { // daemon mode requires special handling
                LOGGER.trace("Will wait for request to stop on {}.", circuitBreaker);
                circuitBreaker.await();
                LOGGER.trace("Request to stop received.");
                if (this.suddenDeath.isSuddenDeath()) {
                    throw new SuddenDeathException();
                }
            }
            return Optional.of(buffer.getInvestmentsMade());
        } catch (final SuddenDeathException ex) {
            LOGGER.error("Thread stack traces:");
            Thread.getAllStackTraces().forEach((key, value) -> {
                LOGGER.error("Stack trace for thread {}: {}", key, Stream.of(value)
                        .map(StackTraceElement::toString)
                        .collect(Collectors.joining(System.lineSeparator())));
            });
            throw new IllegalStateException(ex);
        } catch (final Exception ex) {
            LOGGER.error("Failed executing purchase.", ex);
            return Optional.empty();
        }
    }

    private void openMarketplace(final Consumer<Collection<Participation>> target) {
        final Runnable marketplaceCheck = () -> {
            try { // FIXME perhaps streaming would be more resource-efficient?
                authenticated.run(z -> target.accept(z.getAvailableParticipations().collect(Collectors.toList())));
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
        final long checkPeriodInSeconds = this.periodBetweenChecks.get(ChronoUnit.SECONDS);
        LOGGER.debug("Scheduling marketplace checks {} seconds apart.", checkPeriodInSeconds);
        executor.scheduleWithFixedDelay(marketplaceCheck, 0, checkPeriodInSeconds, TimeUnit.SECONDS);
        executor.scheduleAtFixedRate(this.suddenDeath, 0, checkPeriodInSeconds, TimeUnit.SECONDS);
    }

    private Function<Collection<ParticipationDescriptor>, Collection<Investment>> getInvestor() {
        return new StrategyExecution(refreshableStrategy, authenticated, maximumSleepPeriod, isDryRun);
    }

    @Override
    public boolean isFaultTolerant() {
        return isFaultTolerant;
    }

    @Override
    public boolean isDryRun() {
        return isDryRun;
    }

    @Override
    public String getUsername() {
        return authenticated.getSecretProvider().getUsername();
    }

    @Override
    public void close() throws Exception {
        LOGGER.trace("Shutting down executor.");
        this.executor.shutdownNow();
    }
}
