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

package com.github.robozonky.notifications.listeners;

import java.util.Optional;
import java.util.OptionalInt;

import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.common.state.TenantState;
import com.github.robozonky.notifications.Target;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class BalanceTracker {

    private static final Logger LOGGER = LoggerFactory.getLogger(BalanceTracker.class);

    private final Target target;

    public BalanceTracker(final Target target) {
        this.target = target;
    }

    public OptionalInt getLastKnownBalance(final SessionInfo sessionInfo) {
        final Optional<String> lastKnownBalance = TenantState.of(sessionInfo)
                .in(BalanceTracker.class)
                .getValue(target.getId());
        if (!lastKnownBalance.isPresent()) {
            BalanceTracker.LOGGER.debug("No last known balance.");
            return OptionalInt.empty();
        } else {
            try {
                return OptionalInt.of(Integer.parseInt(lastKnownBalance.get()));
            } catch (final Exception ex) {
                BalanceTracker.LOGGER.debug("Failed initializing balance.", ex);
                return OptionalInt.empty();
            }
        }
    }

    public void setLastKnownBalance(final SessionInfo sessionInfo, final int newBalance) {
        TenantState.of(sessionInfo)
                .in(BalanceTracker.class)
                .reset(b -> b.put(target.getId(), String.valueOf(newBalance)));
    }

    public static void reset(final SessionInfo sessionInfo) {
        TenantState.of(sessionInfo).in(BalanceTracker.class).reset();
    }

}
