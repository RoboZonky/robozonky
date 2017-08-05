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

package com.github.triceo.robozonky.app.purchasing;

import java.time.temporal.TemporalAmount;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.github.triceo.robozonky.api.Refreshable;
import com.github.triceo.robozonky.api.remote.entities.Investment;
import com.github.triceo.robozonky.api.strategies.ParticipationDescriptor;
import com.github.triceo.robozonky.api.strategies.PurchaseStrategy;
import com.github.triceo.robozonky.app.authentication.Authenticated;
import com.github.triceo.robozonky.common.remote.Zonky;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class StrategyExecution implements Function<Collection<ParticipationDescriptor>, Collection<Investment>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(
            StrategyExecution.class);

    private final Authenticated authenticationHandler;
    private final Refreshable<PurchaseStrategy> refreshableStrategy;
    private final TemporalAmount maximumSleepPeriod;
    private final boolean dryRun;

    public StrategyExecution(final Refreshable<PurchaseStrategy> strategy, final Authenticated auth,
                             final TemporalAmount maximumSleepPeriod, final boolean dryRun) {
        this.authenticationHandler = auth;
        this.refreshableStrategy = strategy;
        this.maximumSleepPeriod = maximumSleepPeriod;
        this.dryRun = dryRun;
    }

    private Collection<Investment> invest(final PurchaseStrategy strategy,
                                          final Collection<ParticipationDescriptor> marketplace) {
        final Function<Zonky, Collection<Investment>> op = (zonky) -> {
            final InvestmentCommand c = new InvestmentCommand(strategy, marketplace);
            return Session.invest(zonky, c, dryRun);
        };
        return authenticationHandler.call(op);
    }

    @Override
    public Collection<Investment> apply(final Collection<ParticipationDescriptor> items) {
        return refreshableStrategy.getLatest()
                .map(strategy -> {
                    final Activity activity = new Activity(items, maximumSleepPeriod);
                    if (activity.shouldSleep()) {
                        StrategyExecution.LOGGER.info("Purchasing is asleep as there is nothing going on.");
                        return Collections.<Investment>emptyList();
                    }
                    StrategyExecution.LOGGER.debug("Sending following participations to purchasing: {}.", items.stream()
                            .map(p -> String.valueOf(p.item().getId()))
                            .collect(Collectors.joining(", ")));
                    final Collection<Investment> investments = invest(strategy, items);
                    activity.settle();
                    return investments;
                }).orElseGet(() -> {
                    StrategyExecution.LOGGER.info("Purchasing is asleep as there is no investment strategy.");
                    return Collections.emptyList();
                });
    }
}
