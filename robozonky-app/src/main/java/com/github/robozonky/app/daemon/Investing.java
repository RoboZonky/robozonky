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

package com.github.robozonky.app.daemon;

import java.util.Collection;

import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.strategies.InvestmentStrategy;
import com.github.robozonky.api.strategies.LoanDescriptor;
import com.github.robozonky.app.tenant.PowerTenant;

class Investing extends StrategyExecutor<LoanDescriptor, InvestmentStrategy> {

    private final Investor investor;

    public Investing(final Investor investor, final PowerTenant auth) {
        super(auth, auth::getInvestmentStrategy);
        this.investor = investor;
    }

    @Override
    protected boolean isBalanceUnderMinimum(final int current) {
        return current < getTenant().getRestrictions().getMinimumInvestmentAmount();
    }

    @Override
    protected long identify(final LoanDescriptor descriptor) {
        return descriptor.item().getId();
    }

    @Override
    protected Collection<Investment> execute(final InvestmentStrategy strategy,
                                             final Collection<LoanDescriptor> marketplace) {
        return InvestingSession.invest(investor, getTenant(), marketplace, strategy);
    }

}
