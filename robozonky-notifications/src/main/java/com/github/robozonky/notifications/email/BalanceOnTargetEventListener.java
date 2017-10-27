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

import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;

import com.github.robozonky.api.notifications.ExecutionStartedEvent;
import com.github.robozonky.api.notifications.SessionInfo;

class BalanceOnTargetEventListener extends AbstractBalanceRegisteringEmailingListener<ExecutionStartedEvent> {

    private final int targetBalance;
    private boolean shouldSendEmail = false;

    public BalanceOnTargetEventListener(final ListenerSpecificNotificationProperties properties) {
        super((ExecutionStartedEvent e) -> e.getPortfolioOverview().getCzkAvailable(), properties);
        this.targetBalance = properties.getListenerSpecificIntProperty("targetBalance", 200);
    }

    @Override
    boolean shouldSendEmail(final ExecutionStartedEvent event) {
        return super.shouldSendEmail(event) && this.shouldSendEmail;
    }

    @Override
    String getSubject(final ExecutionStartedEvent event) {
        return "Zůstatek na Zonky účtu dosáhl " + this.targetBalance + ",- Kč";
    }

    @Override
    String getTemplateFileName() {
        return "target-balance-reached.ftl";
    }

    @Override
    protected Map<String, Object> getData(final ExecutionStartedEvent event) {
        return new HashMap<String, Object>() {{
            put("newBalance", getNewBalance(event));
            put("targetBalance", targetBalance);
        }};
    }

    @Override
    public void handle(final ExecutionStartedEvent event, final SessionInfo sessionInfo) {
        // figure out whether or not the balance is over the threshold
        final OptionalInt lastKnownBalance = BalanceTracker.INSTANCE.getLastKnownBalance();
        final int newBalance = event.getPortfolioOverview().getCzkAvailable();
        if (newBalance >= targetBalance) {
            this.shouldSendEmail = !lastKnownBalance.isPresent() || lastKnownBalance.getAsInt() < targetBalance;
        } else {
            this.shouldSendEmail = false;
        }
        // and continue with event-processing, possibly eventually sending the e-mail
        super.handle(event, sessionInfo);
    }
}
