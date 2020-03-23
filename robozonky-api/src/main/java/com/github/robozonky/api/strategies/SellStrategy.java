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

package com.github.robozonky.api.strategies;

import java.util.Collection;
import java.util.stream.Stream;

/**
 * Determines which investments will be sold, and for how much. What the strategy does or does not allow depends
 * on the {@link StrategyService} implementation.
 */
public interface SellStrategy {

    /**
     * Retrieve investments that are acceptable by the strategy, in the order in which they are to be sold. After
     * selling any of these investments, the strategy should be called again to re-evaluate the resulting situation.
     * 
     * @param available Investments to be evaluated for acceptability.
     * @param portfolio Aggregation of information as to the user's current portfolio.
     * @return Acceptable investments, in the order of their decreasing priority, mapped to the recommended sell price.
     */
    Stream<RecommendedInvestment> recommend(Collection<InvestmentDescriptor> available, PortfolioOverview portfolio);
}
