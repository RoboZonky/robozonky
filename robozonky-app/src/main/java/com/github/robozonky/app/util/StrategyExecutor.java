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

package com.github.robozonky.app.util;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import com.github.robozonky.api.remote.entities.Investment;
import com.github.robozonky.app.configuration.daemon.MarketplaceActivity;
import com.github.robozonky.app.portfolio.Portfolio;
import com.github.robozonky.util.TextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class StrategyExecutor<T, S> implements BiFunction<Portfolio, Collection<T>, Collection<Investment>> {

    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    private final Supplier<Optional<S>> strategyProvider;
    private final Function<Collection<T>, MarketplaceActivity> activityProvider;

    protected StrategyExecutor(final Function<Collection<T>, MarketplaceActivity> activity,
                               final Supplier<Optional<S>> strategy) {
        this.activityProvider = activity;
        this.strategyProvider = strategy;
    }

    protected abstract int identify(final T item);

    protected abstract Collection<Investment> execute(final Portfolio portfolio, final S strategy,
                                                      final Collection<T> marketplace);

    private Collection<Investment> invest(final Portfolio portfolio, final S strategy,
                                          final Collection<T> marketplace) {
        final MarketplaceActivity activity = activityProvider.apply(marketplace);
        if (activity.shouldSleep()) {
            LOGGER.debug("Asleep as there is nothing going on.");
            return Collections.emptyList();
        }
        LOGGER.debug("Sent: {}.", TextUtil.toString(marketplace, l -> String.valueOf(identify(l))));
        final Collection<Investment> investments = execute(portfolio, strategy, marketplace);
        activity.settle();
        return investments;
    }

    @Override
    public Collection<Investment> apply(final Portfolio portfolio, final Collection<T> marketplace) {
        return strategyProvider.get()
                .map(strategy -> invest(portfolio, strategy, marketplace))
                .orElseGet(() -> {
                    LOGGER.info("Asleep as there is no strategy.");
                    return Collections.emptyList();
                });
    }
}
