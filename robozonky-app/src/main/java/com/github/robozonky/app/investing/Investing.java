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

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
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
    private final AtomicReference<int[]> actionableWhenLastChecked = new AtomicReference<>(new int[0]);

    public Investing(final Investor.Builder investor, final Supplier<Optional<InvestmentStrategy>> strategy,
                     final Authenticated auth) {
        super(strategy);
        this.auth = auth;
        this.investor = investor;
    }

    @Override
    protected boolean isBalanceUnderMinimum(final int current) {
        return current < auth.getRestrictions().getMinimumInvestmentAmount();
    }

    @Override
    protected boolean hasMarketplaceUpdates(final Collection<LoanDescriptor> marketplace) {
        final OffsetDateTime now = OffsetDateTime.now();
        final int[] actionableLoansNow = marketplace.stream()
                .filter(l -> l.getLoanCaptchaProtectionEndDateTime()
                        .map(d -> d.isBefore(now))
                        .orElse(true)
                ).mapToInt(l -> l.item().getId()).toArray();
        final int[] lastCheckedActionableLoans = actionableWhenLastChecked.getAndSet(actionableLoansNow);
        return StrategyExecutor.hasNewIds(lastCheckedActionableLoans, actionableLoansNow);
    }

    @Override
    protected Collection<Investment> execute(final Portfolio portfolio, final InvestmentStrategy strategy,
                                             final Collection<LoanDescriptor> marketplace) {
        final RestrictedInvestmentStrategy s = new RestrictedInvestmentStrategy(strategy, auth.getRestrictions());
        return Session.invest(portfolio, investor.build(auth), auth, marketplace, s);
    }
}
