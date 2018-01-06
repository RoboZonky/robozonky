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

package com.github.robozonky.api.notifications;

import java.util.Collection;
import java.util.Collections;

import com.github.robozonky.api.remote.entities.Investment;
import com.github.robozonky.api.strategies.PortfolioOverview;

/**
 * Fired immediately after the investing algorithm is finished selling participations in the secondary marketplace.
 */
public final class SellingCompletedEvent extends Event implements Financial {

    private final Collection<Investment> investments;
    private final PortfolioOverview portfolioOverview;

    public SellingCompletedEvent(final Collection<Investment> investment, final PortfolioOverview portfolio) {
        this.investments = Collections.unmodifiableCollection(investment);
        this.portfolioOverview = portfolio;
    }

    /**
     * @return The investments that were made.
     */
    public Collection<Investment> getInvestments() {
        return investments;
    }

    @Override
    public PortfolioOverview getPortfolioOverview() {
        return portfolioOverview;
    }
}
