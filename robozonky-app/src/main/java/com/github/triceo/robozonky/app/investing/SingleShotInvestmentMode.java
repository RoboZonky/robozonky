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
import java.util.Optional;
import java.util.concurrent.Semaphore;
import java.util.function.Consumer;
import java.util.function.Function;

import com.github.triceo.robozonky.api.Refreshable;
import com.github.triceo.robozonky.api.marketplaces.ExpectedTreatment;
import com.github.triceo.robozonky.api.marketplaces.Marketplace;
import com.github.triceo.robozonky.api.remote.entities.Investment;
import com.github.triceo.robozonky.api.remote.entities.Loan;
import com.github.triceo.robozonky.api.strategies.InvestmentStrategy;
import com.github.triceo.robozonky.api.strategies.LoanDescriptor;
import com.github.triceo.robozonky.app.authentication.AuthenticationHandler;
import com.github.triceo.robozonky.common.remote.Apis;

public class SingleShotInvestmentMode extends AbstractInvestmentMode {

    private final Refreshable<InvestmentStrategy> refreshableStrategy;
    private final Marketplace marketplace;
    private final TemporalAmount maximumSleepPeriod;

    public SingleShotInvestmentMode(final AuthenticationHandler auth, final ZonkyProxy.Builder builder,
                                    final boolean isFaultTolerant, final Marketplace marketplace,
                                    final Refreshable<InvestmentStrategy> strategy,
                                    final TemporalAmount maximumSleepPeriod) {
        super(auth, builder, isFaultTolerant);
        if (marketplace.specifyExpectedTreatment() != ExpectedTreatment.POLLING) {
            throw new IllegalArgumentException("Polling marketplace implementation required.");
        }
        this.refreshableStrategy = strategy;
        this.marketplace = marketplace;
        this.maximumSleepPeriod = maximumSleepPeriod;
    }

    public SingleShotInvestmentMode(final AuthenticationHandler auth, final ZonkyProxy.Builder builder,
                                    final boolean isFaultTolerant, final Marketplace marketplace,
                                    final Refreshable<InvestmentStrategy> strategy) {
        this(auth, builder, isFaultTolerant, marketplace, strategy, Duration.ofMinutes(60));
    }

    @Override
    protected Optional<Collection<Investment>> execute(final Apis apis) {
        return execute(apis, new Semaphore(1));
    }

    @Override
    protected void openMarketplace(final Consumer<Collection<Loan>> target) {
        marketplace.registerListener(target);
        marketplace.run();
    }

    @Override
    protected Function<Collection<LoanDescriptor>, Collection<Investment>> getInvestor(final Apis apis) {
        return new StrategyExecution(apis, getProxyBuilder(), refreshableStrategy, getAuthenticationHandler(),
                maximumSleepPeriod);
    }

    @Override
    public void close() throws Exception {
        this.marketplace.close();
    }
}
