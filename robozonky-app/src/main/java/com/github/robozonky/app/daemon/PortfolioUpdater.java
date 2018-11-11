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

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import com.github.robozonky.api.strategies.SellStrategy;
import com.github.robozonky.app.daemon.operations.Selling;
import com.github.robozonky.app.daemon.transactions.IncomeProcessor;
import com.github.robozonky.common.Tenant;
import com.github.robozonky.util.Backoff;
import com.github.robozonky.util.IoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class PortfolioUpdater implements Runnable,
                                  PortfolioSupplier {

    private static final Logger LOGGER = LoggerFactory.getLogger(PortfolioUpdater.class);
    private final Tenant tenant;
    private final AtomicReference<Portfolio> current = new AtomicReference<>();
    private final Collection<PortfolioDependant> dependants = new CopyOnWriteArrayList<>();
    private final Consumer<Throwable> shutdownCall;
    private final Supplier<BlockedAmountProcessor> blockedAmounts;
    private final Duration retryFor;

    PortfolioUpdater(final Consumer<Throwable> shutdownCall, final Tenant tenant,
                     final Supplier<BlockedAmountProcessor> blockedAmounts, final Duration retryFor) {
        this.shutdownCall = shutdownCall;
        this.tenant = tenant;
        this.blockedAmounts = blockedAmounts;
        this.retryFor = retryFor;
    }

    PortfolioUpdater(final Consumer<Throwable> shutdownCall, final Tenant tenant,
                     final Supplier<BlockedAmountProcessor> blockedAmounts) {
        this(shutdownCall, tenant, blockedAmounts, Duration.ofHours(1));
    }

    PortfolioUpdater(final Tenant tenant, final Supplier<BlockedAmountProcessor> blockedAmounts) {
        this(t -> {
        }, tenant, blockedAmounts, Duration.ofHours(1));
    }

    public static PortfolioUpdater create(final Consumer<Throwable> shutdownCall, final Tenant auth,
                                          final Supplier<Optional<SellStrategy>> sp) {
        final Supplier<BlockedAmountProcessor> blockedAmounts = BlockedAmountProcessor.createLazy(auth);
        final PortfolioUpdater updater = new PortfolioUpdater(shutdownCall, auth, blockedAmounts);
        // update delinquents automatically with every portfolio update; important to be first as it brings risk data
        updater.registerDependant(new AmountsAtRiskSummarizer());
        // attempt to sell participations; a transaction update later may already pick up some sales
        updater.registerDependant(new Selling(sp));
        // update currentlyUsedPortfolio with blocked amounts coming from Zonky
        updater.registerDependant(po -> blockedAmounts.get().accept(po));
        // send notifications based on new transactions coming from Zonky
        updater.registerDependant(new IncomeProcessor());
        return updater;
    }

    void registerDependant(final PortfolioDependant updater) {
        if (!dependants.contains(updater)) {
            LOGGER.debug("Registering dependant: {}.", updater);
            dependants.add(updater);
        }
    }

    Collection<PortfolioDependant> getRegisteredDependants() {
        return Collections.unmodifiableCollection(dependants);
    }

    public boolean isInitializing() {
        return !get().isPresent();
    }

    private Portfolio runIt(final Portfolio old) {
        final Portfolio result = old == null ? Portfolio.create(tenant, blockedAmounts) : old;
        final TransactionalPortfolio transactional = new TransactionalPortfolio(result, tenant);
        final CompletableFuture<TransactionalPortfolio> combined = dependants.stream()
                .map(d -> (Function<TransactionalPortfolio, TransactionalPortfolio>) folio -> {
                    LOGGER.trace("Running {}.", d);
                    d.accept(folio);
                    LOGGER.trace("Finished {}.", d);
                    return folio;
                })
                .reduce(CompletableFuture.completedFuture(transactional),
                        CompletableFuture::thenApply,
                        (s1, s2) -> s1.thenCombine(s2, (p1, p2) -> p2));
        try {
            final TransactionalPortfolio p = combined.get();
            p.run(); // persist stored information
            return p.getPortfolio();
        } catch (final Exception ex) {
            throw new IllegalStateException("Portfolio update failed.", ex);
        }
    }

    private void terminate(final Exception cause) {
        current.set(null);
        shutdownCall.accept(new IllegalStateException("Portfolio initialization failed.", cause));
    }

    @Override
    public void run() {
        LOGGER.info("Updating internal data structures. May take a long time for large portfolios.");
        try { // close the old portfolio
            IoUtil.tryConsumer(current::get, old -> {
                final Backoff<Portfolio> backoff = Backoff.exponential(() -> runIt(old), Duration.ofSeconds(1),
                                                                       retryFor);
                final Optional<Portfolio> maybeNew = backoff.get();
                if (maybeNew.isPresent()) {
                    current.set(maybeNew.get());
                    LOGGER.info("Update finished.");
                } else {
                    terminate(backoff.getLastException().orElse(null));
                }
            });
        } catch (final Exception ex) {
            terminate(ex);
        }
    }

    @Override
    public Optional<Portfolio> get() {
        return Optional.ofNullable(current.get());
    }
}
