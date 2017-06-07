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

import java.time.Duration;
import java.time.temporal.TemporalAmount;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.github.triceo.robozonky.api.Refreshable;
import com.github.triceo.robozonky.api.notifications.LoanArrivedEvent;
import com.github.triceo.robozonky.api.remote.entities.Investment;
import com.github.triceo.robozonky.api.strategies.InvestmentStrategy;
import com.github.triceo.robozonky.api.strategies.LoanDescriptor;
import com.github.triceo.robozonky.app.Events;
import com.github.triceo.robozonky.app.authentication.AuthenticationHandler;
import com.github.triceo.robozonky.common.remote.ApiProvider;
import com.github.triceo.robozonky.common.remote.AuthenticatedZonky;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class StrategyExecution implements Function<Collection<LoanDescriptor>, Collection<Investment>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StrategyExecution.class);

    private final ApiProvider apiProvider;
    private final AuthenticationHandler authenticationHandler;
    private final Investor.Builder proxyBuilder;
    private final Refreshable<InvestmentStrategy> refreshableStrategy;
    private final TemporalAmount maximumSleepPeriod;

    public StrategyExecution(final ApiProvider apiProvider, final Investor.Builder proxyBuilder,
                             final Refreshable<InvestmentStrategy> strategy, final AuthenticationHandler auth,
                             final TemporalAmount maximumSleepPeriod) {
        this.apiProvider = apiProvider;
        this.authenticationHandler = auth;
        this.proxyBuilder = proxyBuilder;
        this.refreshableStrategy = strategy;
        this.maximumSleepPeriod = maximumSleepPeriod;
    }

    public StrategyExecution(final ApiProvider apiProvider, final Investor.Builder proxyBuilder,
                             final Refreshable<InvestmentStrategy> strategy, final AuthenticationHandler auth) {
        this(apiProvider, proxyBuilder, strategy, auth, Duration.ofMinutes(60));
    }

    private Collection<Investment> invest(final InvestmentStrategy strategy,
                                          final Collection<LoanDescriptor> marketplace) {
        final Function<AuthenticatedZonky, Collection<Investment>> op = (zonky) -> {
            final Investor proxy = proxyBuilder.build(zonky);
            final InvestmentCommand c = new StrategyBasedInvestmentCommand(strategy, marketplace);
            return Session.invest(proxy, zonky, c);
        };
        return authenticationHandler.execute(apiProvider, op);
    }

    private Collection<Investment> justReauth() {
        return authenticationHandler.execute(apiProvider, null);
    }

    @Override
    public Collection<Investment> apply(final Collection<LoanDescriptor> loans) {
        if (loans.isEmpty()) {
            return Collections.emptyList();
        }
        return refreshableStrategy.getLatest()
                .map(strategy -> {
                    final Activity activity = new Activity(loans, maximumSleepPeriod);
                    final boolean shouldSleep = activity.shouldSleep();
                    if (shouldSleep) {
                        final Collection<Investment> empty = justReauth();
                        StrategyExecution.LOGGER.info("RoboZonky is asleep as there is nothing going on.");
                        return empty;
                    } else {
                        StrategyExecution.LOGGER.debug("Sending following marketplace to the investor: {}.", loans.stream()
                                .peek(l -> Events.fire(new LoanArrivedEvent(l)))
                                .map(l -> String.valueOf(l.getLoan().getId()))
                                .collect(Collectors.joining(", ")));
                        final Collection<Investment> investments = invest(strategy, loans);
                        activity.settle();
                        return investments;
                    }
                }).orElseGet(() -> {
                    StrategyExecution.LOGGER.info("RoboZonky is asleep as there is no investment strategy.");
                    return Collections.emptyList();
                });
    }
}
