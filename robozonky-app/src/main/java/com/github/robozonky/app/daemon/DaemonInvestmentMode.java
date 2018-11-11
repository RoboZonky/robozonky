/*
 * Copyright 2018 The RoboZonky Project
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

package com.github.robozonky.app.daemon;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ThreadFactory;
import java.util.function.Consumer;

import com.github.robozonky.app.ReturnCode;
import com.github.robozonky.app.configuration.InvestmentMode;
import com.github.robozonky.app.daemon.operations.Investor;
import com.github.robozonky.app.events.Events;
import com.github.robozonky.app.runtime.Lifecycle;
import com.github.robozonky.common.Tenant;
import com.github.robozonky.common.extensions.JobServiceLoader;
import com.github.robozonky.common.jobs.Job;
import com.github.robozonky.common.jobs.SimpleJob;
import com.github.robozonky.common.jobs.SimplePayload;
import com.github.robozonky.common.jobs.TenantJob;
import com.github.robozonky.common.jobs.TenantPayload;
import com.github.robozonky.util.RoboZonkyThreadFactory;
import com.github.robozonky.util.Scheduler;
import com.github.robozonky.util.Schedulers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.github.robozonky.app.events.EventFactory.roboZonkyDaemonFailed;

public class DaemonInvestmentMode implements InvestmentMode {

    private static final Logger LOGGER = LoggerFactory.getLogger(DaemonInvestmentMode.class);
    private static final ThreadFactory THREAD_FACTORY = new RoboZonkyThreadFactory(newThreadGroup("rzDaemon"));
    private final DaemonOperation investing, purchasing;
    private final PortfolioUpdater portfolio;
    private final Tenant tenant;
    private final Consumer<Throwable> shutdownCall;
    private final String sessionName;

    public DaemonInvestmentMode(final String sessionName, final Consumer<Throwable> shutdownCall, final Tenant tenant,
                                final Investor investor, final StrategyProvider strategyProvider,
                                final Duration primaryMarketplaceCheckPeriod,
                                final Duration secondaryMarketplaceCheckPeriod) {
        this.sessionName = sessionName;
        this.portfolio = PortfolioUpdater.create(shutdownCall, tenant, strategyProvider::getToSell);
        this.tenant = tenant;
        this.investing = new InvestingDaemon(shutdownCall, tenant, investor, strategyProvider::getToInvest,
                                             portfolio, primaryMarketplaceCheckPeriod);
        this.purchasing = new PurchasingDaemon(shutdownCall, tenant, strategyProvider::getToPurchase, portfolio,
                                               secondaryMarketplaceCheckPeriod);
        this.shutdownCall = shutdownCall;
    }

    DaemonInvestmentMode(final String sessionName, final Tenant tenant, final Investor investor,
                         final StrategyProvider strategyProvider, final Duration primaryMarketplaceCheckPeriod,
                         final Duration secondaryMarketplaceCheckPeriod) {
        this(sessionName, t -> {
        }, tenant, investor, strategyProvider, primaryMarketplaceCheckPeriod, secondaryMarketplaceCheckPeriod);
    }

    static void runSafe(final Events events, final Runnable runnable, final Consumer<Throwable> shutdownCall) {
        try {
            runnable.run();
        } catch (final Exception ex) {
            LOGGER.warn("Caught unexpected exception, continuing operation.", ex);
            events.fire(roboZonkyDaemonFailed(ex));
        } catch (final Error t) {
            LOGGER.error("Caught unexpected error, terminating.", t);
            shutdownCall.accept(t);
        }
    }

    private static ThreadGroup newThreadGroup(final String name) {
        final ThreadGroup threadGroup = new ThreadGroup(name);
        threadGroup.setMaxPriority(Thread.NORM_PRIORITY + 1); // these threads should be a bit more important
        threadGroup.setDaemon(true); // no thread from this group shall block shutdown
        return threadGroup;
    }

    private static Duration getMaxJobRuntime(final Job job) {
        final long maxMillis = job.killIn().toMillis();
        final long absoluteMaxMillis = Duration.ofHours(1).toMillis();
        final long result = Math.min(maxMillis, absoluteMaxMillis);
        return Duration.ofMillis(result);
    }

    private Skippable toSkippable(final DaemonOperation daemonOperation) {
        return new Skippable(daemonOperation, this::isUpdating);
    }

    private void scheduleDaemons(final Scheduler executor) {
        LOGGER.debug("Scheduling portfolio updates.");
        executor.run(portfolio); // first run the update
        // schedule hourly refresh
        final Duration oneHour = Duration.ofHours(1);
        executor.submit(portfolio, oneHour, oneHour);
        // run investing and purchasing daemons
        LOGGER.debug("Scheduling daemon threads.");
        executor.submit(toSkippable(investing), investing.getRefreshInterval());
        executor.submit(toSkippable(purchasing), purchasing.getRefreshInterval(), Duration.ofMillis(250));
    }

    private boolean isUpdating() {
        return !tenant.isAvailable() || portfolio.isInitializing();
    }

    private void scheduleSimpleJob(final SimpleJob job, final Scheduler executor) {
        final SimplePayload payload = job.payload();
        scheduleJob(job, payload, executor);
    }

    void scheduleJob(final Job job, final Runnable runnable, final Scheduler executor) {
        final Runnable payload = () -> {
            LOGGER.debug("Running job {}.", job);
            runSafe(Events.forSession(tenant.getSessionInfo()), runnable, shutdownCall);
            LOGGER.debug("Finished job {}.", job);
        };
        final Duration maxRunTime = getMaxJobRuntime(job);
        final Duration cancelIn = job.startIn().plusMillis(maxRunTime.toMillis());
        LOGGER.debug("Scheduling job {}. Max run time: {}. Cancel in: {}.", job, maxRunTime, cancelIn);
        executor.submit(payload, job.repeatEvery(), job.startIn());
    }

    private void scheduleTenantJob(final TenantJob job, final Scheduler executor) {
        final TenantPayload payload = job.payload();
        scheduleJob(job, () -> payload.accept(tenant), executor);
    }

    private void scheduleJobs(final Scheduler executor) {
        // TODO implement payload timeouts (https://github.com/RoboZonky/robozonky/issues/307)
        LOGGER.debug("Scheduling simple batch jobs.");
        JobServiceLoader.loadSimpleJobs().forEach(job -> scheduleSimpleJob(job, executor));
        LOGGER.debug("Scheduling tenant-based batch jobs.");
        JobServiceLoader.loadTenantJobs().forEach(job -> scheduleTenantJob(job, executor));
    }

    @Override
    public String getSessionName() {
        return sessionName;
    }

    @Override
    public ReturnCode apply(final Lifecycle lifecycle) {
        scheduleJobs(Scheduler.inBackground());
        try (final Scheduler executor = Schedulers.INSTANCE.create(1, THREAD_FACTORY)) {
            // schedule the tasks
            scheduleDaemons(executor);
            // block until request to stop the app is received
            lifecycle.suspend();
            LOGGER.trace("Request to stop received.");
            // signal the end of standard operation
            return ReturnCode.OK;
        }
    }

    @Override
    public void close() throws IOException {
        tenant.close();
    }
}
