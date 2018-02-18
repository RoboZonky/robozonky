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
import java.util.Optional;
import java.util.function.Supplier;

import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.strategies.InvestmentStrategy;
import com.github.robozonky.api.strategies.LoanDescriptor;
import com.github.robozonky.app.authentication.Authenticated;
import com.github.robozonky.app.portfolio.Portfolio;
import com.github.robozonky.app.util.StrategyExecutor;

public class Investing extends StrategyExecutor<LoanDescriptor, InvestmentStrategy> {

    private final Authenticated auth;
    private final Investor.Builder investor;

    public Investing(final Investor.Builder investor, final Supplier<Optional<InvestmentStrategy>> strategy,
                     final Authenticated auth, final TemporalAmount maximumSleepPeriod) {
        super((l) -> new Activity(l, maximumSleepPeriod), strategy);
        this.auth = auth;
        this.investor = investor;
    }

    @Override
    protected int identify(final LoanDescriptor item) {
        return item.item().getId();
    }

    @Override
    protected Collection<Investment> execute(final Portfolio portfolio, final InvestmentStrategy strategy,
                                             final Collection<LoanDescriptor> marketplace) {
        final RestrictedInvestmentStrategy s = new RestrictedInvestmentStrategy(strategy, auth.getRestrictions());
        return auth.call(zonky -> Session.invest(portfolio, investor.build(zonky), zonky, marketplace, s));
    }
}
