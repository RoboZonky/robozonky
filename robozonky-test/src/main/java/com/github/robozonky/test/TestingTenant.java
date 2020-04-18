/*
 * Copyright 2020 The RoboZonky Project
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

import static com.github.robozonky.test.AbstractRoboZonkyTest.SESSION;
import static com.github.robozonky.test.AbstractRoboZonkyTest.mockPortfolio;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.Optional;
import java.util.function.Function;

import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.api.strategies.InvestmentStrategy;
import com.github.robozonky.api.strategies.PurchaseStrategy;
import com.github.robozonky.api.strategies.ReservationStrategy;
import com.github.robozonky.api.strategies.SellStrategy;
import com.github.robozonky.internal.remote.Zonky;
import com.github.robozonky.internal.state.InstanceState;
import com.github.robozonky.internal.state.TenantState;
import com.github.robozonky.internal.tenant.Availability;
import com.github.robozonky.internal.tenant.RemotePortfolio;
import com.github.robozonky.internal.tenant.Tenant;

public class TestingTenant implements Tenant {

    private final Zonky zonky;
    private final SessionInfo sessionInfo;
    private final RemotePortfolio portfolio;
    private final Availability availability = spy(new MyAvailability());

    public TestingTenant(final boolean isDryRun, final Zonky zonky) {
        this.sessionInfo = new SessionInfo(zonky::getConsents, zonky::getRestrictions, SESSION.getUsername(),
                SESSION.getName(), isDryRun);
        this.zonky = zonky;
        this.portfolio = mockPortfolio();
    }

    @Override
    public <T> T call(final Function<Zonky, T> operation) {
        return operation.apply(zonky);
    }

    @Override
    public Availability getAvailability() {
        return availability;
    }

    @Override
    public RemotePortfolio getPortfolio() {
        return portfolio;
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
    public <T> InstanceState<T> getState(final Class<T> clz) {
        return TenantState.of(getSessionInfo())
            .in(clz);
    }

    @Override
    public void close() {
        // no need to do anything here
    }

    private static class MyAvailability implements Availability {

        @Override
        public Instant nextAvailabilityCheck() {
            return Instant.now();
        }

        @Override
        public boolean isAvailable() {
            return false;
        }

        @Override
        public Optional<Instant> registerSuccess() {
            return Optional.empty();
        }

        @Override
        public boolean registerException(final Exception ex) {
            return true;
        }

    }
}
