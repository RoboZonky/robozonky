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

import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.notifications.Target;
import com.github.robozonky.test.AbstractRoboZonkyTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class BalanceTrackerTest extends AbstractRoboZonkyTest {

    private static final SessionInfo SESSION = new SessionInfo("someone@robozonky.cz");

    @Test
    void lifecycle() {
        final BalanceTracker b = new BalanceTracker(Target.EMAIL);
        assertThat(b.getLastKnownBalance(SESSION)).isEmpty();
        // store new value
        final BigDecimal newBalance = BigDecimal.valueOf(200);
        b.setLastKnownBalance(SESSION, newBalance);
        assertThat(b.getLastKnownBalance(SESSION)).isPresent().hasValue(newBalance);
        // overwrite value
        final BigDecimal newerBalance = BigDecimal.valueOf(100);
        b.setLastKnownBalance(SESSION, newerBalance);
        assertThat(b.getLastKnownBalance(SESSION)).isPresent().hasValue(newerBalance);
        BalanceTracker.reset(SESSION);
        assertThat(b.getLastKnownBalance(SESSION)).isEmpty();
    }
}
