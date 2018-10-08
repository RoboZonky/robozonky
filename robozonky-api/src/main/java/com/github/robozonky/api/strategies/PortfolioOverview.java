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

package com.github.robozonky.api.strategies;

import java.math.BigDecimal;

import com.github.robozonky.api.remote.enums.Rating;

/**
 * Class with some aggregate statistics about user's portfolio. Used primarily as the main input into
 * {@link InvestmentStrategy}.
 */
public interface PortfolioOverview {

    /**
     * Available balance in the wallet.
     * @return Amount in CZK.
     */
    BigDecimal getCzkAvailable();

    /**
     * Sum total of all amounts yet unpaid.
     * @return Amount in CZK.
     */
    BigDecimal getCzkInvested();

    /**
     * Amount yet unpaid in a given rating.
     * @param r Rating in question.
     * @return Amount in CZK.
     */
    BigDecimal getCzkInvested(final Rating r);

    /**
     * Sum total of all remaining principal where loans are currently overdue.
     * @return Amount in CZK.
     */
    BigDecimal getCzkAtRisk();

    /**
     * How much is at risk out of the entire portfolio, in relative terms.
     * @return Percentage.
     */
    BigDecimal getShareAtRisk();

    /**
     * Sum total of all remaining principal where loans in a given rating are currently overdue.
     * @param r Rating in question.
     * @return Amount in CZK.
     */
    BigDecimal getCzkAtRisk(final Rating r);

    /**
     * Retrieve the amounts due in a given rating, divided by {@link #getCzkInvested()}.
     * @param r Rating in question.
     * @return Share of the given rating on overall investments.
     */
    BigDecimal getShareOnInvestment(final Rating r);

    /**
     * Retrieve the amounts due in a given rating, divided by {@link #getCzkInvested()}.
     * @param r Rating in question.
     * @return Share of the given rating on overall investments.
     */
    BigDecimal getAtRiskShareOnInvestment(final Rating r);
}
