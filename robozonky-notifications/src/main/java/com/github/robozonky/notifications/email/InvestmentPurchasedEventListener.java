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

import java.math.BigDecimal;
import java.util.Map;

import com.github.robozonky.api.notifications.InvestmentPurchasedEvent;
import com.github.robozonky.api.remote.entities.Investment;
import com.github.robozonky.util.FinancialCalculator;

class InvestmentPurchasedEventListener extends AbstractBalanceRegisteringEmailingListener<InvestmentPurchasedEvent> {

    public InvestmentPurchasedEventListener(final ListenerSpecificNotificationProperties properties) {
        super(i -> i.getPortfolioOverview().getCzkAvailable(), properties);
    }

    @Override
    String getSubject(final InvestmentPurchasedEvent event) {
        return "Zakoupena participace k půjčce č. " + event.getInvestment().getLoanId();
    }

    @Override
    String getTemplateFileName() {
        return "investment-purchased.ftl";
    }

    @Override
    protected Map<String, Object> getData(final InvestmentPurchasedEvent event) {
        final Investment i = event.getInvestment();
        final Map<String, Object> result = Util.getLoanData(i, event.getLoan());
        result.put("yield", FinancialCalculator.expectedInterestAfterFees(i, event.getPortfolioOverview()));
        final BigDecimal interestRate =
                FinancialCalculator.expectedInterestRateAfterFees(i, event.getPortfolioOverview());
        result.put("relativeYield", interestRate);
        result.put("newBalance", getNewBalance(event));
        return result;
    }
}
