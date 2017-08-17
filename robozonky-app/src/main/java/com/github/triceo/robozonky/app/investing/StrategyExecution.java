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

package com.github.triceo.robozonky.app.investing;

import java.time.temporal.TemporalAmount;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Function;

import com.github.triceo.robozonky.api.Refreshable;
import com.github.triceo.robozonky.api.remote.entities.Investment;
import com.github.triceo.robozonky.api.strategies.InvestmentStrategy;
import com.github.triceo.robozonky.api.strategies.LoanDescriptor;
import com.github.triceo.robozonky.app.authentication.Authenticated;
import com.github.triceo.robozonky.common.remote.Zonky;
import com.github.triceo.robozonky.util.TextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class StrategyExecution implements Function<Collection<LoanDescriptor>, Collection<Investment>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StrategyExecution.class);

    private final Authenticated authenticationHandler;
    private final Investor.Builder investor;
    private final Refreshable<InvestmentStrategy> refreshableStrategy;
    private final TemporalAmount maximumSleepPeriod;

    public StrategyExecution(final Investor.Builder investor, final Refreshable<InvestmentStrategy> strategy,
                             final Authenticated auth, final TemporalAmount maximumSleepPeriod) {
        this.authenticationHandler = auth;
        this.investor = investor;
        this.refreshableStrategy = strategy;
        this.maximumSleepPeriod = maximumSleepPeriod;
    }

    private Collection<Investment> invest(final InvestmentStrategy strategy,
                                          final Collection<LoanDescriptor> marketplace) {
        final Function<Zonky, Collection<Investment>> op = (zonky) -> {
            final InvestmentCommand c = new InvestmentCommand(strategy, marketplace);
            return Session.invest(investor, zonky, c);
        };
        return authenticationHandler.call(op);
    }

    @Override
    public Collection<Investment> apply(final Collection<LoanDescriptor> loans) {
        return refreshableStrategy.getLatest()
                .map(strategy -> {
                    final Activity activity = new Activity(loans, maximumSleepPeriod);
                    if (activity.shouldSleep()) {
                        StrategyExecution.LOGGER.debug("Investing is asleep as there is nothing going on.");
                        return Collections.<Investment>emptyList();
                    }
                    StrategyExecution.LOGGER.debug("Sending following loans to the investor: {}.",
                                                   TextUtil.toString(loans, l -> String.valueOf(l.item().getId())));
                    final Collection<Investment> investments = invest(strategy, loans);
                    activity.settle();
                    return investments;
                }).orElseGet(() -> {
                    StrategyExecution.LOGGER.info("Investing is asleep as there is no strategy.");
                    return Collections.emptyList();
                });
    }
}
