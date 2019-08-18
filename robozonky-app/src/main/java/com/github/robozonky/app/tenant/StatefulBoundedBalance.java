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

package com.github.robozonky.app.tenant;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import com.github.robozonky.internal.state.InstanceState;
import com.github.robozonky.internal.tenant.Tenant;
import com.github.robozonky.internal.test.DateUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

final class StatefulBoundedBalance {

    private static final Duration BALANCE_INCREASE_INTERVAL_STEP = Duration.ofHours(1);
    private static final String VALUE_KEY = "lastKnownUpperBound";
    private static final Logger LOGGER = LogManager.getLogger(StatefulBoundedBalance.class);

    private final InstanceState<StatefulBoundedBalance> state;
    private final AtomicLong currentValue = new AtomicLong();
    private final AtomicReference<Instant> lastModificationDate = new AtomicReference<>();

    public StatefulBoundedBalance(final Tenant tenant) {
        this.state = tenant.getState(StatefulBoundedBalance.class);
        reloadFromState();
    }

    private synchronized void reloadFromState() {
        final Instant lastModified = state.getLastUpdated().map(OffsetDateTime::toInstant).orElse(Instant.EPOCH);
        lastModificationDate.set(lastModified);
        final long lastKnownValue = state.getValue(VALUE_KEY).map(Long::parseLong).orElse(Long.MAX_VALUE);
        currentValue.set(lastKnownValue);
        LOGGER.trace("Loaded {} CZK from {}", lastKnownValue, lastModified);
    }

    public synchronized void set(final long value) {
        currentValue.set(value);
        lastModificationDate.set(DateUtil.now());
        state.update(m -> m.put(VALUE_KEY, Long.toString(value)));
    }

    public synchronized long get() {
        final long balance = currentValue.get();
        if (balance == Long.MAX_VALUE) { // no need to do any other magic
            LOGGER.trace("Balance already at maximum.");
            return balance;
        }
        final Instant lastModified = lastModificationDate.get();
        final Duration timeBetweenLastBalanceCheckAndNow = Duration.between(DateUtil.now(), lastModified).abs();
        if (timeBetweenLastBalanceCheckAndNow.compareTo(BALANCE_INCREASE_INTERVAL_STEP) < 0) {
            LOGGER.trace("Balance of {} CZK is still fresh ({}).", balance, lastModified);
            return balance;
        }
        // try to increase the balance; double it for every BALANCE_INCREASE_INTERVAL_STEP
        final long nanosPeriod = BALANCE_INCREASE_INTERVAL_STEP.toNanos();
        final long nanosBetween = timeBetweenLastBalanceCheckAndNow.toNanos();
        final long elapsedCycles = nanosBetween / nanosPeriod;
        final long newBalance = balance * 2 * elapsedCycles;
        LOGGER.trace("Changing balance upper bound from {} to {} CZK", balance, newBalance);
        return newBalance;
    }
}
