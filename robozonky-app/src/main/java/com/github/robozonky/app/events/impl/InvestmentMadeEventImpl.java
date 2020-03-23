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

package com.github.robozonky.app.events.impl;

import java.util.StringJoiner;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.notifications.InvestmentMadeEvent;
import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.api.strategies.PortfolioOverview;

final class InvestmentMadeEventImpl extends AbstractEventImpl implements InvestmentMadeEvent {

    private final Loan loan;
    private final Money investedAmount;
    private final PortfolioOverview portfolioOverview;

    public InvestmentMadeEventImpl(final Loan loan, final Money amount, final PortfolioOverview portfolioOverview) {
        this.loan = loan;
        this.investedAmount = amount;
        this.portfolioOverview = portfolioOverview;
    }

    @Override
    public Loan getLoan() {
        return loan;
    }

    @Override
    public Money getInvestedAmount() {
        return investedAmount;
    }

    @Override
    public PortfolioOverview getPortfolioOverview() {
        return portfolioOverview;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", InvestmentMadeEventImpl.class.getSimpleName() + "[", "]")
            .add("super=" + super.toString())
            .add("loan=" + loan)
            .add("investedAmount=" + investedAmount)
            .add("portfolioOverview=" + portfolioOverview)
            .toString();
    }
}
