/*
 * Copyright 2017 Lukáš Petrovický
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

package com.github.triceo.robozonky.notifications.email;

import java.util.Optional;
import java.util.OptionalInt;

import com.github.triceo.robozonky.internal.api.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

enum BalanceTracker {

    INSTANCE; // fast thread-safe singleton

    private static final Logger LOGGER = LoggerFactory.getLogger(BalanceTracker.class);
    private static final State.ClassSpecificState STATE = State.INSTANCE.forClass(BalanceTracker.class);
    static final String BALANCE_KEY = "lastKnownBalance";

    public OptionalInt getLastKnownBalance() {
        final Optional<String> lastKnownBalance = BalanceTracker.STATE.getValue(BalanceTracker.BALANCE_KEY);
        if (!lastKnownBalance.isPresent()) {
            BalanceTracker.LOGGER.debug("No last known balance.");
            return OptionalInt.empty();
        } else try {
            return OptionalInt.of(Integer.parseInt(lastKnownBalance.get()));
        } catch (final Exception ex) {
            BalanceTracker.LOGGER.debug("Failed initializing balance.", ex);
            this.reset();
            return OptionalInt.empty();
        }
    }

    public void setLastKnownBalance(final int newBalance) {
        BalanceTracker.STATE.setValue(BalanceTracker.BALANCE_KEY, String.valueOf(newBalance));
    }

    boolean reset() {
        return BalanceTracker.STATE.reset();
    }

}
