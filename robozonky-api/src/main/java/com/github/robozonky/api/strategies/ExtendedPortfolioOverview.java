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

/**
 * Class with some aggregate statistics about user's portfolio. Used primarily as the main input into
 * {@link InvestmentStrategy}.
 */
public interface ExtendedPortfolioOverview extends PortfolioOverview {

    /**
     * Sum total of all remaining principal where loans are currently overdue.
     * @return Amount.
     */
    Money getAtRisk();

    /**
     * How much is at risk out of the entire portfolio, in relative terms.
     * @return Percentage.
     */
    Ratio getShareAtRisk();

    /**
     * Sum total of all remaining principal where loans in a given rating are currently overdue.
     * @param r Rating in question.
     * @return Amount.
     */
    Money getAtRisk(final Rating r);

    /**
     * Retrieve the amounts due in a given rating, divided by {@link #getInvested()}.
     * @param r Rating in question.
     * @return Share of the given rating on overall investments.
     */
    Ratio getAtRiskShareOnInvestment(final Rating r);

    /**
     * Sum total of all remaining principal which can be sold right now.
     * @return Amount.
     */
    Money getSellable();

    /**
     * How much can be sold of the entire portfolio, in relative terms.
     * @return Percentage.
     */
    Ratio getShareSellable();

    /**
     * Sum total of all remaining principal which can be sold right now in a given rating.
     * @param r Rating in question.
     * @return Amount.
     */
    Money getSellable(final Rating r);

    /**
     * Retrieve the sellable in a given rating, divided by {@link #getInvested(Rating)}.
     * @param r Rating in question.
     * @return Share of sellable on overall investments in a given rating.
     */
    Ratio getShareSellable(final Rating r);

    /**
     * Sum total of all remaining principal which can be sold right now, without sale fees.
     * @return Amount.
     */
    Money getSellableFeeless();

    /**
     * How much can be sold of the entire portfolio without fees, in relative terms.
     * @return Percentage.
     */
    Ratio getShareSellableFeeless();

    /**
     * Sum total of all remaining principal which can be sold right now without fees in a given rating.
     * @param r Rating in question.
     * @return Amount.
     */
    Money getSellableFeeless(final Rating r);

    /**
     * Retrieve the sellable without fees in a given rating, divided by {@link #getInvested(Rating)}.
     * @param r Rating in question.
     * @return Share of sellable on overall investments in a given rating.
     */
    Ratio getShareSellableFeeless(final Rating r);

}
