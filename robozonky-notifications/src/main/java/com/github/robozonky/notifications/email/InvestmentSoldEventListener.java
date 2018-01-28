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

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.Period;
import java.util.Map;

import com.github.robozonky.api.notifications.InvestmentSoldEvent;
import com.github.robozonky.api.remote.entities.Investment;
import com.github.robozonky.util.FinancialCalculator;

class InvestmentSoldEventListener extends AbstractBalanceRegisteringEmailingListener<InvestmentSoldEvent> {

    public InvestmentSoldEventListener(final ListenerSpecificNotificationProperties properties) {
        super(i -> i.getPortfolioOverview().getCzkAvailable(), properties);
    }

    @Override
    String getSubject(final InvestmentSoldEvent event) {
        final Investment i = event.getInvestment();
        return "Participace prodána - " + i.getRemainingPrincipal().intValue() + ",- Kč, půjčka č. " + i.getLoanId();
    }

    @Override
    String getTemplateFileName() {
        return "investment-sold.ftl";
    }

    private long monthsBetweenNowAnd(final OffsetDateTime then) {
        final LocalDate thenDate = then.toLocalDate();
        final LocalDate now = LocalDate.now();
        return Period.between(now, thenDate).toTotalMonths();
    }

    @Override
    protected Map<String, Object> getData(final InvestmentSoldEvent event) {
        final Investment i = event.getInvestment();
        final Map<String, Object> result = Util.getLoanData(i, event.getLoan());
        result.put("yield",
                   FinancialCalculator.actualInterestAfterFees(event.getInvestment(), event.getPortfolioOverview(),
                                                               true));
        result.put("newBalance", getNewBalance(event));
        return result;
    }
}
