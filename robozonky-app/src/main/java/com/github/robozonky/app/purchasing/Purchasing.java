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

package com.github.robozonky.app.purchasing;

import java.time.temporal.TemporalAmount;
import java.util.Collection;
import java.util.stream.Collectors;

import com.github.robozonky.api.Refreshable;
import com.github.robozonky.api.remote.entities.Investment;
import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.api.remote.entities.Participation;
import com.github.robozonky.api.strategies.ParticipationDescriptor;
import com.github.robozonky.api.strategies.PurchaseStrategy;
import com.github.robozonky.app.portfolio.Portfolio;
import com.github.robozonky.app.util.StrategyExecutor;
import com.github.robozonky.common.remote.Zonky;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Purchasing extends StrategyExecutor<Participation, PurchaseStrategy> {

    private static final Logger LOGGER = LoggerFactory.getLogger(Purchasing.class);

    private final Zonky zonky;
    private final boolean dryRun;

    public Purchasing(final Refreshable<PurchaseStrategy> strategy, final Zonky zonky,
                      final TemporalAmount maximumSleepPeriod, final boolean dryRun) {
        super((l) -> new Activity(l, maximumSleepPeriod), strategy);
        this.zonky = zonky;
        this.dryRun = dryRun;
    }

    private static ParticipationDescriptor toDescriptor(final Participation p, final Zonky zonky) {
        final Loan l = Portfolio.INSTANCE.getLoan(zonky, p.getLoanId());
        return new ParticipationDescriptor(p, l);
    }

    @Override
    protected int identify(final Participation item) {
        return item.getId();
    }

    @Override
    protected Collection<Investment> execute(final PurchaseStrategy strategy,
                                             final Collection<Participation> marketplace) {
        final Collection<ParticipationDescriptor> participations = marketplace.parallelStream()
                .map(p -> toDescriptor(p, zonky))
                .filter(d -> { // never re-purchase what was once sold
                    final Loan l = d.related();
                    final boolean wasSoldBefore = Portfolio.INSTANCE.wasOnceSold(l);
                    if (wasSoldBefore) {
                        LOGGER.debug("Ignoring loan #{} as the user had already sold it before.", l.getId());
                    }
                    return !wasSoldBefore;
                })
                .collect(Collectors.toList());
        return Session.purchase(zonky, participations, strategy, dryRun);
    }
}
