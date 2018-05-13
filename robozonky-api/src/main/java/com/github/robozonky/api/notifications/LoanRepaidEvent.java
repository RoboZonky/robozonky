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

package com.github.robozonky.api.notifications;

import com.github.robozonky.api.remote.entities.RawInvestment;
import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.strategies.PortfolioOverview;

/**
 * Fired immediately after an {@link RawInvestment} is identified as having been fully repaid.
 */
public final class LoanRepaidEvent extends Event implements InvestmentBased,
                                                            Financial {

    private final Investment investment;
    private final PortfolioOverview portfolioOverview;

    public LoanRepaidEvent(final Investment investment, final PortfolioOverview portfolioOverview) {
        this.investment = investment;
        this.portfolioOverview = portfolioOverview;
    }

    @Override
    public Investment getInvestment() {
        return investment;
    }

    @Override
    public PortfolioOverview getPortfolioOverview() {
        return portfolioOverview;
    }
}
