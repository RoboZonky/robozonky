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

package com.github.robozonky.app.events;

import java.util.Collection;
import java.util.Collections;

import com.github.robozonky.api.notifications.ExecutionCompletedEvent;
import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.strategies.PortfolioOverview;

final class ExecutionCompletedEventImpl extends AbstractEventImpl implements ExecutionCompletedEvent {

    private final Collection<Investment> investments;
    private final PortfolioOverview portfolioOverview;

    public ExecutionCompletedEventImpl(final Collection<Investment> investment, final PortfolioOverview portfolio) {
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
