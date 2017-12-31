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

import com.github.robozonky.api.remote.entities.Investment;
import com.github.robozonky.api.strategies.PortfolioOverview;

/**
 * Fired immediately after an investment was submitted to the API.
 */
public final class InvestmentMadeEvent extends Event {

    private final Investment investment;
    private final PortfolioOverview portfolioOverview;
    private final boolean dryRun;

    public InvestmentMadeEvent(final Investment investment, final PortfolioOverview portfolioOverview,
                               final boolean isDryRun) {
        this.investment = investment;
        this.portfolioOverview = portfolioOverview;
        this.dryRun = isDryRun;
    }

    /**
     * @return The investment that was made.
     */
    public Investment getInvestment() {
        return this.investment;
    }

    /**
     *
     * @return Status of the portfolio after the investment was made.
     */
    public PortfolioOverview getPortfolioOverview() {
        return portfolioOverview;
    }

    /**
     * @return True if investment was only simulated.
     */
    public boolean isDryRun() {
        return dryRun;
    }
}
