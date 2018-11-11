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

package com.github.robozonky.app.daemon.operations;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.strategies.InvestmentStrategy;
import com.github.robozonky.api.strategies.LoanDescriptor;
import com.github.robozonky.app.daemon.Portfolio;
import com.github.robozonky.common.Tenant;
import com.github.robozonky.util.NumberUtil;

public class Investing extends StrategyExecutor<LoanDescriptor, InvestmentStrategy> {

    private static final long[] NO_LONGS = new long[0];
    private final Tenant auth;
    private final Investor investor;
    private final AtomicReference<long[]> actionableWhenLastChecked = new AtomicReference<>(NO_LONGS);

    public Investing(final Investor investor, final Supplier<Optional<InvestmentStrategy>> strategy,
                     final Tenant auth) {
        super(strategy);
        this.auth = auth;
        this.investor = investor;
    }

    private static boolean isActionable(final LoanDescriptor loanDescriptor) {
        final OffsetDateTime now = OffsetDateTime.now();
        return loanDescriptor.getLoanCaptchaProtectionEndDateTime()
                .map(d -> d.isBefore(now))
                .orElse(true);
    }

    @Override
    protected boolean isBalanceUnderMinimum(final int current) {
        return current < auth.getRestrictions().getMinimumInvestmentAmount();
    }

    @Override
    protected boolean hasMarketplaceUpdates(final Collection<LoanDescriptor> marketplace) {
        final long[] actionableLoansNow = marketplace.stream()
                .filter(Investing::isActionable)
                .mapToLong(l -> l.item().getId())
                .toArray();
        final long[] lastCheckedActionableLoans = actionableWhenLastChecked.getAndSet(actionableLoansNow);
        return NumberUtil.hasAdditions(lastCheckedActionableLoans, actionableLoansNow);
    }

    @Override
    protected Collection<Investment> execute(final Portfolio portfolio, final InvestmentStrategy strategy,
                                             final Collection<LoanDescriptor> marketplace) {
        return InvestingSession.invest(portfolio, investor, auth, marketplace, strategy);
    }
}
