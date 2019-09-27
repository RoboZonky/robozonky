/*
 * Copyright 2019 The RoboZonky Project
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

package com.github.robozonky.app.events.impl;

import com.github.robozonky.api.notifications.ReservationCheckCompletedEvent;
import com.github.robozonky.api.remote.entities.Investment;
import com.github.robozonky.api.strategies.PortfolioOverview;

import java.util.Collection;
import java.util.Collections;

final class ReservationCheckCompletedEventImpl extends AbstractEventImpl implements ReservationCheckCompletedEvent {

    private final Collection<Investment> investments;
    private final PortfolioOverview portfolioOverview;

    public ReservationCheckCompletedEventImpl(final Collection<Investment> investment, final PortfolioOverview portfolio) {
        super("investments");
        this.investments = Collections.unmodifiableCollection(investment);
        this.portfolioOverview = portfolio;
    }

    @Override
    public PortfolioOverview getPortfolioOverview() {
        return portfolioOverview;
    }

    @Override
    public Collection<Investment> getInvestments() {
        return investments;
    }
}
