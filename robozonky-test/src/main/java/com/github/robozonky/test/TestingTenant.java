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

package com.github.robozonky.test;

import java.util.Optional;
import java.util.function.Function;

import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.api.remote.entities.Restrictions;
import com.github.robozonky.api.strategies.InvestmentStrategy;
import com.github.robozonky.api.strategies.PurchaseStrategy;
import com.github.robozonky.api.strategies.SellStrategy;
import com.github.robozonky.common.RemoteBalance;
import com.github.robozonky.common.Tenant;
import com.github.robozonky.common.ZonkyScope;
import com.github.robozonky.common.remote.Zonky;
import com.github.robozonky.common.secrets.SecretProvider;

class TestingTenant implements Tenant {

    private final Zonky zonky;
    private final SessionInfo sessionInfo;
    private final SecretProvider secretProvider;
    private final RemoteBalance balance;

    public TestingTenant(final SessionInfo sessionInfo, final Zonky zonky) {
        this.sessionInfo = sessionInfo;
        this.secretProvider = SecretProvider.inMemory(sessionInfo.getUsername());
        this.zonky = zonky;
        this.balance = AbstractRoboZonkyTest.mockBalance(zonky);
    }

    @Override
    public <T> T call(final Function<Zonky, T> operation, final ZonkyScope scope) {
        return operation.apply(zonky);
    }

    @Override
    public boolean isAvailable(final ZonkyScope scope) {
        return true;
    }

    @Override
    public RemoteBalance getBalance() {
        return balance;
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
    public SecretProvider getSecrets() {
        return secretProvider;
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
    public void close() {
        // no need to do anything here
    }
}
