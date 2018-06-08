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

import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.api.notifications.LoanDefaultedEvent;
import com.github.robozonky.notifications.AbstractTargetHandler;
import com.github.robozonky.notifications.SupportedListener;

public class LoanDefaultedEventListener extends AbstractListener<LoanDefaultedEvent> {

    public LoanDefaultedEventListener(final SupportedListener listener, final AbstractTargetHandler handler) {
        super(listener, handler);
    }

    @Override
    protected void finish(final LoanDefaultedEvent event, final SessionInfo sessionInfo) {
        super.finish(event, sessionInfo);
        delinquencyTracker.setDelinquent(sessionInfo, event.getInvestment());
    }

    @Override
    String getSubject(final LoanDefaultedEvent event) {
        return "Půjčka " + Util.identifyLoan(event) + " byla zesplatněna";
    }

    @Override
    String getTemplateFileName() {
        return "loan-defaulted.ftl";
    }
}
