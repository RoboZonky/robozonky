/*
 * Copyright 2017 Lukáš Petrovický
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

import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;

import com.github.triceo.robozonky.api.notifications.ExecutionStartedEvent;

public class BalanceUnderMinimumEventListener extends AbstractEmailingListener<ExecutionStartedEvent> {

    private final int minimumBalance;
    private boolean shouldSendEmail = false;

    public BalanceUnderMinimumEventListener(final ListenerSpecificNotificationProperties properties) {
        super(properties);
        this.minimumBalance = properties.getListenerSpecificIntProperty("minimumBalance", 200);
    }

    @Override
    boolean shouldSendEmail(final ExecutionStartedEvent event) {
        return super.shouldSendEmail(event) && this.shouldSendEmail;
    }

    @Override
    String getSubject(final ExecutionStartedEvent event) {
        return "Zůstatek na Zonky účtu klesl pod " + this.minimumBalance + ",- Kč";
    }

    @Override
    String getTemplateFileName() {
        return "under-minimum-balance.ftl";
    }

    @Override
    Map<String, Object> getData(final ExecutionStartedEvent event) {
        final Map<String, Object> result = new HashMap<>();
        result.put("newBalance", event.getBalance());
        result.put("minimumBalance", minimumBalance);
        return result;
    }

    @Override
    public void handle(final ExecutionStartedEvent event) {
        // figure out whether or not the balance is over the threshold
        final OptionalInt lastKnownBalance = BalanceTracker.INSTANCE.getLastKnownBalance();
        final int newBalance = event.getBalance();
        if (newBalance < minimumBalance) {
            this.shouldSendEmail = !lastKnownBalance.isPresent() || lastKnownBalance.getAsInt() >= minimumBalance;
        } else {
            this.shouldSendEmail = false;
        }
        // set the new threshold
        BalanceTracker.INSTANCE.setLastKnownBalance(newBalance);
        // and continue with event-processing, possibly eventually sending the e-mail
        super.handle(event);
    }
}
