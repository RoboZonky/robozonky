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

package com.github.robozonky.api.strategies;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.Ratio;
import com.github.robozonky.api.remote.enums.Rating;

import java.time.ZonedDateTime;

/**
 * Class with some aggregate statistics about user's portfolio. Used primarily as the main input into
 * {@link InvestmentStrategy}.
 */
public interface PortfolioOverview {

    /**
     * Sum total of all amounts yet unpaid.
     * @return Amount.
     */
    Money getInvested();

    /**
     * Amount yet unpaid in a given rating.
     * @param r Rating in question.
     * @return Amount.
     */
    Money getInvested(final Rating r);

    /**
     * Retrieve the amounts due in a given rating, divided by {@link #getInvested()}.
     * @param r Rating in question.
     * @return Share of the given rating on overall investments.
     */
    Ratio getShareOnInvestment(final Rating r);

    /**
     * Retrieve annual rate of return of the entire portfolio as reported by Zonky.
     * @return
     */
    Ratio getAnnualProfitability();

    /**
     * Retrieve minimal annual rate of return of the entire portfolio, assuming Zonky rist cost model holds.
     * (See {@link Rating#getMinimalRevenueRate(Money)}.)
     * @return
     */
    Ratio getMinimalAnnualProfitability();

    /**
     * Retrieve maximal annual rate of return of the entire portfolio, assuming none of the loans are ever delinquent.
     * (See {@link Rating#getMaximalRevenueRate(Money)}.)
     * @return
     */
    Ratio getOptimalAnnualProfitability();

    /**
     * Retrieve the expected monthly revenue, based on {@link #getAnnualProfitability()}.
     * @return Amount.
     */
    Money getMonthlyProfit();

    /**
     * Retrieve the expected monthly revenue, based on {@link #getMinimalAnnualProfitability()}.
     * @return Amount.
     */
    Money getMinimalMonthlyProfit();

    /**
     * Retrieve the expected monthly revenue, based on {@link #getOptimalAnnualProfitability()}.
     * @return Amount.
     */
    Money getOptimalMonthlyProfit();

    /**
     * @return When this instance was created.
     */
    ZonedDateTime getTimestamp();
}
