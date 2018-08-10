/*
 * Copyright 2018 The RoboZonky Project
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.github.robozonky.api.remote.entities.Participation;
import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.strategies.ParticipationDescriptor;
import com.github.robozonky.api.strategies.PurchaseStrategy;
import com.github.robozonky.app.authentication.Tenant;
import com.github.robozonky.app.portfolio.Portfolio;
import com.github.robozonky.app.util.LoanCache;
import com.github.robozonky.app.util.SoldParticipationCache;
import com.github.robozonky.app.util.StrategyExecutor;
import com.github.robozonky.util.NumberUtil;

public class Purchasing extends StrategyExecutor<Participation, PurchaseStrategy> {

    private final Tenant auth;
    private final SoldParticipationCache soldParticipationCache;
    private final AtomicReference<int[]> lastChecked = new AtomicReference<>(new int[0]);

    public Purchasing(final Supplier<Optional<PurchaseStrategy>> strategy, final Tenant auth) {
        super(strategy);
        this.auth = auth;
        this.soldParticipationCache = SoldParticipationCache.forTenant(auth);
    }

    private static ParticipationDescriptor toDescriptor(final Participation p, final Tenant auth) {
        return new ParticipationDescriptor(p, LoanCache.INSTANCE.getLoan(p.getLoanId(), auth));
    }

    @Override
    protected boolean isBalanceUnderMinimum(final int current) { // there is no minimum in purchasing
        return false;
    }

    @Override
    protected boolean hasMarketplaceUpdates(final Collection<Participation> marketplace) {
        final int[] idsFromMarketplace = marketplace.stream().mapToInt(Participation::getId).toArray();
        final int[] presentWhenLastChecked = lastChecked.getAndSet(idsFromMarketplace);
        return NumberUtil.hasAdditions(presentWhenLastChecked, idsFromMarketplace);
    }

    @Override
    protected Collection<Investment> execute(final Portfolio portfolio, final PurchaseStrategy strategy,
                                             final Collection<Participation> marketplace) {
        final Collection<ParticipationDescriptor> participations = marketplace.parallelStream()
                .map(p -> toDescriptor(p, auth))
                .filter(d -> { // never re-purchase what was once sold
                    final int loanId = d.item().getLoanId();
                    final boolean wasSoldBefore = soldParticipationCache.wasOnceSold(loanId);
                    if (wasSoldBefore) {
                        LOGGER.debug("Ignoring loan #{} as the user had already sold it before.", loanId);
                    }
                    return !wasSoldBefore;
                }).collect(Collectors.toCollection(ArrayList::new));
        final RestrictedPurchaseStrategy s = new RestrictedPurchaseStrategy(strategy, auth.getRestrictions());
        return Session.purchase(portfolio, auth, participations, s);
    }
}
