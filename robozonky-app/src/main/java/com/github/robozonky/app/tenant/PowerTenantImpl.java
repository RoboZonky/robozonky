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

package com.github.robozonky.app.tenant;

import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Supplier;

import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.api.notifications.SessionEvent;
import com.github.robozonky.api.remote.entities.Restrictions;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.remote.enums.OAuthScope;
import com.github.robozonky.api.strategies.InvestmentStrategy;
import com.github.robozonky.api.strategies.PurchaseStrategy;
import com.github.robozonky.api.strategies.ReservationStrategy;
import com.github.robozonky.api.strategies.SellStrategy;
import com.github.robozonky.app.events.Events;
import com.github.robozonky.common.async.Reloadable;
import com.github.robozonky.common.remote.ApiProvider;
import com.github.robozonky.common.remote.Zonky;
import com.github.robozonky.common.state.InstanceState;
import com.github.robozonky.common.state.TenantState;
import com.github.robozonky.common.tenant.LazyEvent;
import com.github.robozonky.common.tenant.RemotePortfolio;
import io.vavr.Lazy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

class PowerTenantImpl implements PowerTenant {

    private static final Logger LOGGER = LogManager.getLogger(PowerTenantImpl.class);
    private static final Restrictions FULLY_RESTRICTED = new Restrictions();

    private final SessionInfo sessionInfo;
    private final ApiProvider apis;
    private final Function<OAuthScope, ZonkyApiTokenSupplier> supplier;
    private final BooleanSupplier availability;
    private final Map<OAuthScope, ZonkyApiTokenSupplier> tokens = new EnumMap<>(OAuthScope.class);
    private final RemotePortfolio portfolio;
    private final Reloadable<Restrictions> restrictions;
    private final Lazy<StrategyProvider> strategyProvider;
    private final Lazy<LoanCache> loanCache = Lazy.of(() -> new LoanCache(this));

    PowerTenantImpl(final SessionInfo sessionInfo, final ApiProvider apis, final BooleanSupplier zonkyAvailability,
                    final Supplier<StrategyProvider> strategyProvider,
                    final Function<OAuthScope, ZonkyApiTokenSupplier> tokenSupplier) {
        this.strategyProvider = Lazy.of(strategyProvider);
        this.apis = apis;
        this.sessionInfo = sessionInfo;
        this.availability = zonkyAvailability;
        this.supplier = tokenSupplier;
        this.portfolio = new RemotePortfolioImpl(this);
        this.restrictions = Reloadable.with(() -> this.call(Zonky::getRestrictions))
                .reloadAfter(Duration.ofHours(1))
                .build();
    }

    @Override
    public Restrictions getRestrictions() {
        return restrictions.get().getOrElseGet(ex -> {
            LOGGER.info("Failed retrieving Zonky restrictions, disabling all operations.", ex);
            return FULLY_RESTRICTED;
        });
    }

    private ZonkyApiTokenSupplier getTokenSupplier(final OAuthScope scope) {
        return tokens.computeIfAbsent(scope, supplier);
    }

    @Override
    public <T> T call(final Function<Zonky, T> operation, final OAuthScope scope) {
        return apis.call(operation, getTokenSupplier(scope));
    }

    @Override
    public boolean isAvailable(final OAuthScope scope) {
        // either Zonky is not available, or we have already logged out prior to daemon shutdown
        return availability.getAsBoolean() && !getTokenSupplier(scope).isClosed();
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
        return strategyProvider.get().getToInvest();
    }

    @Override
    public Optional<SellStrategy> getSellStrategy() {
        return strategyProvider.get().getToSell();
    }

    @Override
    public Optional<PurchaseStrategy> getPurchaseStrategy() {
        return strategyProvider.get().getToPurchase();
    }

    @Override
    public Optional<ReservationStrategy> getReservationStrategy() {
        return strategyProvider.get().getForReservations();
    }

    @Override
    public Loan getLoan(final int loanId) {
        return loanCache.get().getLoan(loanId);
    }

    @Override
    public <T> InstanceState<T> getState(final Class<T> clz) {
        return TenantState.of(getSessionInfo()).in(clz);
    }

    @Override
    public void close() {
        tokens.forEach((k, v) -> v.close()); // cancel existing tokens
        loanCache.get().close(); // clean up the cache
    }

    @Override
    public Runnable fire(final SessionEvent event) {
        return Events.forSession(this).fire(event);
    }

    @Override
    public Runnable fire(final LazyEvent<? extends SessionEvent> event) {
        return Events.forSession(this).fire(event);
    }

    @Override
    public String toString() {
        return "PowerTenantImpl{" +
                "sessionInfo=" + sessionInfo +
                '}';
    }
}
