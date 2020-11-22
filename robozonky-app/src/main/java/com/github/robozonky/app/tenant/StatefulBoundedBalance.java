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

package com.github.robozonky.app.tenant;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.robozonky.api.Money;
import com.github.robozonky.internal.Defaults;
import com.github.robozonky.internal.state.InstanceState;
import com.github.robozonky.internal.tenant.Tenant;
import com.github.robozonky.internal.test.DateUtil;

/**
 * On Zonky, we do not know the user's current account balance. However, in order for the robot to be able to function
 * effectively and not repeat useless Zonky calls etc., some information about balance is still required. This class
 * helps with that.
 * <p>
 * By default, the balance is {@link Integer#MAX_VALUE}. Every time {@link #set(Money)} is called, the balance is set to
 * this new value. Also, a timer is reset - unless {@link #set(Money)} is called within reasonable time, the balance
 * as returned by {@link #get()} will be increased back to max.
 * <p>
 * The external code is expected to {@link #set(Money)} whatever value it thinks may still be available. For example, if
 * we attempt to invest 600 and Zonky gives us an insufficient balance error, we should {@link #set(Money)} 599 as that
 * is a value that can still be available.
 */
final class StatefulBoundedBalance {

    static final Money MAXIMUM = Money.from(Integer.MAX_VALUE);
    private static final Duration BALANCE_INCREASE_INTERVAL_STEP = Duration.ofMinutes(10);
    private static final String VALUE_KEY = "lastKnownUpperBound";
    private static final Logger LOGGER = LogManager.getLogger(StatefulBoundedBalance.class);

    private final InstanceState<StatefulBoundedBalance> state;
    private final AtomicReference<Money> currentValue = new AtomicReference<>();
    private final AtomicReference<ZonedDateTime> lastModificationDate = new AtomicReference<>();

    public StatefulBoundedBalance(final Tenant tenant) {
        this.state = tenant.getState(StatefulBoundedBalance.class);
        reloadFromState();
    }

    private synchronized void reloadFromState() {
        var lastModified = state.getLastUpdated()
            .map(i -> i.atZoneSameInstant(Defaults.ZONKYCZ_ZONE_ID))
            .orElseGet(() -> ZonedDateTime.ofInstant(Instant.EPOCH, Defaults.ZONKYCZ_ZONE_ID));
        lastModificationDate.set(lastModified);
        var lastKnownValue = state.getValue(VALUE_KEY)
            .map(Money::from)
            .orElse(MAXIMUM);
        currentValue.set(lastKnownValue);
        LOGGER.trace("Loaded {} from {}", lastKnownValue, lastModified);
    }

    public synchronized Money set(final Money value) {
        var newValue = value.max(Money.from(1));
        currentValue.set(newValue);
        lastModificationDate.set(DateUtil.zonedNow());
        state.update(m -> m.put(VALUE_KEY, newValue.getValue()
            .toPlainString()));
        return newValue;
    }

    private Duration getTimeBetweenLastBalanceCheckAndNow() {
        var lastModified = lastModificationDate.get();
        return Duration.between(DateUtil.zonedNow(), lastModified)
            .abs();
    }

    public synchronized Money get() {
        var balance = currentValue.get();
        if (balance.equals(MAXIMUM)) { // no need to do any other magic
            LOGGER.trace("Balance already at maximum.");
            return balance;
        }
        var lastModified = lastModificationDate.get();
        var timeBetweenLastBalanceCheckAndNow = getTimeBetweenLastBalanceCheckAndNow();
        if (timeBetweenLastBalanceCheckAndNow.compareTo(BALANCE_INCREASE_INTERVAL_STEP) < 0) {
            LOGGER.trace("Balance of {} is still fresh ({}).", balance, lastModified);
            return balance;
        }
        LOGGER.trace("Resetting balance upper bound as it's been too long since {}.", lastModified);
        return set(MAXIMUM);
    }
}
