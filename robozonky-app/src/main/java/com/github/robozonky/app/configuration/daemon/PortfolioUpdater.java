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
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

import com.github.robozonky.app.authentication.Authenticated;
import com.github.robozonky.app.portfolio.Portfolio;
import com.github.robozonky.app.portfolio.PortfolioDependant;
import com.github.robozonky.util.Backoff;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PortfolioUpdater implements Runnable,
                                         PortfolioSupplier {

    private static final Logger LOGGER = LoggerFactory.getLogger(PortfolioUpdater.class);
    private final Authenticated authenticated;
    private final AtomicReference<Portfolio> portfolio = new AtomicReference<>();
    private final Collection<PortfolioDependant> dependants = new CopyOnWriteArraySet<>();
    private final AtomicBoolean updating = new AtomicBoolean(true);
    private final Consumer<Throwable> shutdownCall;
    private final Duration retryFor;

    PortfolioUpdater(final Consumer<Throwable> shutdownCall, final Authenticated authenticated,
                     final Duration retryFor) {
        this.shutdownCall = shutdownCall;
        this.authenticated = authenticated;
        this.retryFor = retryFor;
    }

    public PortfolioUpdater(final Consumer<Throwable> shutdownCall, final Authenticated authenticated) {
        this(shutdownCall, authenticated, Duration.ofHours(1));
    }

    public void registerDependant(final PortfolioDependant updater) {
        LOGGER.debug("Registering dependant: {}.", updater);
        dependants.add(updater);
    }

    public boolean isUpdating() {
        return updating.get();
    }

    private Portfolio runIt() {
        final Portfolio result = authenticated.call(Portfolio::create);
        final CompletableFuture<Portfolio> combined = dependants.stream()
                .map(d -> (Function<Portfolio, Portfolio>) folio -> {
                    LOGGER.trace("Running {}.", d);
                    d.accept(folio, authenticated);
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
        final Backoff<Portfolio> backoff = Backoff.exponential(this::runIt, Duration.ofSeconds(1), retryFor);
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
