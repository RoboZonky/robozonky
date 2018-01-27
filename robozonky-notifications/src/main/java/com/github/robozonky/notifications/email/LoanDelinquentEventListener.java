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

import com.github.robozonky.api.notifications.LoanDelinquentEvent;

class LoanDelinquentEventListener extends AbstractEmailingListener<LoanDelinquentEvent> {

    public LoanDelinquentEventListener(final ListenerSpecificNotificationProperties properties) {
        super(properties);
        registerFinisher(event -> DelinquencyTracker.INSTANCE.setDelinquent(event.getInvestment()));
    }

    @Override
    String getSubject(final LoanDelinquentEvent event) {
        final int threshold = event.getThresholdInDays();
        final int loanId = event.getInvestment().getLoanId();
        if (threshold == 0) {
            return "Půjčka č. " + loanId + " je nově v prodlení";
        } else {
            return "Půjčka č. " + loanId + " je " + threshold + " dní v prodlení";
        }
    }

    @Override
    String getTemplateFileName() {
        return "loan-delinquent.ftl";
    }

    @Override
    protected Map<String, Object> getData(final LoanDelinquentEvent event) {
        return Util.getDelinquentData(event.getInvestment(), event.getLoan(), event.getDelinquentSince());
    }
}
