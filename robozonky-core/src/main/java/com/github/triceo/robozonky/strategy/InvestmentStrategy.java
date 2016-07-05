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

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;

import com.github.triceo.robozonky.PortfolioOverview;
import com.github.triceo.robozonky.remote.Loan;

/**
 * Determines which loans will be invested into, and how much. What the strategy does or does not allow depends on the
 * {@link InvestmentStrategyService} implementation.
 */
public interface InvestmentStrategy {

    int MINIMAL_INVESTMENT_ALLOWED = 200;
    int MINIMAL_INVESTMENT_INCREMENT = 200;

    /**
     * Load the correct strategy using Java's {@link ServiceLoader}.
     * @param file Investment strategy configuration file.
     * @return Strategy to read that file, if found.
     * @throws InvestmentStrategyParseException When problem found when parsing the strategy configuration file.
     */
    static Optional<InvestmentStrategy> load(final File file) throws InvestmentStrategyParseException {
        return InvestmentStrategyLoader.load(file);
    }

    /**
     * Retrieve a list of loans that are acceptable by the strategy, in the order in which they are to be evaluated.
     * After an investment has been made into any single one of these loans, the strategy should be called again to
     * re-evaluate the resulting situation.
     *
     * @param availableLoans Loans to be evaluated for acceptability.
     * @param portfolio Aggregation of information as to the sser's current portolio.
     * @return List of acceptable loans, ordered by their priority.
     */
    List<Loan> getMatchingLoans(List<Loan> availableLoans, PortfolioOverview portfolio);

    /**
     * Recommend the size of an investment based on loan parameters.
     *
     * @param loan Loan in question.
     * @param portfolio Aggregation of information as to the sser's current portolio.
     * @return Amount in CZK, recommended to invest.
     */
    int recommendInvestmentAmount(Loan loan, PortfolioOverview portfolio);

}
