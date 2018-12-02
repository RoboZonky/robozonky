/*
 * Copyright 2018 The RoboZonky Project
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

import com.github.robozonky.api.notifications.LoanDelinquentEvent;
import com.github.robozonky.notifications.AbstractTargetHandler;
import com.github.robozonky.notifications.SupportedListener;

public class LoanDelinquentEventListener extends AbstractListener<LoanDelinquentEvent> {

    public LoanDelinquentEventListener(final SupportedListener listener, final AbstractTargetHandler handler) {
        super(listener, handler);
    }

    @Override
    String getSubject(final LoanDelinquentEvent event) {
        final int threshold = event.getThresholdInDays();
        if (threshold == 0) {
            return "Půjčka " + Util.identifyLoan(event) + " je nově v prodlení";
        } else {
            return "Půjčka " + Util.identifyLoan(event) + " je " + threshold + " dní v prodlení";
        }
    }

    @Override
    String getTemplateFileName() {
        return "loan-delinquent.ftl";
    }

    @Override
    protected Map<String, Object> getData(final LoanDelinquentEvent event) {
        return Util.getDelinquentData(event.getInvestment(), event.getLoan(), event.getCollectionActions(),
                                      event.getDelinquentSince());
    }
}
