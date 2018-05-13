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

package com.github.robozonky.api.notifications;

import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.strategies.PortfolioOverview;

/**
 * Fired immediately after an investment was submitted to the API.
 */
public final class InvestmentMadeEvent extends Event implements InvestmentBased,
                                                                LoanBased,
                                                                Financial {

    private final Investment investment;
    private final Loan loan;
    private final PortfolioOverview portfolioOverview;

    public InvestmentMadeEvent(final Investment investment, final Loan loan,
                               final PortfolioOverview portfolioOverview) {
        this.investment = investment;
        this.loan = loan;
        this.portfolioOverview = portfolioOverview;
    }

    @Override
    public Loan getLoan() {
        return loan;
    }

    @Override
    public Investment getInvestment() {
        return this.investment;
    }

    @Override
    public PortfolioOverview getPortfolioOverview() {
        return portfolioOverview;
    }
}
