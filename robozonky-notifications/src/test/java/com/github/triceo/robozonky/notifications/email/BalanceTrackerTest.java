/*
 * Copyright 2016 Lukáš Petrovický
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

import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Assume;
import org.junit.Test;

public class BalanceTrackerTest {

    @After
    public void deleteBalance() {
        BalanceTracker.INSTANCE.reset();
    }

    @Test
    public void initialToSet() {
        Assume.assumeFalse(BalanceTracker.BALANCE_STORE.exists());
        Assertions.assertThat(BalanceTracker.INSTANCE.getLastKnownBalance()).isEmpty();
        final int newBalance = 200;
        BalanceTracker.INSTANCE.setLastKnownBalance(newBalance);
        Assertions.assertThat(BalanceTracker.INSTANCE.getLastKnownBalance()).isPresent().hasValue(newBalance);
        Assertions.assertThat(BalanceTracker.BALANCE_STORE).exists();
    }

}
