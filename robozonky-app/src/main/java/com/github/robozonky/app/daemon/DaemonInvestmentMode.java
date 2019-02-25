/*
 * Copyright 2019 The RoboZonky Project
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
import java.util.function.Function;

import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.app.ReturnCode;
import com.github.robozonky.app.configuration.InvestmentMode;
import com.github.robozonky.app.runtime.Lifecycle;
import com.github.robozonky.app.tenant.PowerTenant;
import com.github.robozonky.common.async.Scheduler;
import com.github.robozonky.common.async.Tasks;
import com.github.robozonky.common.extensions.JobServiceLoader;
import com.github.robozonky.common.jobs.Job;
import io.vavr.control.Try;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DaemonInvestmentMode implements InvestmentMode {

    private static final Logger LOGGER = LogManager.getLogger(DaemonInvestmentMode.class);
    private final PowerTenant tenant;
    private final Investor investor;
    private final Duration secondaryMarketplaceCheckPeriod;

    public DaemonInvestmentMode(final PowerTenant tenant, final Investor investor,
                                final Duration secondaryMarketplaceCheckPeriod) {
        this.tenant = tenant;
        this.investor = investor;
        this.secondaryMarketplaceCheckPeriod = secondaryMarketplaceCheckPeriod;
    }

    private static Scheduler getSchedulerForJob(final Job job) {
        return job.prioritize() ? Tasks.SUPPORTING.scheduler() : Tasks.BACKGROUND.scheduler();
    }

    private void scheduleDaemons(final Scheduler executor) { // run investing and purchasing daemons
        LOGGER.debug("Scheduling daemon threads.");
        submit(executor, StrategyExecutor.forInvesting(tenant, investor)::get, InvestingSession.class,
               Duration.ofSeconds(1));
        submit(executor, StrategyExecutor.forPurchasing(tenant)::get, PurchasingSession.class,
               secondaryMarketplaceCheckPeriod, Duration.ofMillis(250));
    }

    private void submit(final Scheduler executor, final Runnable r, final Class<?> type, final Duration repeatAfter) {
        submit(executor, r, type, repeatAfter, Duration.ZERO);
    }

    void submit(final Scheduler executor, final Runnable r, final Class<?> type, final Duration repeatAfter,
                final Duration initialDelay) {
        LOGGER.debug("Submitting {} to {}, repeating after {}, starting in {}.", type, executor, repeatAfter,
                     initialDelay);
        executor.submit(new Skippable(r, type, tenant), repeatAfter, initialDelay);
    }

    private void scheduleJobs() {
        // TODO implement payload timeouts (https://github.com/RoboZonky/robozonky/issues/307)
        LOGGER.debug("Scheduling simple batch jobs.");
        JobServiceLoader.loadSimpleJobs()
                .forEach(j -> submit(getSchedulerForJob(j), j.payload(), j.getClass(), j.repeatEvery(), j.startIn()));
        LOGGER.debug("Scheduling tenant-based batch jobs.");
        JobServiceLoader.loadTenantJobs()
                .forEach(j -> submit(getSchedulerForJob(j), () -> j.payload().accept(tenant), j.getClass(),
                                     j.repeatEvery(), j.startIn()));
        LOGGER.debug("Job scheduling over.");
    }

    @Override
    public SessionInfo getSessionInfo() {
        return tenant.getSessionInfo();
    }

    @Override
    public ReturnCode apply(final Lifecycle lifecycle) {
        return Try.of(() -> {
            // schedule the tasks
            scheduleJobs();
            scheduleDaemons(Tasks.REALTIME.scheduler());
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
