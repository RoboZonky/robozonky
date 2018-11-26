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
import com.github.robozonky.common.state.TenantState;
import com.github.robozonky.internal.util.DateUtil;
import com.github.robozonky.notifications.Target;

class DelinquencyTracker {

    private final Target target;

    public DelinquencyTracker(final Target target) {
        this.target = target;
    }

    private String toId(final Investment investment) {
        return target.getId() + "-" + investment.getLoanId();
    }

    public void setDelinquent(final SessionInfo sessionInfo, final Investment investment) {
        if (this.isDelinquent(sessionInfo, investment)) {
            return;
        }
        TenantState.of(sessionInfo)
                .in(DelinquencyTracker.class)
                .update(b -> b.put(toId(investment), DateUtil.offsetNow().toString()));
    }

    public void unsetDelinquent(final SessionInfo sessionInfo, final Investment investment) {
        if (!this.isDelinquent(sessionInfo, investment)) {
            return;
        }
        TenantState.of(sessionInfo)
                .in(DelinquencyTracker.class)
                .update(b -> b.remove(toId(investment)));
    }

    public boolean isDelinquent(final SessionInfo sessionInfo, final Investment investment) {
        return TenantState.of(sessionInfo)
                .in(DelinquencyTracker.class)
                .getValue(toId(investment))
                .isPresent();
    }
}
