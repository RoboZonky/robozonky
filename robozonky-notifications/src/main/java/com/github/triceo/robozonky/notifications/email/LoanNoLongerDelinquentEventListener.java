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

import com.github.triceo.robozonky.api.notifications.LoanNoLongerDelinquentEvent;
import com.github.triceo.robozonky.api.remote.entities.Loan;

public class LoanNoLongerDelinquentEventListener extends AbstractEmailingListener<LoanNoLongerDelinquentEvent> {

    public LoanNoLongerDelinquentEventListener(final ListenerSpecificNotificationProperties properties) {
        super(properties);
    }

    @Override
    String getSubject(final LoanNoLongerDelinquentEvent event) {
        return "Půjčka č. " + event.getLoan().getId() + " již není v prodlení";
    }

    @Override
    String getTemplateFileName() {
        return "loan-not-delinquent.ftl";
    }

    @Override
    protected Map<String, Object> getData(final LoanNoLongerDelinquentEvent event) {
        final Loan loan = event.getLoan();
        final Map<String, Object> result = new HashMap<>();
        result.put("loanId", loan.getId());
        result.put("loanAmount", loan.getAmount());
        result.put("loanRating", loan.getRating().getCode());
        result.put("loanTerm", loan.getTermInMonths());
        result.put("loanUrl", Loan.getUrlSafe(loan));
        return result;
    }
}
