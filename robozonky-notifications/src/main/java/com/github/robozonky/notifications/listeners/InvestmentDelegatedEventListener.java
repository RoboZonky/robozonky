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

import java.util.Map;

import com.github.robozonky.api.notifications.InvestmentDelegatedEvent;
import com.github.robozonky.notifications.AbstractTargetHandler;
import com.github.robozonky.notifications.SupportedListener;

public class InvestmentDelegatedEventListener extends AbstractListener<InvestmentDelegatedEvent> {

    public InvestmentDelegatedEventListener(final SupportedListener listener, final AbstractTargetHandler handler) {
        super(listener, handler);
    }

    @Override
    String getSubject(final InvestmentDelegatedEvent event) {
        return "Investice delegována - " + event.getRecommendation().intValue() + ",- Kč, " + "půjčka "
                + Util.identifyLoan(event);
    }

    @Override
    String getTemplateFileName() {
        return "investment-delegated.ftl";
    }

    @Override
    protected Map<String, Object> getData(final InvestmentDelegatedEvent event) {
        final Map<String, Object> result = super.getData(event);
        result.put("loanRecommendation", event.getRecommendation());
        result.put("confirmationProviderId", event.getConfirmationProviderId());
        return result;
    }
}
