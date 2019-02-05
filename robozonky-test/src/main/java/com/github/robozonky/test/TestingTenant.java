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

package com.github.robozonky.test;

import java.util.Optional;
import java.util.function.Function;

import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.api.remote.entities.Restrictions;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.remote.enums.OAuthScope;
import com.github.robozonky.api.strategies.InvestmentStrategy;
import com.github.robozonky.api.strategies.PurchaseStrategy;
import com.github.robozonky.api.strategies.ReservationStrategy;
import com.github.robozonky.api.strategies.SellStrategy;
import com.github.robozonky.common.remote.Zonky;
import com.github.robozonky.common.state.InstanceState;
import com.github.robozonky.common.state.TenantState;
import com.github.robozonky.common.tenant.RemotePortfolio;
import com.github.robozonky.common.tenant.Tenant;

public class TestingTenant implements Tenant {

    private final Zonky zonky;
    private final SessionInfo sessionInfo;
    private final RemotePortfolio portfolio;

    public TestingTenant(final SessionInfo sessionInfo, final Zonky zonky) {
        this.sessionInfo = sessionInfo;
        this.zonky = zonky;
        this.portfolio = AbstractRoboZonkyTest.mockPortfolio(zonky);
    }

    @Override
    public <T> T call(final Function<Zonky, T> operation, final OAuthScope scope) {
        return operation.apply(zonky);
    }

    @Override
    public boolean isAvailable(final OAuthScope scope) {
        return true;
    }

    @Override
    public RemotePortfolio getPortfolio() {
        return portfolio;
    }

    @Override
    public Restrictions getRestrictions() {
        return zonky.getRestrictions();
    }

    @Override
    public SessionInfo getSessionInfo() {
        return sessionInfo;
    }

    @Override
    public Optional<InvestmentStrategy> getInvestmentStrategy() {
        return Optional.empty();
    }

    @Override
    public Optional<SellStrategy> getSellStrategy() {
        return Optional.empty();
    }

    @Override
    public Optional<PurchaseStrategy> getPurchaseStrategy() {
        return Optional.empty();
    }

    @Override
    public Optional<ReservationStrategy> getReservationStrategy() {
        return Optional.empty();
    }

    @Override
    public Loan getLoan(final int loanId) {
        return call(z -> z.getLoan(loanId));
    }

    @Override
    public <T> InstanceState<T> getState(final Class<T> clz) {
        return TenantState.of(getSessionInfo()).in(clz);
    }

    @Override
    public void close() {
        // no need to do anything here
    }
}
