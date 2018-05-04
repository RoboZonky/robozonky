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

package com.github.robozonky.app.configuration.daemon;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import com.github.robozonky.api.strategies.SellStrategy;
import com.github.robozonky.app.authentication.Tenant;
import com.github.robozonky.app.portfolio.Delinquents;
import com.github.robozonky.app.portfolio.Portfolio;
import com.github.robozonky.app.portfolio.PortfolioDependant;
import com.github.robozonky.app.portfolio.RemoteBalance;
import com.github.robozonky.app.portfolio.Repayments;
import com.github.robozonky.app.portfolio.Selling;
import com.github.robozonky.util.Backoff;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class PortfolioUpdater implements Runnable,
                                  PortfolioSupplier {

    private static final Logger LOGGER = LoggerFactory.getLogger(PortfolioUpdater.class);
    private final Tenant tenant;
    private final AtomicReference<Portfolio> portfolio = new AtomicReference<>();
    private final Collection<PortfolioDependant> dependants = new CopyOnWriteArrayList<>();
    private final AtomicBoolean updating = new AtomicBoolean(true);
    private final Consumer<Throwable> shutdownCall;
    private final Duration retryFor;
    private final RemoteBalance balance;

    PortfolioUpdater(final Consumer<Throwable> shutdownCall, final Tenant tenant,
                     final RemoteBalance balance,
                     final Duration retryFor) {
        this.shutdownCall = shutdownCall;
        this.tenant = tenant;
        this.balance = balance;
        this.retryFor = retryFor;
        // run update of transactions automatically with every portfolio update
        registerDependant(Portfolio::updateTransactions);
    }

    PortfolioUpdater(final Consumer<Throwable> shutdownCall, final Tenant tenant,
                     final RemoteBalance balance) {
        this(shutdownCall, tenant, balance, Duration.ofHours(1));
    }

    public static PortfolioUpdater create(final Consumer<Throwable> shutdownCall, final Tenant auth,
                                          final Supplier<Optional<SellStrategy>> sp, final boolean isDryRun) {
        final RemoteBalance balance = RemoteBalance.create(auth, isDryRun);
        final PortfolioUpdater updater = new PortfolioUpdater(shutdownCall, auth, balance);
        // update loans repaid with every portfolio update
        updater.registerDependant(new Repayments());
        // update delinquents automatically with every portfolio update
        updater.registerDependant((p, a) -> Delinquents.update(a, p));
        // attempt to sell participations after every portfolio update
        updater.registerDependant(new Selling(sp, isDryRun));
        return updater;
    }

    public void registerDependant(final PortfolioDependant updater) {
        if (!dependants.contains(updater)) {
            LOGGER.debug("Registering dependant: {}.", updater);
            dependants.add(updater);
        }
    }

    Collection<PortfolioDependant> getRegisteredDependants() {
        return Collections.unmodifiableCollection(dependants);
    }

    public boolean isUpdating() {
        return updating.get();
    }

    private Portfolio runIt(final Portfolio old) {
        final Portfolio result = old == null ? Portfolio.create(tenant, balance) : old.reloadFromZonky(tenant, balance);
        final CompletableFuture<Portfolio> combined = dependants.stream()
                .map(d -> (Function<Portfolio, Portfolio>) folio -> {
                    LOGGER.trace("Running {}.", d);
                    d.accept(folio, tenant);
                    LOGGER.trace("Finished {}.", d);
                    return folio;
                })
                .reduce(CompletableFuture.completedFuture(result),
                        CompletableFuture::thenApply,
                        (s1, s2) -> s1.thenCombine(s2, (p1, p2) -> p2));
        try {
            return combined.get();
        } catch (final Throwable t) {
            throw new IllegalStateException("Portfolio update failed.", t);
        }
    }

    @Override
    public void run() {
        LOGGER.info("Pausing RoboZonky in order to update internal data structures.");
        updating.set(true);
        final Backoff<Portfolio> backoff = Backoff.exponential(() -> runIt(portfolio.get()), Duration.ofSeconds(1),
                                                               retryFor);
        final Optional<Portfolio> p = backoff.get();
        if (p.isPresent()) {
            portfolio.set(p.get());
            updating.set(false);
            LOGGER.info("RoboZonky resumed.");
        } else {
            shutdownCall.accept(new IllegalStateException("Portfolio initialization failed."));
        }
    }

    @Override
    public Optional<Portfolio> get() {
        return Optional.ofNullable(portfolio.get());
    }
}
