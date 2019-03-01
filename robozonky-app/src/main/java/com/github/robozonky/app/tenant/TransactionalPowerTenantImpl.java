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

import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;

import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.api.notifications.SessionEvent;
import com.github.robozonky.api.remote.entities.Restrictions;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.remote.enums.OAuthScope;
import com.github.robozonky.api.strategies.InvestmentStrategy;
import com.github.robozonky.api.strategies.PurchaseStrategy;
import com.github.robozonky.api.strategies.ReservationStrategy;
import com.github.robozonky.api.strategies.SellStrategy;
import com.github.robozonky.common.async.Reloadable;
import com.github.robozonky.common.remote.Zonky;
import com.github.robozonky.common.state.InstanceState;
import com.github.robozonky.common.tenant.LazyEvent;
import com.github.robozonky.common.tenant.RemotePortfolio;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

class TransactionalPowerTenantImpl implements TransactionalPowerTenant {

    private static final Logger LOGGER = LogManager.getLogger(TransactionalPowerTenantImpl.class);

    private final PowerTenant parent;
    private final Reloadable<DelayedFiring> delayedFiring = Reloadable.with(DelayedFiring::new)
            .build();
    private final Queue<Runnable> stateUpdates = new ConcurrentLinkedQueue<>();

    public TransactionalPowerTenantImpl(final PowerTenant parent) {
        this.parent = parent;
    }

    private DelayedFiring getDelayedFiring() {
        return delayedFiring.get().getOrElseThrow(() -> new IllegalStateException("Can not happen."));
    }

    @Override
    public Runnable fire(final SessionEvent event) {
        LOGGER.trace("Event stored within transaction: {}.", event);
        return getDelayedFiring().delay(() -> parent.fire(event));
    }

    @Override
    public Runnable fire(final LazyEvent<? extends SessionEvent> event) {
        LOGGER.trace("Lazy event stored within transaction: {}.", event);
        return getDelayedFiring().delay(() -> parent.fire(event));
    }

    @Override
    public void commit() {
        LOGGER.debug("Replaying transaction.");
        while (!stateUpdates.isEmpty()) {
            stateUpdates.poll().run();
        }
        getDelayedFiring().run();
        LOGGER.debug("Done.");
    }

    @Override
    public void abort() {
        stateUpdates.clear();
        getDelayedFiring().cancel();
        delayedFiring.clear();
    }

    @Override
    public <T> T call(final Function<Zonky, T> operation, final OAuthScope scope) {
        return parent.call(operation, scope);
    }

    @Override
    public boolean isAvailable(final OAuthScope scope) {
        return parent.isAvailable(scope);
    }

    @Override
    public RemotePortfolio getPortfolio() {
        return parent.getPortfolio();
    }

    @Override
    public Restrictions getRestrictions() {
        return parent.getRestrictions();
    }

    @Override
    public SessionInfo getSessionInfo() {
        return parent.getSessionInfo();
    }

    @Override
    public Optional<InvestmentStrategy> getInvestmentStrategy() {
        return parent.getInvestmentStrategy();
    }

    @Override
    public Optional<SellStrategy> getSellStrategy() {
        return parent.getSellStrategy();
    }

    @Override
    public Optional<PurchaseStrategy> getPurchaseStrategy() {
        return parent.getPurchaseStrategy();
    }

    @Override
    public Optional<ReservationStrategy> getReservationStrategy() {
        return parent.getReservationStrategy();
    }

    @Override
    public Loan getLoan(final int loanId) {
        return parent.getLoan(loanId);
    }

    @Override
    public <T> InstanceState<T> getState(final Class<T> clz) {
        LOGGER.trace("Creating transactional instance state for {}.", clz);
        return new TransactionalInstanceState<>(stateUpdates, parent.getState(clz));
    }

    @Override
    public void close() {
        if (!stateUpdates.isEmpty()) {
            throw new IllegalStateException("There are uncommitted changes.");
        }
        if (getDelayedFiring().isPending()) {
            throw new IllegalStateException("There are uncommitted events.");
        }
    }

    @Override
    public String toString() {
        return "TransactionalPowerTenantImpl{" +
                "parent=" + parent +
                '}';
    }
}
