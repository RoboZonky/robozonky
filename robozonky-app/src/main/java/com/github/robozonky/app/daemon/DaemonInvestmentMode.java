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
import com.github.robozonky.api.remote.enums.InvestmentType;
import com.github.robozonky.app.ReturnCode;
import com.github.robozonky.app.configuration.InvestmentMode;
import com.github.robozonky.app.runtime.Lifecycle;
import com.github.robozonky.app.tenant.PowerTenant;
import com.github.robozonky.internal.async.Scheduler;
import com.github.robozonky.internal.async.Tasks;
import com.github.robozonky.internal.extensions.JobServiceLoader;
import com.github.robozonky.internal.remote.Zonky;
import io.vavr.control.Try;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DaemonInvestmentMode implements InvestmentMode {

    private static final Logger LOGGER = LogManager.getLogger(DaemonInvestmentMode.class);
    private final PowerTenant tenant;
    private final Consumer<Throwable> shutdownCall;

    public DaemonInvestmentMode(final Consumer<Throwable> shutdownCall, final PowerTenant tenant) {
        this.shutdownCall = shutdownCall;
        this.tenant = tenant;
    }

    DaemonInvestmentMode(final PowerTenant tenant) {
        this(t -> {
        }, tenant);
    }

    private void scheduleDaemons(final Scheduler executor) { // run investing and purchasing daemons
        LOGGER.debug("Scheduling daemon threads.");
        // never check marketplaces more than once per second, or else Zonky quotas will come knocking
        final Duration oneSecond = Duration.ofSeconds(1);
        submit(executor, StrategyExecutor.forInvesting(tenant)::get, InvestingSession.class, oneSecond);
        submit(executor, StrategyExecutor.forPurchasing(tenant)::get, PurchasingSession.class, oneSecond,
               Duration.ofMillis(250)); // delay so that primary and secondary don't happen at the same time
    }

    private void submit(final Scheduler executor, final Runnable r, final Class<?> type, final Duration repeatAfter) {
        submit(executor, r, type, repeatAfter, Duration.ZERO);
    }

    private void submit(final Scheduler executor, final Runnable r, final Class<?> type, final Duration repeatAfter,
                        final Duration initialDelay) {
        submit(executor, r, type, repeatAfter, initialDelay, Duration.ZERO);
    }

    void submit(final Scheduler executor, final Runnable r, final Class<?> type, final Duration repeatAfter,
                final Duration initialDelay, final Duration timeout) {
        LOGGER.debug("Submitting {} to {}, repeating after {}, starting in {}. Optional timeout of {}.", type,
                     executor, repeatAfter, initialDelay, timeout);
        executor.submit(new Skippable(r, type, tenant, shutdownCall), repeatAfter, initialDelay, timeout);
    }

    private void scheduleJobs() {
        // TODO implement payload timeouts (https://github.com/RoboZonky/robozonky/issues/307)
        LOGGER.debug("Scheduling simple batch jobs.");
        JobServiceLoader.loadSimpleJobs()
                .forEach(j -> submit(Tasks.INSTANCE.scheduler(), j.payload(), j.getClass(), j.repeatEvery(), j.startIn(),
                                     j.killIn()));
        LOGGER.debug("Scheduling tenant-based batch jobs.");
        JobServiceLoader.loadTenantJobs()
                .forEach(j -> submit(Tasks.INSTANCE.scheduler(), () -> j.payload().accept(tenant), j.getClass(),
                                     j.repeatEvery(), j.startIn(), j.killIn()));
        LOGGER.debug("Job scheduling over.");
    }

    @Override
    public SessionInfo getSessionInfo() {
        return tenant.getSessionInfo();
    }

    @Override
    public InvestmentType getInvestmentType() {
        return tenant.call(Zonky::getWallet).getInvestmentType();
    }

    @Override
    public ReturnCode apply(final Lifecycle lifecycle) {
        return Try.of(() -> {
            // schedule the tasks
            scheduleJobs();
            scheduleDaemons(Tasks.INSTANCE.scheduler());
            // block until request to stop the app is received
            lifecycle.suspend();
            LOGGER.trace("Request to stop received.");
            // signal the end of operation
            return (lifecycle.isFailed()) ? ReturnCode.ERROR_UNEXPECTED : ReturnCode.OK;
        }).getOrElseThrow((Function<Throwable, IllegalStateException>) IllegalStateException::new);
    }

    @Override
    public void close() throws Exception {
        tenant.close();
    }
}
