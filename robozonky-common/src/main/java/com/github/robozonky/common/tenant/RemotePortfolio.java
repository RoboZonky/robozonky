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

package com.github.robozonky.common.tenant;

import java.math.BigDecimal;
import java.util.Map;

import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.PortfolioOverview;

public interface RemotePortfolio {

    /**
     * Adjust {@link #getTotal()} and {@link #getBalance()} temporarily to reflect an operation performed by the robot
     * that was not yet retrieved from the remote API.
     * @param loanId
     * @param rating
     * @param amount
     */
    void simulateCharge(final int loanId, final Rating rating, final BigDecimal amount);

    BigDecimal getBalance();

    Map<Rating, BigDecimal> getTotal();

    Map<Rating, BigDecimal> getAtRisk();

    /**
     * Takes {@link #getBalance()}, {@link #getTotal()} and {@link #getAtRisk()} and summarizes them.
     * @return
     */
    PortfolioOverview getOverview();
}
