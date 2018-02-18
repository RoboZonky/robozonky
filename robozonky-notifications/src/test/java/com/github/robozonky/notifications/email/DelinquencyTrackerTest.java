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

import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.entities.sanitized.MarketplaceLoan;
import com.github.robozonky.internal.api.State;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class DelinquencyTrackerTest {

    private static final MarketplaceLoan LOAN = MarketplaceLoan.custom().setId(1).setAmount(200).build();
    private static final Investment INVESTMENT = Investment.fresh(LOAN, 200).build();

    @AfterEach
    @BeforeEach
    void reset() {
        State.forClass(DelinquencyTracker.class).newBatch(true).call();
    }

    @Test
    void standard() {
        final DelinquencyTracker t = DelinquencyTracker.INSTANCE;
        assertThat(t.isDelinquent(INVESTMENT)).isFalse();
        assertThat(t.setDelinquent(INVESTMENT)).isTrue();
        assertThat(t.isDelinquent(INVESTMENT)).isTrue();
        assertThat(t.unsetDelinquent(INVESTMENT)).isTrue();
        assertThat(t.isDelinquent(INVESTMENT)).isFalse();
    }

    @Test
    void settingSet() {
        final DelinquencyTracker t = DelinquencyTracker.INSTANCE;
        assertThat(t.setDelinquent(INVESTMENT)).isTrue();
        assertThat(t.setDelinquent(INVESTMENT)).isFalse();
        this.reset();
        assertThat(t.isDelinquent(INVESTMENT)).isFalse();
    }

    @Test
    void unsettingNeverSet() {
        final DelinquencyTracker t = DelinquencyTracker.INSTANCE;
        assertThat(t.unsetDelinquent(INVESTMENT)).isFalse();
    }
}
