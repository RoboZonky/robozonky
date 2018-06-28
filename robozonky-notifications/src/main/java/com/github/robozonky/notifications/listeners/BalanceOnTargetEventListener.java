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

package com.github.robozonky.notifications.listeners;

import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;

import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.api.notifications.ExecutionStartedEvent;
import com.github.robozonky.notifications.AbstractTargetHandler;
import com.github.robozonky.notifications.SupportedListener;

public class BalanceOnTargetEventListener extends AbstractListener<ExecutionStartedEvent> {

    private final int targetBalance;

    public BalanceOnTargetEventListener(final SupportedListener listener, final AbstractTargetHandler handler) {
        super(listener, handler);
        this.targetBalance = handler.getListenerSpecificIntProperty(SupportedListener.BALANCE_ON_TARGET,
                                                                    "targetBalance", 200);
    }

    @Override
    String getSubject(final ExecutionStartedEvent event) {
        return "Zůstatek na Zonky účtu přesáhl " + this.targetBalance + ",- Kč";
    }

    @Override
    String getTemplateFileName() {
        return "target-balance-reached.ftl";
    }

    @Override
    protected Map<String, Object> getData(final ExecutionStartedEvent event) {
        return new HashMap<String, Object>(super.getData(event)) {{
            put("targetBalance", targetBalance);
        }};
    }

    @Override
    boolean shouldNotify(final ExecutionStartedEvent event, final SessionInfo sessionInfo) {
        final OptionalInt lastKnownBalance = balanceTracker.getLastKnownBalance(sessionInfo);
        final int newBalance = event.getPortfolioOverview().getCzkAvailable();
        LOGGER.debug("Last known balance: {}, target: {}, new: {}.", lastKnownBalance, targetBalance, newBalance);
        final boolean balanceNowExceeded = newBalance > targetBalance;
        final boolean wasFineLastTime = !lastKnownBalance.isPresent() || lastKnownBalance.getAsInt() < targetBalance;
        return (balanceNowExceeded && wasFineLastTime);
    }

}
