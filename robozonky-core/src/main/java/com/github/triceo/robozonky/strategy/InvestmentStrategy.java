/*
 * Copyright 2016 Lukáš Petrovický
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

package com.github.triceo.robozonky.strategy;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.github.triceo.robozonky.remote.Loan;
import com.github.triceo.robozonky.remote.Rating;

/**
 * Determines which loans will be invested into, and how much. What the strategy does or does not allow depends on the
 * {@link InvestmentStrategyService} implementation.
 */
public interface InvestmentStrategy {

    int MINIMAL_INVESTMENT_INCREMENT = 200;

    /**
     * Retrieve a list of loans that are acceptable by the strategy, in the order in which they are to be evaluated.
     *
     * @param availableLoans Loans to be evaluated for acceptability.
     * @param ratingShare How much money is invested in a given rating, compared to the sum total of all investments.
     * @return List of acceptable loans, ordered by their priority.
     */
    List<Loan> getMatchingLoans(List<Loan> availableLoans, Map<Rating, BigDecimal> ratingShare);

    /**
     * Recommend the size of an investment based on loan parameters.
     *
     * @param loan Loan in question.
     * @param availableBalance User's available cash.
     * @return Amount in CZK, recommended to invest.
     */
    int recommendInvestmentAmount(Loan loan, BigDecimal availableBalance);

}
