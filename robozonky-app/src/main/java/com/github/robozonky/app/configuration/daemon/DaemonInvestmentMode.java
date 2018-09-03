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

package com.github.robozonky.app.configuration.daemon;

import java.time.Duration;
import java.util.concurrent.ThreadFactory;
import java.util.function.Consumer;

import com.github.robozonky.api.notifications.RoboZonkyDaemonFailedEvent;
import com.github.robozonky.app.Events;
import com.github.robozonky.app.ReturnCode;
import com.github.robozonky.app.authentication.Tenant;
import com.github.robozonky.app.configuration.InvestmentMode;
import com.github.robozonky.app.investing.Investor;
import com.github.robozonky.app.runtime.Lifecycle;
import com.github.robozonky.common.extensions.JobServiceLoader;
import com.github.robozonky.common.jobs.Job;
import com.github.robozonky.common.jobs.Payload;
import com.github.robozonky.util.RoboZonkyThreadFactory;
import com.github.robozonky.util.Scheduler;
import com.github.robozonky.util.Schedulers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DaemonInvestmentMode implements InvestmentMode {

    private static final Logger LOGGER = LoggerFactory.getLogger(DaemonInvestmentMode.class);
    private static final ThreadFactory THREAD_FACTORY = new RoboZonkyThreadFactory(newThreadGroup("rzDaemon"));
    private final DaemonOperation investing, purchasing;
    private final PortfolioUpdater portfolioUpdater;
    private final Tenant tenant;
    private final Consumer<Throwable> shutdownCall;

    public DaemonInvestmentMode(final Consumer<Throwable> shutdownCall, final Tenant tenant,
                                final Investor investor, final StrategyProvider strategyProvider,
                                final Duration primaryMarketplaceCheckPeriod,
                                final Duration secondaryMarketplaceCheckPeriod) {
        this.portfolioUpdater = PortfolioUpdater.create(shutdownCall, tenant, strategyProvider::getToSell);
        this.tenant = tenant;
        this.investing = new InvestingDaemon(shutdownCall, tenant, investor, strategyProvider::getToInvest,
                                             portfolioUpdater, primaryMarketplaceCheckPeriod);
        this.purchasing = new PurchasingDaemon(shutdownCall, tenant, strategyProvider::getToPurchase, portfolioUpdater,
                                               secondaryMarketplaceCheckPeriod);
        this.shutdownCall = shutdownCall;
    }

    static void runSafe(final Runnable runnable, final Consumer<Throwable> shutdownCall) {
        try {
            runnable.run();
        } catch (final Exception ex) {
            LOGGER.warn("Caught unexpected exception, continuing operation.", ex);
            Events.fire(new RoboZonkyDaemonFailedEvent(ex));
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
        executor.run(portfolioUpdater); // first run the update
        // schedule hourly refresh
        final Duration oneHour = Duration.ofHours(1);
        executor.submit(portfolioUpdater, oneHour, oneHour);
        // run investing and purchasing daemons
        executor.submit(toSkippable(investing), investing.getRefreshInterval());
        executor.submit(toSkippable(purchasing), purchasing.getRefreshInterval(), Duration.ofMillis(250));
    }

    private boolean isUpdating() {
        return !tenant.isAvailable() || portfolioUpdater.isUpdating();
    }

    private void scheduleJob(final Job job, final Scheduler executor) {
        final Payload payload = job.payload();
        final String payloadId = payload.id();
        final Runnable runnable = () -> {
            LOGGER.debug("Running payload {} ({}).", payloadId, job);
            runSafe(() -> payload.accept(tenant.getSecrets()), shutdownCall);
            LOGGER.debug("Finished payload {} ({}).", payloadId, job);
        };
        final Duration maxRunTime = getMaxJobRuntime(job);
        final Duration cancelIn = job.startIn().plusMillis(maxRunTime.toMillis());
        LOGGER.debug("Scheduling payload {} ({}). Max run time: {}. Cancel in: {}.", payloadId, job, maxRunTime,
                     cancelIn);
        executor.submit(runnable, job.repeatEvery(), job.startIn());
        // TODO implement payload timeouts (https://github.com/RoboZonky/robozonky/issues/307)
    }

    private void scheduleJobs(final Scheduler executor) {
        JobServiceLoader.load().forEach(job -> scheduleJob(job, executor));
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
}
