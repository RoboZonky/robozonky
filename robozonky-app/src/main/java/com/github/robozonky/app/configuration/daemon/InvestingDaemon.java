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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.github.robozonky.api.marketplaces.Marketplace;
import com.github.robozonky.api.strategies.InvestmentStrategy;
import com.github.robozonky.api.strategies.LoanDescriptor;
import com.github.robozonky.app.authentication.Authenticated;
import com.github.robozonky.app.investing.Investing;
import com.github.robozonky.app.investing.Investor;
import com.github.robozonky.app.portfolio.Portfolio;

class InvestingDaemon extends DaemonOperation {

    private static final class InitializingInvestor implements BiConsumer<Portfolio, Authenticated> {

        private final AtomicBoolean registered = new AtomicBoolean(false);
        private final BiConsumer<Portfolio, Authenticated> core;

        public InitializingInvestor(final Investor.Builder builder, final Marketplace marketplace,
                                    final Supplier<Optional<InvestmentStrategy>> strategy,
                                    final Duration maximumSleepPeriod) {
            this.core = (portfolio, auth) -> {
                if (!registered.getAndSet(true)) { // only register the listener once
                    final Investing i = new Investing(builder, strategy, auth, maximumSleepPeriod);
                    marketplace.registerListener((loans) -> {
                        final Collection<LoanDescriptor> descriptors = loans.stream()
                                .map(LoanDescriptor::new)
                                .collect(Collectors.toList());
                        i.apply(portfolio, descriptors);
                    });
                }
                marketplace.run();
            };
        }

        @Override
        public void accept(final Portfolio portfolio, final Authenticated zonky) {
            core.accept(portfolio, zonky);
        }
    }

    public InvestingDaemon(final Authenticated auth, final Investor.Builder builder, final Marketplace marketplace,
                           final Supplier<Optional<InvestmentStrategy>> strategy,
                           final PortfolioSupplier portfolio, final Duration maximumSleepPeriod,
                           final Duration refreshPeriod) {
        super(auth, portfolio,
              new InitializingInvestor(builder, marketplace, strategy, maximumSleepPeriod), refreshPeriod);
    }
}
