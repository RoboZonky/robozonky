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

import com.github.triceo.robozonky.api.notifications.InvestmentMadeEvent;

final class InvestmentMadeEventListener extends AbstractEmailingListener<InvestmentMadeEvent> {

    public InvestmentMadeEventListener(final ListenerSpecificNotificationProperties properties) {
        super(properties);
    }

    @Override
    boolean shouldSendEmail(final InvestmentMadeEvent event) {
        return true;
    }

    @Override
    String getSubject(final InvestmentMadeEvent event) {
        return "Nová investice - " + event.getInvestment().getAmount() + ",- Kč, půjčka č. " +
                event.getInvestment().getLoanId();
    }

    @Override
    String getTemplateFileName() {
        return "investment-made.ftl";
    }

    @Override
    Map<String, Object> getData(final InvestmentMadeEvent event) {
        final Map<String, Object> result = new HashMap<>();
        result.put("investedAmount", event.getInvestment().getAmount());
        result.put("loanId", event.getInvestment().getLoanId());
        result.put("loanRating", event.getInvestment().getRating().getCode());
        result.put("loanTerm", event.getInvestment().getLoanTermInMonth());
        result.put("newBalance", event.getFinalBalance());
        return result;
    }

    @Override
    public void handle(final InvestmentMadeEvent event) {
        BalanceTracker.INSTANCE.setLastKnownBalance(event.getFinalBalance());
        super.handle(event);
    }

}
