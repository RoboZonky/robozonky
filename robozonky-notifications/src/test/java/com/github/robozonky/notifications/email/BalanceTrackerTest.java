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
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

public class BalanceTrackerTest {

    @After
    public void deleteBalance() {
        BalanceTracker.INSTANCE.reset();
    }

    @Before
    public void makeSureNoState() {
        final State.ClassSpecificState state = State.forClass(BalanceTracker.class);
        Assume.assumeFalse(state.getValue(BalanceTracker.BALANCE_KEY).isPresent());
    }

    @Test
    public void lifecycle() {
        System.out.println("A");
        Assume.assumeFalse(State.forClass(BalanceTracker.class).getValue(BalanceTracker.BALANCE_KEY).isPresent());
        Assertions.assertThat(BalanceTracker.INSTANCE.getLastKnownBalance()).isEmpty();
        // store new value
        System.out.println("B");
        final int newBalance = 200;
        BalanceTracker.INSTANCE.setLastKnownBalance(newBalance);
        Assertions.assertThat(BalanceTracker.INSTANCE.getLastKnownBalance()).isPresent().hasValue(newBalance);
        // overwrite value
        System.out.println("C");
        final int newerBalance = 100;
        BalanceTracker.INSTANCE.setLastKnownBalance(newerBalance);
        Assertions.assertThat(BalanceTracker.INSTANCE.getLastKnownBalance()).isPresent().hasValue(newerBalance);
        Assertions.assertThat(State.forClass(BalanceTracker.class).getValue(BalanceTracker.BALANCE_KEY)).isPresent();
        System.out.println("D");
        // reset value
        Assertions.assertThat(BalanceTracker.INSTANCE.reset()).isTrue();
        Assertions.assertThat(BalanceTracker.INSTANCE.getLastKnownBalance()).isEmpty();
    }

    @Test
    public void wrongData() {
        final State.ClassSpecificState state = State.forClass(BalanceTracker.class);
        state.newBatch().set(BalanceTracker.BALANCE_KEY, UUID.randomUUID().toString()).call();
        Assertions.assertThat(BalanceTracker.INSTANCE.getLastKnownBalance()).isEmpty();
    }
}
