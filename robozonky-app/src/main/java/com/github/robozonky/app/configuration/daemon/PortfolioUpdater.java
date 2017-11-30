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
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import com.github.robozonky.app.authentication.Authenticated;
import com.github.robozonky.app.portfolio.Portfolio;
import com.github.robozonky.app.portfolio.PortfolioDependant;
import com.github.robozonky.app.util.DaemonRuntimeExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PortfolioUpdater implements Runnable,
                                         Supplier<Optional<Portfolio>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PortfolioUpdater.class);
    private final Authenticated authenticated;
    private final AtomicReference<Portfolio> portfolio = new AtomicReference<>();
    private final Set<PortfolioDependant> dependants = new CopyOnWriteArraySet<>();

    public PortfolioUpdater(final Authenticated authenticated) {
        this.authenticated = authenticated;
    }

    public void registerDependant(final PortfolioDependant updater) {
        LOGGER.debug("Registering dependant: {}.", updater);
        dependants.add(updater);
    }

    @Override
    public void run() {
        LOGGER.info("Pausing RoboZonky in order to update internal data structures.");
        final Portfolio folio = portfolio.updateAndGet(p -> authenticated.call(Portfolio::create).orElse(null));
        if (folio == null) {
            return;
        }
        // execute every dependant with its own authentication, to prevent token timeouts
        dependants.forEach(d -> {
            LOGGER.trace("Running dependant: {}.", d);
            try {
                d.accept(folio, authenticated);
            } catch (final Throwable t) {
                new DaemonRuntimeExceptionHandler().handle(t);
            }
        });
        LOGGER.info("RoboZonky resumed.");
    }

    @Override
    public Optional<Portfolio> get() {
        return Optional.ofNullable(portfolio.get());
    }
}
