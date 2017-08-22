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

package com.github.robozonky.app.investing;

import java.time.temporal.TemporalAmount;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.github.robozonky.api.Refreshable;
import com.github.robozonky.api.marketplaces.Marketplace;
import com.github.robozonky.api.remote.entities.Investment;
import com.github.robozonky.api.strategies.InvestmentStrategy;
import com.github.robozonky.api.strategies.LoanDescriptor;
import com.github.robozonky.app.authentication.Authenticated;
import com.github.robozonky.app.portfolio.Portfolio;
import com.github.robozonky.app.util.DaemonRuntimeExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Investing implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(Investing.class);

    private final Marketplace marketplace;

    public Investing(final Authenticated auth, final Investor.Builder builder, final Marketplace marketplace,
                     final Refreshable<InvestmentStrategy> strategy, final TemporalAmount maximumSleepPeriod) {
        this.marketplace = marketplace;
        final Function<Collection<LoanDescriptor>, Collection<Investment>> investor =
                new StrategyExecution(builder, strategy, auth, maximumSleepPeriod);
        marketplace.registerListener((loans) -> {
            if (loans == null) {
                investor.apply(Collections.emptyList());
            } else {
                final Collection<LoanDescriptor> descriptors = loans.stream()
                        .map(LoanDescriptor::new)
                        .collect(Collectors.toList());
                investor.apply(descriptors);
            }
        });
    }

    @Override
    public void run() {
        try {
            if (!Portfolio.INSTANCE.isUpdating()) {
                LOGGER.trace("Starting.");
                marketplace.run();
                LOGGER.trace("Finished.");
            }
        } catch (final Throwable t) {
            /*
             * We catch Throwable so that we can inform users even about errors. Sudden death detection will take
             * care of errors stopping the thread.
             */
            new DaemonRuntimeExceptionHandler().handle(t);
        }
    }
}
