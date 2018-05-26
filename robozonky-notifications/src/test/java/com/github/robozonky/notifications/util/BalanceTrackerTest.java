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

package com.github.robozonky.notifications.util;

import java.util.UUID;

import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.common.state.InstanceState;
import com.github.robozonky.common.state.TenantState;
import com.github.robozonky.test.AbstractRoboZonkyTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class BalanceTrackerTest extends AbstractRoboZonkyTest {

    private static final SessionInfo SESSION = new SessionInfo("someone@robozonky.cz");

    @Test
    void lifecycle() {
        assertThat(BalanceTracker.INSTANCE.getLastKnownBalance(SESSION)).isEmpty();
        // store new value
        final int newBalance = 200;
        BalanceTracker.INSTANCE.setLastKnownBalance(SESSION, newBalance);
        assertThat(BalanceTracker.INSTANCE.getLastKnownBalance(SESSION)).isPresent().hasValue(newBalance);
        // overwrite value
        final int newerBalance = 100;
        BalanceTracker.INSTANCE.setLastKnownBalance(SESSION, newerBalance);
        assertThat(BalanceTracker.INSTANCE.getLastKnownBalance(SESSION)).isPresent().hasValue(newerBalance);
        assertThat(TenantState.of(SESSION).in(BalanceTracker.class).getValue(BalanceTracker.BALANCE_KEY)).isPresent();
    }

    @Test
    void wrongData() {
        final InstanceState<BalanceTracker> state = TenantState.of(SESSION).in(BalanceTracker.class);
        state.reset(b -> b.put(BalanceTracker.BALANCE_KEY, UUID.randomUUID().toString()));
        assertThat(BalanceTracker.INSTANCE.getLastKnownBalance(SESSION)).isEmpty();
    }
}
