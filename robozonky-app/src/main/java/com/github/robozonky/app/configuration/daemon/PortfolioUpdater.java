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

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import com.github.robozonky.app.authentication.Authenticated;
import com.github.robozonky.app.portfolio.Portfolio;
import com.github.robozonky.app.util.DaemonRuntimeExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PortfolioUpdater implements Runnable,
                                         PortfolioSupplier {

    private static final Logger LOGGER = LoggerFactory.getLogger(PortfolioUpdater.class);
    private final Authenticated authenticated;
    private final AtomicReference<Portfolio> portfolio = new AtomicReference<>();
    private final Set<PortfolioDependant> dependants = new CopyOnWriteArraySet<>();
    private final AtomicBoolean updating = new AtomicBoolean(false);

    public PortfolioUpdater(final Authenticated authenticated) {
        this.authenticated = authenticated;
    }

    public void registerDependant(final PortfolioDependant updater) {
        LOGGER.debug("Registering dependant: {}.", updater);
        dependants.add(updater);
    }

    public boolean isUpdating() {
        return updating.get();
    }

    @Override
    public void run() {
        LOGGER.info("Pausing RoboZonky in order to update internal data structures.");
        updating.set(true);
        try {
            final Portfolio result = authenticated.call(Portfolio::create);
            final CompletableFuture<Portfolio> combined = dependants.stream()
                    .map(d -> (Function<Portfolio, Portfolio>) folio -> {
                        d.accept(folio, authenticated);
                        return folio;
                    })
                    .reduce(CompletableFuture.completedFuture(result),
                            CompletableFuture::thenApply,
                            (s1, s2) -> s1.thenCombine(s2, (p1, p2) -> p2));
            portfolio.set(combined.get());
        } catch (final Throwable t) {
            portfolio.set(null);
            new DaemonRuntimeExceptionHandler().handle(t);
        } finally {
            updating.set(false);
        }
        LOGGER.info("RoboZonky resumed.");
    }

    @Override
    public Optional<Portfolio> get() {
        return Optional.ofNullable(portfolio.get());
    }
}
