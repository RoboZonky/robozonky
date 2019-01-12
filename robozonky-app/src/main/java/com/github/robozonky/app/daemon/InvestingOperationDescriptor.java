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

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.stream.Stream;

import com.github.robozonky.api.strategies.InvestmentStrategy;
import com.github.robozonky.api.strategies.LoanDescriptor;
import com.github.robozonky.common.remote.Select;
import com.github.robozonky.common.tenant.Tenant;
import com.github.robozonky.internal.util.DateUtil;

class InvestingOperationDescriptor implements OperationDescriptor<LoanDescriptor, InvestmentStrategy> {

    /**
     * Will make sure that the endpoint only loads loans that are on the marketplace, and not the entire history.
     */
    private static final Select SELECT = new Select().greaterThan("nonReservedRemainingInvestment", 0);
    private final Duration refreshInterval;
    private final Investor investor;

    public InvestingOperationDescriptor(final Investor investor, final Duration refreshInterval) {
        this.investor = investor;
        this.refreshInterval = refreshInterval;
    }

    public InvestingOperationDescriptor(final Investor investor) {
        this(investor, Duration.ZERO);
    }

    public InvestingOperationDescriptor() {
        this(null);
    }

    private static boolean isActionable(final LoanDescriptor loanDescriptor) {
        final OffsetDateTime now = DateUtil.offsetNow();
        return loanDescriptor.getLoanCaptchaProtectionEndDateTime()
                .map(d -> d.isBefore(now))
                .orElse(true);
    }

    @Override
    public boolean isEnabled(final Tenant tenant) {
        return !tenant.getRestrictions().isCannotInvest();
    }

    @Override
    public Optional<InvestmentStrategy> getStrategy(final Tenant tenant) {
        return tenant.getInvestmentStrategy();
    }

    @Override
    public Duration getRefreshInterval() {
        return refreshInterval;
    }

    @Override
    public Stream<LoanDescriptor> readMarketplace(final Tenant tenant) {
        return tenant.call(zonky -> zonky.getAvailableLoans(SELECT))
                .filter(l -> !l.getMyInvestment().isPresent()) // re-investing would fail
                .map(LoanDescriptor::new)
                .filter(InvestingOperationDescriptor::isActionable);
    }

    @Override
    public BigDecimal getMinimumBalance(final Tenant tenant) {
        return BigDecimal.valueOf(tenant.getRestrictions().getMinimumInvestmentAmount());
    }

    @Override
    public long identify(final LoanDescriptor descriptor) {
        return descriptor.item().getId();
    }

    @Override
    public Operation<LoanDescriptor, InvestmentStrategy> getOperation() {
        return (a, b, c) -> InvestingSession.invest(investor, a, b, c);
    }
}
