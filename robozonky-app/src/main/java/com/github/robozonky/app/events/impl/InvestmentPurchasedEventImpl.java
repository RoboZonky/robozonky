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

package com.github.robozonky.app.events.impl;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.notifications.InvestmentPurchasedEvent;
import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.api.remote.entities.Participation;
import com.github.robozonky.api.strategies.PortfolioOverview;

final class InvestmentPurchasedEventImpl extends AbstractEventImpl implements InvestmentPurchasedEvent {

    private final Participation participation;
    private final Loan loan;
    private final Money purchasedAmount;
    private final PortfolioOverview portfolioOverview;

    public InvestmentPurchasedEventImpl(final Participation participation, final Loan loan, final Money amount,
                                        final PortfolioOverview portfolio) {
        this.participation = participation;
        this.loan = loan;
        this.purchasedAmount = amount;
        this.portfolioOverview = portfolio;
    }

    @Override
    public Loan getLoan() {
        return this.loan;
    }

    @Override
    public Money getPurchasedAmount() {
        return purchasedAmount;
    }

    @Override
    public PortfolioOverview getPortfolioOverview() {
        return portfolioOverview;
    }

    @Override
    public Participation getParticipation() {
        return participation;
    }
}
