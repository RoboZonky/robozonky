/*
 * Copyright 2017 Lukáš Petrovický
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

package com.github.triceo.robozonky.api.strategies;

import java.util.Collection;
import java.util.stream.Stream;

/**
 * Determines which participation will be purchased, and for how much. What the strategy does or does not allow depends
 * on the {@link StrategyService} implementation.
 */
public interface PurchaseStrategy {

    /**
     * Retrieve participations that are acceptable by the strategy, in the order in which they are to be evaluated.
     * After purchasing any one of these participations, the strategy should be called again to re-evaluate the
     * resulting situation.
     * @param available Participations to be evaluated for acceptability.
     * @param portfolio Aggregation of information as to the user's current portfolio.
     * @return Acceptable participations, in the order of their decreasing priority, mapped to the recommended
     * purchase value.
     */
    Stream<RecommendedParticipation> recommend(Collection<ParticipationDescriptor> available,
                                               PortfolioOverview portfolio);
}
