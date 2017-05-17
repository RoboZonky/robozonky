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

import com.github.triceo.robozonky.api.notifications.InvestmentSkippedEvent;
import com.github.triceo.robozonky.api.remote.entities.Loan;

final class InvestmentSkippedEventListener extends AbstractEmailingListener<InvestmentSkippedEvent> {

    public InvestmentSkippedEventListener(final ListenerSpecificNotificationProperties properties) {
        super(properties);
    }

    @Override
    String getSubject(final InvestmentSkippedEvent event) {
        return "Půjčka č. " + event.getRecommendation().getLoanDescriptor().getLoan().getId() + " dočasně přeskočena";
    }

    @Override
    String getTemplateFileName() {
        return "investment-skipped.ftl";
    }

    @Override
    Map<String, Object> getData(final InvestmentSkippedEvent event) {
        final Loan loan = event.getRecommendation().getLoanDescriptor().getLoan();
        final Map<String, Object> result = new HashMap<>();
        result.put("loanId", loan.getId());
        result.put("loanRecommendation", event.getRecommendation().getRecommendedInvestmentAmount());
        result.put("loanAmount", loan.getAmount());
        result.put("loanRating", loan.getRating().getCode());
        result.put("loanTerm", loan.getTermInMonths());
        result.put("loanUrl", Loan.getUrlSafe(loan));
        return result;
    }

}
