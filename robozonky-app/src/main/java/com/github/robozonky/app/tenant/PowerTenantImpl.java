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

package com.github.robozonky.app.tenant;

import java.time.Duration;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.api.notifications.SessionEvent;
import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.api.remote.entities.Restrictions;
import com.github.robozonky.api.remote.entities.SellInfo;
import com.github.robozonky.api.strategies.InvestmentStrategy;
import com.github.robozonky.api.strategies.PurchaseStrategy;
import com.github.robozonky.api.strategies.ReservationStrategy;
import com.github.robozonky.api.strategies.SellStrategy;
import com.github.robozonky.app.events.Events;
import com.github.robozonky.internal.async.Reloadable;
import com.github.robozonky.internal.remote.ApiProvider;
import com.github.robozonky.internal.remote.Zonky;
import com.github.robozonky.internal.state.InstanceState;
import com.github.robozonky.internal.state.TenantState;
import com.github.robozonky.internal.tenant.Availability;
import com.github.robozonky.internal.tenant.LazyEvent;
import com.github.robozonky.internal.tenant.RemotePortfolio;
import io.vavr.Lazy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

class PowerTenantImpl implements PowerTenant {

    private static final Logger LOGGER = LogManager.getLogger(PowerTenantImpl.class);

    private final Random random = new Random();
    private final SessionInfo sessionInfo;
    private final ApiProvider apis;
    private final Runnable quotaMonitor;
    private final RemotePortfolio portfolio;
    private final Reloadable<Restrictions> restrictions;
    private final Lazy<ZonkyApiTokenSupplier> token;
    private final StrategyProvider strategyProvider;
    private final Lazy<Cache<Loan>> loanCache = Lazy.of(() -> Cache.forLoan(this));
    private final Lazy<Cache<SellInfo>> sellInfoCache = Lazy.of(() -> Cache.forSellInfo(this));
    private final StatefulBoundedBalance balance;
    private final Lazy<Availability> availability;

    PowerTenantImpl(final SessionInfo sessionInfo, final ApiProvider apis, final StrategyProvider strategyProvider,
                    final Supplier<ZonkyApiTokenSupplier> tokenSupplier) {
        this.strategyProvider = strategyProvider;
        this.apis = apis;
        this.quotaMonitor = apis.getRequestCounter()
                .map(r -> (Runnable) new QuotaMonitor(r))
                .orElse(() -> {
                    // do nothing
                });
        this.sessionInfo = sessionInfo;
        this.token = Lazy.of(tokenSupplier);
        this.availability = Lazy.of(() -> new AvailabilityImpl(token.get(), apis.getRequestCounter().orElse(null)));
        this.portfolio = new RemotePortfolioImpl(this);
        this.restrictions = Reloadable.with(() -> this.call(Zonky::getRestrictions))
                .reloadAfter(Duration.ofHours(1))
                .build();
        this.balance = new StatefulBoundedBalance(this);
    }

    @Override
    public Restrictions getRestrictions() {
        return restrictions.get()
                .getOrElseThrow(t -> new IllegalStateException("Failed retrieving Restrictions from Zonky.", t));
    }

    @Override
    public <T> T call(final Function<Zonky, T> operation) {
        try {
            return apis.call(operation, token.get());
        } finally {
            final int randomBetweenZeroAndHundred = random.nextInt(100);
            if (randomBetweenZeroAndHundred == 0) { // check request situation in 1 % of cases
                quotaMonitor.run();
            }
        }
    }

    @Override
    public Availability getAvailability() {
        return availability.get();
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
    public Optional<ReservationStrategy> getReservationStrategy() {
        return strategyProvider.getForReservations();
    }

    @Override
    public Loan getLoan(final int loanId) {
        return loanCache.get().get(loanId);
    }

    @Override
    public SellInfo getSellInfo(final long investmentId) {
        return sellInfoCache.get().get(investmentId);
    }

    @Override
    public <T> InstanceState<T> getState(final Class<T> clz) {
        return TenantState.of(getSessionInfo()).in(clz);
    }

    @Override
    public void close() {
        Stream.of(token, loanCache, sellInfoCache)
                .filter(lazy -> !lazy.isEmpty())
                .forEach(lazy -> {
                    try {
                        lazy.get().close();
                    } catch (final Exception ex) {
                        LOGGER.debug("Failed closing tenant.", ex);
                    }
                });
    }

    @Override
    public Money getKnownBalanceUpperBound() {
        return balance.get();
    }

    @Override
    public void setKnownBalanceUpperBound(final Money knownBalanceUpperBound) {
        balance.set(knownBalanceUpperBound);
    }

    @Override
    public CompletableFuture<?> fire(final SessionEvent event) {
        return Events.forSession(this).fire(event);
    }

    @Override
    public CompletableFuture<?> fire(final LazyEvent<? extends SessionEvent> event) {
        return Events.forSession(this).fire(event);
    }

    @Override
    public String toString() {
        return "PowerTenantImpl{" +
                "sessionInfo=" + sessionInfo +
                '}';
    }
}
