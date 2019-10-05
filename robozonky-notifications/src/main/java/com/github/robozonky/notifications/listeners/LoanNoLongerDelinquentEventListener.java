/*
 * Copyright 2019 The RoboZonky Project
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

import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.api.notifications.LoanNoLongerDelinquentEvent;
import com.github.robozonky.notifications.AbstractTargetHandler;
import com.github.robozonky.notifications.SupportedListener;

public class LoanNoLongerDelinquentEventListener extends AbstractListener<LoanNoLongerDelinquentEvent> {

    public LoanNoLongerDelinquentEventListener(final SupportedListener listener, final AbstractTargetHandler handler) {
        super(listener, handler);
    }

    @Override
    boolean shouldNotify(final LoanNoLongerDelinquentEvent event, final SessionInfo sessionInfo) {
        return super.shouldNotify(event, sessionInfo) && delinquencyTracker.isDelinquent(sessionInfo,
                                                                                         event.getInvestment());
    }

    @Override
    public String getSubject(final LoanNoLongerDelinquentEvent event) {
        return "Půjčka " + Util.identifyLoan(event) + " již není v prodlení";
    }

    @Override
    public String getTemplateFileName() {
        return "loan-not-delinquent.ftl";
    }
}
