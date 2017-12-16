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

package com.github.robozonky.api.strategies;

import java.util.Collection;
import java.util.stream.Stream;

import com.github.robozonky.api.remote.entities.Restrictions;

/**
 * Determines which loans will be invested into, and how much. What the strategy does or does not allow depends on the
 * {@link StrategyService} implementation.
 */
public interface InvestmentStrategy {

    /**
     * Retrieve loans that are acceptable by the strategy, in the order in which they are to be evaluated. After an
     * investment has been made into any single one of these loans, the strategy should be called again to re-evaluate
     * the resulting situation.
     * @param available Loans to be evaluated for acceptability.
     * @param portfolio Aggregation of information as to the user's current portfolio.
     * @param restrictions Restrictions imposed by Zonky on the current user.
     * @return Acceptable loans, in the order of their decreasing priority, mapped to the recommended investment
     * amounts.
     */
    Stream<RecommendedLoan> recommend(Collection<LoanDescriptor> available, PortfolioOverview portfolio,
                                      Restrictions restrictions);
}
