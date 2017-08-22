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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.github.robozonky.api.notifications.LoanDelinquentEvent;
import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.internal.api.Defaults;

class LoanDelinquentEventListener extends AbstractEmailingListener<LoanDelinquentEvent> {

    public LoanDelinquentEventListener(final ListenerSpecificNotificationProperties properties) {
        super(properties);
    }

    @Override
    String getSubject(final LoanDelinquentEvent event) {
        final int threshold = event.getThresholdInDays();
        if (threshold == 0) {
            return "Půjčka č. " + event.getLoan().getId() + " je nově v prodlení";
        } else {
            return "Půjčka č. " + event.getLoan().getId() + " je " + threshold + " dní v prodlení";
        }
    }

    @Override
    String getTemplateFileName() {
        return "loan-delinquent.ftl";
    }

    @Override
    protected Map<String, Object> getData(final LoanDelinquentEvent event) {
        final Loan loan = event.getLoan();
        final Map<String, Object> result = new HashMap<>();
        result.put("loanId", loan.getId());
        result.put("loanAmount", loan.getAmount());
        result.put("loanRating", loan.getRating().getCode());
        result.put("loanTerm", loan.getTermInMonths());
        result.put("loanUrl", Loan.getUrlSafe(loan));
        result.put("since", Date.from(event.getDelinquentSince().atStartOfDay(Defaults.ZONE_ID).toInstant()));
        return result;
    }
}
