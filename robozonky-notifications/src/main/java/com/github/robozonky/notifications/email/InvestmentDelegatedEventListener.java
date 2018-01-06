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

import java.util.Map;

import com.github.robozonky.api.notifications.InvestmentDelegatedEvent;

class InvestmentDelegatedEventListener extends AbstractEmailingListener<InvestmentDelegatedEvent> {

    public InvestmentDelegatedEventListener(final ListenerSpecificNotificationProperties properties) {
        super(properties);
    }

    @Override
    String getSubject(final InvestmentDelegatedEvent event) {
        return "Investice delegována - " + event.getRecommendation().intValue() + ",- Kč, " +
                "půjčka č. " + event.getLoan().getId();
    }

    @Override
    String getTemplateFileName() {
        return "investment-delegated.ftl";
    }

    @Override
    protected Map<String, Object> getData(final InvestmentDelegatedEvent event) {
        final Map<String, Object> result = Util.getLoanData(event.getLoan());
        result.put("loanRecommendation", event.getRecommendation());
        result.put("confirmationProviderId", event.getConfirmationProviderId());
        return result;
    }
}
