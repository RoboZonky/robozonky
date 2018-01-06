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

import com.github.robozonky.api.remote.entities.Investment;
import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.internal.api.State;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class DelinquencyTrackerTest {

    private static final Investment INVESTMENT = new Investment(new Loan(1, 200), 200);

    @After
    @Before
    public void reset() {
        State.forClass(DelinquencyTracker.class).newBatch(true).call();
    }

    @Test
    public void standard() {
        final DelinquencyTracker t = DelinquencyTracker.INSTANCE;
        Assertions.assertThat(t.isDelinquent(INVESTMENT)).isFalse();
        Assertions.assertThat(t.setDelinquent(INVESTMENT)).isTrue();
        Assertions.assertThat(t.isDelinquent(INVESTMENT)).isTrue();
        Assertions.assertThat(t.unsetDelinquent(INVESTMENT)).isTrue();
        Assertions.assertThat(t.isDelinquent(INVESTMENT)).isFalse();
    }

    @Test
    public void settingSet() {
        final DelinquencyTracker t = DelinquencyTracker.INSTANCE;
        Assertions.assertThat(t.setDelinquent(INVESTMENT)).isTrue();
        Assertions.assertThat(t.setDelinquent(INVESTMENT)).isFalse();
        this.reset();
        Assertions.assertThat(t.isDelinquent(INVESTMENT)).isFalse();
    }

    @Test
    public void unsettingNeverSet() {
        final DelinquencyTracker t = DelinquencyTracker.INSTANCE;
        Assertions.assertThat(t.unsetDelinquent(INVESTMENT)).isFalse();
    }
}
