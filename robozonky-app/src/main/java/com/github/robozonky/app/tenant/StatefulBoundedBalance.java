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

/**
 * On Zonky, we do not know the user's current account balance. However, in order for the robot to be able to function
 * effectively and not repeat useless Zonky calls etc., some information about balance is still required. This class
 * helps with that.
 * <p>
 * By default, the balance is {@link Long#MAX_VALUE}. Every time {@link #set(long)} is called, the balance is set to
 * this new value. Also, a timer is reset - unless {@link #set(long)} is called within reasonable time, the balance
 * as returned by {@link #get()} will be gradually increasing to simulate the user's possible real-life balance changes.
 * <p>
 * The external code is expected to {@link #set(long)} whatever value it thinks may still be available. For example, if
 * we attempt to invest 600 and Zonky gives us an insufficient balance error, we should {@link #set(long)} 599 as that
 * is a value that can still be available.
 */
final class StatefulBoundedBalance {

    private static final Duration BALANCE_INCREASE_INTERVAL_STEP = Duration.ofMinutes(10);
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
        final long newValue = Math.max(1, value); // if < 1, the multiplication in get() does not increase balance
        final long oldValue = currentValue.getAndSet(newValue);
        if (oldValue == newValue) { // don't update the timestamp, so that the auto-increases in get() work properly
            return;
        }
        lastModificationDate.set(DateUtil.now());
        state.update(m -> m.put(VALUE_KEY, Long.toString(newValue)));
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
        if (elapsedCycles > 12) {
            LOGGER.trace("Resetting balance upper bound as it's been too long since {}.", lastModified);
            return Long.MAX_VALUE;
        }
        final long newBalance = balance * (long) Math.pow(2, elapsedCycles);
        if (newBalance < balance) { // long overflow
            LOGGER.trace("Balance upper bound reached the theoretical limit.");
            return Long.MAX_VALUE;
        } else {
            LOGGER.trace("Changing balance upper bound from {} to {} CZK.", balance, newBalance);
            return newBalance;
        }
    }
}
