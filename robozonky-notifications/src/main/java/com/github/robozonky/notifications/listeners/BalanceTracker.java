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

package com.github.robozonky.notifications.listeners;

import java.math.BigDecimal;
import java.util.Optional;

import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.internal.state.TenantState;
import com.github.robozonky.notifications.Target;

class BalanceTracker {

    private final Target target;

    public BalanceTracker(final Target target) {
        this.target = target;
    }

    public Optional<BigDecimal> getLastKnownBalance(final SessionInfo sessionInfo) {
        return TenantState.of(sessionInfo)
                .in(BalanceTracker.class)
                .getValue(target.getId())
                .map(BigDecimal::new);
    }

    public void setLastKnownBalance(final SessionInfo sessionInfo, final BigDecimal newBalance) {
        TenantState.of(sessionInfo)
                .in(BalanceTracker.class)
                .update(b -> b.put(target.getId(), String.valueOf(newBalance)));
    }

    public static void reset(final SessionInfo sessionInfo) {
        TenantState.of(sessionInfo).in(BalanceTracker.class).reset();
    }

}
