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

import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.entities.sanitized.MarketplaceLoan;
import com.github.robozonky.notifications.Target;
import com.github.robozonky.test.AbstractRoboZonkyTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class DelinquencyTrackerTest extends AbstractRoboZonkyTest {

    private static final MarketplaceLoan LOAN = MarketplaceLoan.custom().setId(1).setAmount(200).build();
    private static final Investment INVESTMENT = Investment.fresh(LOAN, 200).build();
    private static SessionInfo SESSION = new SessionInfo("someone@robozonky.cz");

    @Test
    void standard() {
        final DelinquencyTracker t = new DelinquencyTracker(Target.EMAIL);
        assertThat(t.isDelinquent(SESSION, INVESTMENT)).isFalse();
        t.setDelinquent(SESSION, INVESTMENT);
        assertThat(t.isDelinquent(SESSION, INVESTMENT)).isTrue();
        t.unsetDelinquent(SESSION, INVESTMENT);
        assertThat(t.isDelinquent(SESSION, INVESTMENT)).isFalse();
    }
}
