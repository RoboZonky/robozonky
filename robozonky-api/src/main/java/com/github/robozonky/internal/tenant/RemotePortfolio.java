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

package com.github.robozonky.internal.tenant;

import java.util.Map;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.PortfolioOverview;

public interface RemotePortfolio {

    /**
     * Adjust {@link #getTotal()} temporarily to reflect an operation performed by the robot that was not yet retrieved
     * from the remote API.
     * 
     * @param loanId ID of the loan.
     * @param rating Rating of the loan.
     * @param amount Amount of the charge.
     */
    void simulateCharge(final int loanId, final Rating rating, final Money amount);

    Map<Rating, Money> getTotal();

    /**
     * Takes {@link #getTotal()} and summarizes it.
     * 
     * @return never null
     */
    PortfolioOverview getOverview();
}
