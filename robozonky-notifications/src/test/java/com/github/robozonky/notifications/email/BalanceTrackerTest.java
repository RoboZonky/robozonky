/*
 * Copyright 2017 The RoboZonky Project
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

package com.github.robozonky.notifications.email;

import java.util.UUID;

import com.github.robozonky.internal.api.State;
import com.github.robozonky.test.AbstractRoboZonkyTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class BalanceTrackerTest extends AbstractRoboZonkyTest {

    @Test
    void lifecycle() {
        assertThat(BalanceTracker.INSTANCE.getLastKnownBalance()).isEmpty();
        // store new value
        final int newBalance = 200;
        BalanceTracker.INSTANCE.setLastKnownBalance(newBalance);
        assertThat(BalanceTracker.INSTANCE.getLastKnownBalance()).isPresent().hasValue(newBalance);
        // overwrite value
        final int newerBalance = 100;
        BalanceTracker.INSTANCE.setLastKnownBalance(newerBalance);
        assertThat(BalanceTracker.INSTANCE.getLastKnownBalance()).isPresent().hasValue(newerBalance);
        assertThat(State.forClass(BalanceTracker.class).getValue(BalanceTracker.BALANCE_KEY)).isPresent();
        // reset value
        assertThat(BalanceTracker.INSTANCE.reset()).isTrue();
        assertThat(BalanceTracker.INSTANCE.getLastKnownBalance()).isEmpty();
    }

    @Test
    void wrongData() {
        final State.ClassSpecificState state = State.forClass(BalanceTracker.class);
        state.newBatch().set(BalanceTracker.BALANCE_KEY, UUID.randomUUID().toString()).call();
        assertThat(BalanceTracker.INSTANCE.getLastKnownBalance()).isEmpty();
    }
}
