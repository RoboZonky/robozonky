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

import java.time.Duration;
import java.util.concurrent.ThreadFactory;
import java.util.function.Consumer;
import java.util.function.Function;

import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.app.ReturnCode;
import com.github.robozonky.app.configuration.InvestmentMode;
import com.github.robozonky.app.runtime.Lifecycle;
import com.github.robozonky.app.tenant.PowerTenant;
import com.github.robozonky.common.async.RoboZonkyThreadFactory;
import com.github.robozonky.common.async.Scheduler;
import com.github.robozonky.common.async.Schedulers;
import com.github.robozonky.common.extensions.JobServiceLoader;
import io.vavr.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.github.robozonky.app.events.impl.EventFactory.roboZonkyDaemonFailed;

public class DaemonInvestmentMode implements InvestmentMode {

    private static final Logger LOGGER = LoggerFactory.getLogger(DaemonInvestmentMode.class);
    private static final ThreadFactory THREAD_FACTORY = new RoboZonkyThreadFactory(new ThreadGroup("rzDaemon"));
    private final DaemonOperation investing, purchasing;
    private final PowerTenant tenant;
    private final Consumer<Throwable> shutdownCall;

    public DaemonInvestmentMode(final Consumer<Throwable> shutdownCall, final PowerTenant tenant,
                                final Investor investor, final Duration primaryMarketplaceCheckPeriod,
                                final Duration secondaryMarketplaceCheckPeriod) {
        this.tenant = tenant;
        this.investing = new InvestingDaemon(tenant, investor, primaryMarketplaceCheckPeriod);
        this.purchasing = new PurchasingDaemon(tenant, secondaryMarketplaceCheckPeriod);
        this.shutdownCall = shutdownCall;
    }

    DaemonInvestmentMode(final PowerTenant tenant, final Investor investor,
                         final Duration primaryMarketplaceCheckPeriod,
                         final Duration secondaryMarketplaceCheckPeriod) {
        this(t -> {
        }, tenant, investor, primaryMarketplaceCheckPeriod, secondaryMarketplaceCheckPeriod);
    }

    /**
     * Make sure that the {@link Runnable} provided may never throw.
     * @param runnable The {@link Runnable} to be made safe.
     * @param tenant To fire failure events in case of a recoverable failure.
     * @param shutdownCall The code to call to tell the daemon that an unrecoverable {@link Error} was encoutered.
     */
    private static void runSafe(final Runnable runnable, final PowerTenant tenant,
                                final Consumer<Throwable> shutdownCall) {
        try {
            LOGGER.trace("Running {}.", runnable);
            runnable.run();
            LOGGER.trace("Finished {}.", runnable);
        } catch (final Exception ex) {
            LOGGER.warn("Caught unexpected exception, continuing operation.", ex);
            tenant.fire(roboZonkyDaemonFailed(ex));
        } catch (final Error t) {
            LOGGER.error("Caught unexpected error, terminating.", t);
            shutdownCall.accept(t);
        }
    }

    /**
     * Converts a {@link Runnable} into one that will never throw, since that would cause it to stop repeating
     * effectively stopping the daemon. The operation will instead just terminate, possibly halting the daemon on an
     * unrecoverable failure.
     * @param operation Operation to be made safe.
     * @return Safe version of the operation.
     */
    private Runnable toSkippable(final Runnable operation) {
        final Runnable r = () -> runSafe(operation, tenant, shutdownCall);
        return new Skippable(r, () -> !tenant.isAvailable());
    }

    private void scheduleDaemons(final Scheduler executor) { // run investing and purchasing daemons
        LOGGER.debug("Scheduling daemon threads.");
        submit(executor, investing, investing.getRefreshInterval());
        submit(executor, purchasing, purchasing.getRefreshInterval(), Duration.ofMillis(250));
    }

    private void submit(final Scheduler executor, final Runnable r, final Duration repeatAfter) {
        submit(executor, r, repeatAfter, Duration.ZERO);
    }

    void submit(final Scheduler executor, final Runnable r, final Duration repeatAfter, final Duration initialDelay) {
        executor.submit(toSkippable(r), repeatAfter, initialDelay);
    }

    private void scheduleJobs(final Scheduler executor) {
        // TODO implement payload timeouts (https://github.com/RoboZonky/robozonky/issues/307)
        LOGGER.debug("Scheduling simple batch jobs.");
        JobServiceLoader.loadSimpleJobs()
                .forEach(j -> submit(executor, j.payload(), j.repeatEvery(), j.startIn()));
        LOGGER.debug("Scheduling tenant-based batch jobs.");
        JobServiceLoader.loadTenantJobs()
                .forEach(j -> submit(executor, () -> j.payload().accept(tenant), j.repeatEvery(), j.startIn()));
        LOGGER.debug("Job scheduling over.");
    }

    @Override
    public SessionInfo getSessionInfo() {
        return tenant.getSessionInfo();
    }

    @Override
    public ReturnCode apply(final Lifecycle lifecycle) {
        scheduleJobs(Scheduler.inBackground());
        return Try.withResources(() -> Schedulers.INSTANCE.create(1, THREAD_FACTORY))
                .of(executor -> {
                    // schedule the tasks
                    scheduleDaemons(executor);
                    // block until request to stop the app is received
                    lifecycle.suspend();
                    LOGGER.trace("Request to stop received.");
                    // signal the end of standard operation
                    return ReturnCode.OK;
                }).getOrElseThrow((Function<Throwable, IllegalStateException>) IllegalStateException::new);
    }

    @Override
    public void close() throws Exception {
        tenant.close();
    }
}
