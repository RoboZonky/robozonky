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
import com.github.triceo.robozonky.api.remote.entities.Participation;
import com.github.triceo.robozonky.api.strategies.ParticipationDescriptor;
import com.github.triceo.robozonky.api.strategies.PurchaseStrategy;
import com.github.triceo.robozonky.app.authentication.Authenticated;
import com.github.triceo.robozonky.app.portfolio.Portfolio;
import com.github.triceo.robozonky.common.remote.Zonky;
import com.github.triceo.robozonky.util.TextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class StrategyExecution implements Function<Collection<Participation>, Collection<Investment>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StrategyExecution.class);

    private final Authenticated authenticated;
    private final Refreshable<PurchaseStrategy> refreshableStrategy;
    private final TemporalAmount maximumSleepPeriod;
    private final boolean dryRun;

    public StrategyExecution(final Refreshable<PurchaseStrategy> strategy, final Authenticated auth,
                             final TemporalAmount maximumSleepPeriod, final boolean dryRun) {
        this.authenticated = auth;
        this.refreshableStrategy = strategy;
        this.maximumSleepPeriod = maximumSleepPeriod;
        this.dryRun = dryRun;
    }

    private Collection<Investment> invest(final PurchaseStrategy strategy,
                                          final Collection<Participation> marketplace) {
        final Function<Zonky, Collection<Investment>> op = (zonky) -> {
            final Collection<ParticipationDescriptor> participations = marketplace.parallelStream()
                    .map(p -> new ParticipationDescriptor(p, Portfolio.INSTANCE.getLoan(zonky, p.getLoanId())))
                    .collect(Collectors.toList());
            final InvestmentCommand c = new InvestmentCommand(strategy);
            return Session.purchase(zonky, participations, c, dryRun);
        };
        return authenticated.call(op);
    }

    @Override
    public Collection<Investment> apply(final Collection<Participation> items) {
        return refreshableStrategy.getLatest()
                .map(strategy -> {
                    final Activity activity = new Activity(items, maximumSleepPeriod);
                    if (activity.shouldSleep()) {
                        StrategyExecution.LOGGER.debug("Purchasing is asleep as there is nothing going on.");
                        return Collections.<Investment>emptyList();
                    }
                    StrategyExecution.LOGGER.debug("Sending following participations to purchasing: {}.",
                                                   TextUtil.toString(items, p -> String.valueOf(p.getId())));
                    final Collection<Investment> investments = invest(strategy, items);
                    activity.settle();
                    return investments;
                }).orElseGet(() -> {
                    StrategyExecution.LOGGER.info("Purchasing is asleep as there is no strategy.");
                    return Collections.emptyList();
                });
    }
}
