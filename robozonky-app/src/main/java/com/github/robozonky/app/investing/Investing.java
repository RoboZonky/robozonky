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

package com.github.robozonky.app.investing;

import java.time.temporal.TemporalAmount;
import java.util.Collection;

import com.github.robozonky.api.Refreshable;
import com.github.robozonky.api.remote.entities.Investment;
import com.github.robozonky.api.strategies.InvestmentStrategy;
import com.github.robozonky.api.strategies.LoanDescriptor;
import com.github.robozonky.app.util.StrategyExecutor;
import com.github.robozonky.common.remote.Zonky;

public class Investing extends StrategyExecutor<LoanDescriptor, InvestmentStrategy> {

    private final Zonky zonky;
    private final Investor.Builder investor;

    public Investing(final Investor.Builder investor, final Refreshable<InvestmentStrategy> strategy,
                     final Zonky zonky, final TemporalAmount maximumSleepPeriod) {
        super((l) -> new Activity(l, maximumSleepPeriod), strategy);
        this.zonky = zonky;
        this.investor = investor;
    }

    @Override
    protected int identify(final LoanDescriptor item) {
        return item.item().getId();
    }

    @Override
    protected Collection<Investment> execute(final InvestmentStrategy strategy,
                                             final Collection<LoanDescriptor> marketplace) {
        return Session.invest(investor, zonky, marketplace, strategy);
    }
}
