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

import java.util.Optional;
import java.util.function.Supplier;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.SessionInfo;

/**
 * Determines which loans will be invested into, and how much.
 * What the strategy does or does not allow depends on the {@link StrategyService} implementation.
 */
public interface InvestmentStrategy {

    /**
     * Determine whether a loan is acceptable by the investment strategy.
     * 
     * @param loanDescriptor            Loan to be evaluated for acceptability.
     * @param portfolioOverviewSupplier Retrieves the latest available information on-demand.
     * @param sessionInfo               Information about the current session.
     * @return If present, the amount which to invest. If empty, do not invest.
     */
    Optional<Money> recommend(LoanDescriptor loanDescriptor, Supplier<PortfolioOverview> portfolioOverviewSupplier,
            SessionInfo sessionInfo);
}
