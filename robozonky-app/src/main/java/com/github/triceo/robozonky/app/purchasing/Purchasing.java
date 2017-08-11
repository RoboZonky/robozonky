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
import java.util.function.Function;
import java.util.stream.Collectors;

import com.github.triceo.robozonky.api.Refreshable;
import com.github.triceo.robozonky.api.remote.entities.Investment;
import com.github.triceo.robozonky.api.remote.entities.Loan;
import com.github.triceo.robozonky.api.remote.entities.Participation;
import com.github.triceo.robozonky.api.strategies.ParticipationDescriptor;
import com.github.triceo.robozonky.api.strategies.PurchaseStrategy;
import com.github.triceo.robozonky.app.authentication.Authenticated;
import com.github.triceo.robozonky.app.portfolio.Portfolio;
import com.github.triceo.robozonky.app.util.DaemonRuntimeExceptionHandler;
import com.github.triceo.robozonky.common.remote.Zonky;

public class Purchasing implements Runnable {

    private final Authenticated authenticated;
    private final boolean isDryRun;
    private final Refreshable<PurchaseStrategy> refreshableStrategy;
    private final TemporalAmount maximumSleepPeriod;

    public Purchasing(final Authenticated auth, final boolean isDryRun, final Refreshable<PurchaseStrategy> strategy,
                      final TemporalAmount maximumSleepPeriod) {
        this.authenticated = auth;
        this.isDryRun = isDryRun;
        this.refreshableStrategy = strategy;
        this.maximumSleepPeriod = maximumSleepPeriod;
    }

    private Loan getLoan(final int loanId, final Zonky zonky) {
        return Portfolio.INSTANCE.getLoan(zonky, loanId);
    }

    private ParticipationDescriptor getDescriptor(final Participation p, final Zonky zonky) {
        return new ParticipationDescriptor(p, getLoan(p.getLoanId(), zonky));
    }

    @Override
    public void run() {
        try { // FIXME perhaps streaming would be more resource-efficient?
            authenticated.run(z -> getInvestor().apply(z.getAvailableParticipations()
                                                               .map(p -> getDescriptor(p, z))
                                                               .collect(Collectors.toList())));
        } catch (final Throwable t) {
            /*
             * We catch Throwable so that we can inform users even about errors. Sudden death detection will take
             * care of errors stopping the thread.
             */
            new DaemonRuntimeExceptionHandler().handle(t);
        }
    }

    private Function<Collection<ParticipationDescriptor>, Collection<Investment>> getInvestor() {
        return new StrategyExecution(refreshableStrategy, authenticated, maximumSleepPeriod, isDryRun);
    }
}
