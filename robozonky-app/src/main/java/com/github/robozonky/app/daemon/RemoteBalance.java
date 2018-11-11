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

import java.io.Closeable;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.github.robozonky.common.Tenant;
import com.github.robozonky.util.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Available balance register which periodically updates from the remote Zonky API.
 */
public interface RemoteBalance extends Supplier<BigDecimal>, Closeable {

    static RemoteBalance create(final Tenant auth, final Consumer<BigDecimal> changeListener) {
        final Logger LOGGER = LoggerFactory.getLogger(RemoteBalance.class);
        final RefreshableBalance b = new RefreshableBalance(auth);
        LOGGER.debug("Scheduling remote balance refresh: {}.", b);
        final ScheduledFuture<?> future = Scheduler.inBackground().submit(b, Duration.ofMinutes(1));
        final Runnable callToCancel = () -> {
            LOGGER.debug("Cancelling balance refresh: {}.", b);
            future.cancel(true);
        };
        return new RemoteBalanceImpl(b, auth.getSessionInfo().isDryRun(), changeListener, callToCancel);
    }

    /**
     * Request an update of balance from the remote API.
     * @param change May be used within the context of a dry run to reflect a pretend operation.
     */
    void update(BigDecimal change);
}
