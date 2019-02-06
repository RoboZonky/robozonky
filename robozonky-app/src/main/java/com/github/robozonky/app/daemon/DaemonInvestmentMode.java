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
import java.util.function.Consumer;
import java.util.function.Function;

import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.app.ReturnCode;
import com.github.robozonky.app.configuration.InvestmentMode;
import com.github.robozonky.app.runtime.Lifecycle;
import com.github.robozonky.app.tenant.PowerTenant;
import com.github.robozonky.common.async.Scheduler;
import com.github.robozonky.common.extensions.JobServiceLoader;
import io.vavr.control.Try;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DaemonInvestmentMode implements InvestmentMode {

    private static final Logger LOGGER = LogManager.getLogger(DaemonInvestmentMode.class);
    private static final Duration MARKETPLACE_CHECK_PERIOD = Duration.ofSeconds(1);
    private final PowerTenant tenant;
    private final Investor investor;
    private final Consumer<Throwable> shutdownCall;

    public DaemonInvestmentMode(final Consumer<Throwable> shutdownCall, final PowerTenant tenant,
                                final Investor investor) {
        this.tenant = tenant;
        this.investor = investor;
        this.shutdownCall = shutdownCall;
    }

    DaemonInvestmentMode(final PowerTenant tenant, final Investor investor) {
        this(t -> {
        }, tenant, investor);
    }

    /**
     * Converts a {@link Runnable} into one that will never throw, since that would cause it to stop repeating,
     * effectively stopping the daemon. The operation will instead just terminate, possibly halting the daemon on an
     * unrecoverable failure.
     * @param operation Operation to be made safe.
     * @return Safe version of the operation.
     */
    private Runnable toSkippable(final Runnable operation) {
        return new Skippable(operation, tenant, shutdownCall);
    }

    private void scheduleDaemons(final Scheduler executor) { // run investing and purchasing daemons
        LOGGER.debug("Scheduling daemon threads.");
        submit(executor, StrategyExecutor.forInvesting(tenant, investor)::get, MARKETPLACE_CHECK_PERIOD);
        submit(executor, StrategyExecutor.forPurchasing(tenant)::get, MARKETPLACE_CHECK_PERIOD, Duration.ofMillis(250));
    }

    private void submit(final Scheduler executor, final Runnable r, final Duration repeatAfter) {
        submit(executor, r, repeatAfter, Duration.ZERO);
    }

    void submit(final Scheduler executor, final Runnable r, final Duration repeatAfter, final Duration initialDelay) {
        LOGGER.debug("Submitting {} to {}, repeating after {} and starting in {}.", r, executor, repeatAfter,
                     initialDelay);
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
        return Try.of(() -> {
            final Scheduler s = Scheduler.inBackground();
            // schedule the tasks
            scheduleJobs(s);
            scheduleDaemons(s);
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
