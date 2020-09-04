/*
 * Copyright 2020 The RoboZonky Project
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

import java.math.BigDecimal;

import com.github.robozonky.api.notifications.InvestmentSoldEvent;
import com.github.robozonky.api.remote.entities.Investment;
import com.github.robozonky.notifications.AbstractTargetHandler;
import com.github.robozonky.notifications.SupportedListener;

public class InvestmentSoldEventListener extends AbstractListener<InvestmentSoldEvent> {

    public InvestmentSoldEventListener(final SupportedListener listener, final AbstractTargetHandler handler) {
        super(listener, handler);
    }

    @Override
    public String getSubject(final InvestmentSoldEvent event) {
        final Investment i = event.getInvestment();
        // TODO Convince Zonky to include the actual sell price in the new API.
        final BigDecimal remaining = i.getPrincipal()
            .getUnpaid()
            .getValue();
        return "Participace prodána - " + remaining.intValue() + ",- Kč, půjčka " + Util.identifyLoan(event);
    }

    @Override
    public String getTemplateFileName() {
        return "investment-sold.ftl";
    }

}
