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

import java.time.OffsetDateTime;

import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.internal.api.Defaults;
import com.github.robozonky.internal.api.State;

enum DelinquencyTracker {

    INSTANCE; // fast thread-safe singleton

    private static final State.ClassSpecificState STATE = State.forClass(DelinquencyTracker.class);

    private static String loanToId(final Loan loan) {
        return String.valueOf(loan.getId());
    }

    public boolean setDelinquent(final Loan loan) {
        if (this.isDelinquent(loan)) {
            return false;
        }
        return DelinquencyTracker.STATE
                .newBatch()
                .set(loanToId(loan), OffsetDateTime.now(Defaults.ZONE_ID).toString())
                .call();
    }

    public boolean unsetDelinquent(final Loan loan) {
        if (!this.isDelinquent(loan)) {
            return false;
        }
        return DelinquencyTracker.STATE
                .newBatch()
                .unset(loanToId(loan))
                .call();
    }

    public boolean isDelinquent(final Loan loan) {
        return DelinquencyTracker.STATE.getValue(loanToId(loan)).isPresent();
    }

}
