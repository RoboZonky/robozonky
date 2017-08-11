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

package com.github.triceo.robozonky.api.notifications;

import java.util.Collection;
import java.util.Collections;

import com.github.triceo.robozonky.api.strategies.InvestmentDescriptor;
import com.github.triceo.robozonky.api.strategies.PortfolioOverview;

/**
 * Fired immediately before existing investments are submitted to the selling algorithm.
 * Will eventually be followed by {@link SellingCompletedEvent}.
 */
public final class SellingStartedEvent extends Event {

    private final Collection<InvestmentDescriptor> descriptors;
    private final PortfolioOverview portfolioOverview;

    public SellingStartedEvent(final Collection<InvestmentDescriptor> descriptors,
                               final PortfolioOverview portfolioOverview) {
        this.descriptors = Collections.unmodifiableCollection(descriptors);
        this.portfolioOverview = portfolioOverview;
    }

    /**
     * @return Participations on the secondary marketplace that are available for robotic investment.
     */
    public Collection<InvestmentDescriptor> getDescriptors() {
        return this.descriptors;
    }

    public PortfolioOverview getPortfolioOverview() {
        return portfolioOverview;
    }
}
