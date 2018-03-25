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

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.github.robozonky.api.remote.entities.Participation;
import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.strategies.ParticipationDescriptor;
import com.github.robozonky.api.strategies.PurchaseStrategy;
import com.github.robozonky.app.authentication.Authenticated;
import com.github.robozonky.app.portfolio.Portfolio;
import com.github.robozonky.app.util.LoanCache;
import com.github.robozonky.app.util.StrategyExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Purchasing extends StrategyExecutor<Participation, PurchaseStrategy> {

    private static final Logger LOGGER = LoggerFactory.getLogger(Purchasing.class);

    private final Authenticated auth;
    private final boolean dryRun;
    private final AtomicReference<int[]> lastChecked = new AtomicReference<>(new int[0]);

    public Purchasing(final Supplier<Optional<PurchaseStrategy>> strategy, final Authenticated auth,
                      final boolean dryRun) {
        super(strategy);
        this.auth = auth;
        this.dryRun = dryRun;
    }

    private static ParticipationDescriptor toDescriptor(final Participation p, final Authenticated auth) {
        return new ParticipationDescriptor(p, auth.call(zonky -> LoanCache.INSTANCE.getLoan(p.getLoanId(), zonky)));
    }

    @Override
    protected boolean hasMarketplaceUpdates(final Collection<Participation> marketplace) {
        final int[] idsFromMarketplace = marketplace.stream().mapToInt(Participation::getId).toArray();
        final int[] presentWhenLastChecked = lastChecked.getAndSet(idsFromMarketplace);
        return StrategyExecutor.hasNewIds(presentWhenLastChecked, idsFromMarketplace);
    }

    @Override
    protected Collection<Investment> execute(final Portfolio portfolio, final PurchaseStrategy strategy,
                                             final Collection<Participation> marketplace) {
        final Stream<ParticipationDescriptor> participations = marketplace.parallelStream()
                .map(p -> toDescriptor(p, auth))
                .filter(d -> { // never re-purchase what was once sold
                    final Loan l = d.related();
                    final boolean wasSoldBefore = portfolio.wasOnceSold(l);
                    if (wasSoldBefore) {
                        LOGGER.debug("Ignoring loan #{} as the user had already sold it before.", l.getId());
                    }
                    return !wasSoldBefore;
                });
        final RestrictedPurchaseStrategy s = new RestrictedPurchaseStrategy(strategy, auth.getRestrictions());
        return Session.purchase(portfolio, auth, participations, s, dryRun);
    }
}
