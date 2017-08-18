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

package com.github.triceo.robozonky.notifications.email;

import java.util.HashMap;
import java.util.Map;

import com.github.triceo.robozonky.api.notifications.InvestmentSoldEvent;
import com.github.triceo.robozonky.api.notifications.SessionInfo;
import com.github.triceo.robozonky.api.remote.entities.Investment;

final class InvestmentSoldEventListener extends AbstractEmailingListener<InvestmentSoldEvent> {

    public InvestmentSoldEventListener(final ListenerSpecificNotificationProperties properties) {
        super(properties);
    }

    @Override
    String getSubject(final InvestmentSoldEvent event) {
        return "Participace prodána - " + event.getInvestment().getAmount() + ",- Kč, půjčka č. " +
                event.getInvestment().getLoanId();
    }

    @Override
    String getTemplateFileName() {
        return "investment-sold.ftl";
    }

    @Override
    protected Map<String, Object> getData(final InvestmentSoldEvent event) {
        final Investment i = event.getInvestment();
        final Map<String, Object> result = new HashMap<>();
        result.put("investedAmount", i.getAmount());
        result.put("loanId", i.getLoanId());
        result.put("loanRating", i.getRating().getCode());
        result.put("loanTerm", i.getLoanTermInMonth());
        result.put("loanUrl", AbstractEmailingListener.getLoanUrl(i));
        result.put("newBalance", event.getFinalBalance());
        return result;
    }

    @Override
    public void handle(final InvestmentSoldEvent event, final SessionInfo sessionInfo) {
        BalanceTracker.INSTANCE.setLastKnownBalance(event.getFinalBalance());
        super.handle(event, sessionInfo);
    }
}
