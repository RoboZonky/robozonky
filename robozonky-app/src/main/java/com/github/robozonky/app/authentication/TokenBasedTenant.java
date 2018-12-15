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

package com.github.robozonky.app.authentication;

import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;
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
import com.github.robozonky.common.remote.ApiProvider;
import com.github.robozonky.common.remote.Zonky;
import com.github.robozonky.util.Reloadable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class TokenBasedTenant implements Tenant {

    private static final Logger LOGGER = LoggerFactory.getLogger(TokenBasedTenant.class);
    private static final Restrictions FULLY_RESTRICTED = new Restrictions();

    private final SessionInfo sessionInfo;
    private final ApiProvider apis;
    private final Function<ZonkyScope, ZonkyApiTokenSupplier> supplier;
    private final Map<ZonkyScope, ZonkyApiTokenSupplier> tokens = new EnumMap<>(ZonkyScope.class);
    private final RemoteBalance balance;
    private final Reloadable<Restrictions> restrictions;
    private final StrategyProvider strategyProvider;

    TokenBasedTenant(final SessionInfo sessionInfo, final ApiProvider apis, final StrategyProvider strategyProvider,
                     final Function<ZonkyScope, ZonkyApiTokenSupplier> tokenSupplier) {
        this.strategyProvider = strategyProvider;
        this.apis = apis;
        this.sessionInfo = sessionInfo;
        this.supplier = tokenSupplier;
        this.balance = new RemoteBalanceImpl(this);
        this.restrictions = Reloadable.of(() -> this.call(Zonky::getRestrictions), Duration.ofHours(1));
    }

    @Override
    public Restrictions getRestrictions() {
        return restrictions.get().getOrElseGet(ex -> {
            LOGGER.info("Failed retrieving Zonky restrictions, disabling all operations.", ex);
            return FULLY_RESTRICTED;
        });
    }

    private ZonkyApiTokenSupplier getTokenSupplier(final ZonkyScope scope) {
        return tokens.computeIfAbsent(scope, supplier);
    }

    @Override
    public <T> T call(final Function<Zonky, T> operation, final ZonkyScope scope) {
        return apis.call(operation, getTokenSupplier(scope));
    }

    @Override
    public boolean isAvailable(final ZonkyScope scope) {
        return getTokenSupplier(scope).isAvailable();
    }

    @Override
    public RemoteBalance getBalance() {
        return balance;
    }

    @Override
    public SessionInfo getSessionInfo() {
        return sessionInfo;
    }

    @Override
    public Optional<InvestmentStrategy> getInvestmentStrategy() {
        return strategyProvider.getToInvest();
    }

    @Override
    public Optional<SellStrategy> getSellStrategy() {
        return strategyProvider.getToSell();
    }

    @Override
    public Optional<PurchaseStrategy> getPurchaseStrategy() {
        return strategyProvider.getToPurchase();
    }

    @Override
    public void close() { // cancel existing tokens
        tokens.forEach((k, v) -> v.close());
    }
}
