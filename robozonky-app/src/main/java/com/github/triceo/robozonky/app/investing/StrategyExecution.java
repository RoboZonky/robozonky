/*
 * Copyright 2017 Lukáš Petrovický
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

package com.github.triceo.robozonky.app.investing;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.temporal.TemporalAmount;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.github.triceo.robozonky.api.Refreshable;
import com.github.triceo.robozonky.api.notifications.ExecutionCompletedEvent;
import com.github.triceo.robozonky.api.notifications.ExecutionStartedEvent;
import com.github.triceo.robozonky.api.notifications.LoanArrivedEvent;
import com.github.triceo.robozonky.api.remote.entities.Investment;
import com.github.triceo.robozonky.api.strategies.InvestmentStrategy;
import com.github.triceo.robozonky.api.strategies.LoanDescriptor;
import com.github.triceo.robozonky.app.Events;
import com.github.triceo.robozonky.app.authentication.AuthenticationHandler;
import com.github.triceo.robozonky.common.remote.ApiProvider;
import com.github.triceo.robozonky.internal.api.Defaults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class StrategyExecution implements Function<Collection<LoanDescriptor>, Collection<Investment>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StrategyExecution.class);

    private static final class StrategyBasedInvestmentCommand implements InvestmentCommand {

        private final InvestmentStrategy strategy;
        private final Collection<LoanDescriptor> loans;

        public StrategyBasedInvestmentCommand(final InvestmentStrategy strategy,
                                              final Collection<LoanDescriptor> loans) {
            this.strategy = strategy;
            this.loans = loans;
        }

        @Override
        public Collection<LoanDescriptor> getLoans() {
            return loans;
        }

        @Override
        public Collection<Investment> apply(final Investor investor) {
            return investor.invest(strategy, loans);
        }

    }

    static BigDecimal getAvailableBalance(final ZonkyProxy api) {
        final int balance = Defaults.getDefaultDryRunBalance();
        return (api.isDryRun() && balance > -1) ?
                BigDecimal.valueOf(balance) :
                api.execute(zonky -> zonky.getWallet().getAvailableBalance());
    }

    static Collection<Investment> invest(final ZonkyProxy proxy, final InvestmentCommand command) {
        final BigDecimal balance = StrategyExecution.getAvailableBalance(proxy);
        Events.fire(new ExecutionStartedEvent(proxy.getUsername(), command.getLoans(), balance.intValue()));
        final Investor investor = new Investor(proxy, balance);
        final Collection<Investment> result = command.apply(investor);
        Events.fire(new ExecutionCompletedEvent(proxy.getUsername(), result, investor.getBalance().intValue()));
        return Collections.unmodifiableCollection(result);
    }

    private final ApiProvider apiProvider;
    private final AuthenticationHandler authenticationHandler;
    private final ZonkyProxy.Builder proxyBuilder;
    private final Refreshable<InvestmentStrategy> refreshableStrategy;
    private final TemporalAmount maximumSleepPeriod;

    public StrategyExecution(final ApiProvider apiProvider, final ZonkyProxy.Builder proxyBuilder,
                             final Refreshable<InvestmentStrategy> strategy, final AuthenticationHandler auth,
                             final TemporalAmount maximumSleepPeriod) {
        this.apiProvider = apiProvider;
        this.authenticationHandler = auth;
        this.proxyBuilder = proxyBuilder;
        this.refreshableStrategy = strategy;
        this.maximumSleepPeriod = maximumSleepPeriod;
    }

    public StrategyExecution(final ApiProvider apiProvider, final ZonkyProxy.Builder proxyBuilder,
                             final Refreshable<InvestmentStrategy> strategy, final AuthenticationHandler auth) {
        this(apiProvider, proxyBuilder, strategy, auth, Duration.ofMinutes(60));
    }

    Collection<Investment> invest(final InvestmentStrategy strategy, final Collection<LoanDescriptor> loans) {
        return authenticationHandler.execute(apiProvider, api -> {
            final InvestmentCommand c = new StrategyExecution.StrategyBasedInvestmentCommand(strategy, loans);
            return StrategyExecution.invest(proxyBuilder.build(api), c);
        });
    }

    @Override
    public Collection<Investment> apply(final Collection<LoanDescriptor> loans) {
        if (loans.isEmpty()) {
            return Collections.emptyList();
        }
        final InvestmentStrategy strategy = refreshableStrategy.getLatestBlocking();
        // only after we've acquired the strategy work with the marketplace
        final Activity activity = new Activity(loans, maximumSleepPeriod);
        final boolean shouldSleep = activity.shouldSleep();
        if (shouldSleep) {
            StrategyExecution.LOGGER.info("RoboZonky is asleep as there is nothing going on.");
            return Collections.emptyList();
        } else {
            StrategyExecution.LOGGER.debug("Sending following loans to the investor: {}.", loans.stream()
                    .peek(l -> Events.fire(new LoanArrivedEvent(l)))
                    .map(l -> String.valueOf(l.getLoan().getId()))
                    .collect(Collectors.joining(", ")));
            final Collection<Investment> investments = invest(strategy, loans);
            activity.settle();
            return investments;
        }
    }
}
