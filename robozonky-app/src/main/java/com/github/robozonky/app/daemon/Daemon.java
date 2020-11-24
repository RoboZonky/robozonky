/*
 * Copyright 2020 The RoboZonky Project
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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.app.InvestmentMode;
import com.github.robozonky.app.ReturnCode;
import com.github.robozonky.app.runtime.Lifecycle;
import com.github.robozonky.app.tenant.PowerTenant;
import com.github.robozonky.internal.async.Scheduler;
import com.github.robozonky.internal.extensions.JobServiceLoader;
import com.github.robozonky.internal.test.DateUtil;

public class Daemon implements InvestmentMode {

    private static final Logger LOGGER = LogManager.getLogger(Daemon.class);
    private final PowerTenant tenant;
    private final Lifecycle lifecycle;
    private final Scheduler scheduler;

    public Daemon(final PowerTenant tenant, final Lifecycle lifecycle) {
        this(tenant, lifecycle, Scheduler.create());
    }

    Daemon(final PowerTenant tenant, final Lifecycle lifecycle, Scheduler scheduler) {
        this.tenant = tenant;
        this.lifecycle = lifecycle;
        this.scheduler = scheduler;
    }

    private void scheduleDaemons(final Scheduler executor) { // run investing and purchasing daemons
        LOGGER.debug("Scheduling daemon threads.");
        var halfSecond = Duration.ofMillis(500);
        submitWithTenant(executor, StrategyExecutor.forInvesting(tenant)::get, InvestingSession.class, halfSecond);
        submitWithTenant(executor, StrategyExecutor.forPurchasing(tenant)::get, PurchasingSession.class, halfSecond,
                halfSecond.dividedBy(2)); // delay so that primary and secondary don't happen at the same time
    }

    private void submitWithTenant(final Scheduler executor, final Runnable r, final Class<?> type,
            final Duration repeatAfter) {
        submitWithTenant(executor, r, type, repeatAfter, Duration.ZERO);
    }

    private void submitWithTenant(final Scheduler executor, final Runnable r, final Class<?> type,
            final Duration repeatAfter,
            final Duration initialDelay) {
        submitWithTenant(executor, r, type, repeatAfter, initialDelay, Duration.ZERO);
    }

    void submitWithTenant(final Scheduler executor, final Runnable r, final Class<?> type, final Duration repeatAfter,
            final Duration initialDelay, final Duration timeout) {
        var startAt = DateUtil.zonedNow()
            .plus(initialDelay);
        LOGGER.debug(() -> "Submitting " + type + " to " + executor
                + ", start on " + DateUtil.toString(startAt)
                + ", repeating after " + repeatAfter
                + ". Optional timeout of " + timeout + ".");
        final Runnable payload = new Skippable(r, type, tenant, this::triggerShutdownDueToFailure);
        executor.submit(payload, repeatAfter, initialDelay, timeout);
    }

    void submitTenantless(final Scheduler executor, final Runnable r, final Class<?> type, final Duration repeatAfter,
            final Duration initialDelay, final Duration timeout) {
        var startAt = DateUtil.zonedNow()
            .plus(initialDelay);
        LOGGER.debug(() -> "Submitting " + type + " to " + executor
                + ", start on " + DateUtil.toString(startAt)
                + ", repeating after " + repeatAfter
                + ". Optional timeout of " + timeout + ".");
        final Runnable payload = new SimpleSkippable(r, type, this::triggerShutdownDueToFailure);
        executor.submit(payload, repeatAfter, initialDelay, timeout);
    }

    private void triggerShutdownDueToFailure(final Throwable throwable) {
        lifecycle.resumeToFail(throwable);
    }

    private void scheduleJobs() {
        LOGGER.debug("Scheduling simple batch jobs.");
        JobServiceLoader.loadSimpleJobs()
            .forEach(j -> submitTenantless(scheduler, j.payload(), j.getClass(), j.repeatEvery(),
                    j.startIn(), j.killIn()));
        LOGGER.debug("Scheduling tenant-based batch jobs.");
        JobServiceLoader.loadTenantJobs()
            .forEach(j -> submitWithTenant(scheduler, () -> j.payload()
                .accept(tenant), j.getClass(),
                    j.repeatEvery(), j.startIn(), j.killIn()));
        LOGGER.debug("Job scheduling over.");
    }

    @Override
    public SessionInfo getSessionInfo() {
        return tenant.getSessionInfo();
    }

    @Override
    public ReturnCode get() {
        try {
            // schedule the tasks
            scheduleJobs();
            scheduleDaemons(scheduler);
            // block until request to stop the app is received
            lifecycle.suspend();
            LOGGER.trace("Request to stop received.");
            // signal the end of operation
            return (lifecycle.isFailed()) ? ReturnCode.ERROR_UNEXPECTED : ReturnCode.OK;
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Override
    public void close() throws Exception {
        scheduler.close();
        tenant.close();
    }
}
