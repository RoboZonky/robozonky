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

package com.github.robozonky.app.daemon.operations;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.github.robozonky.api.remote.entities.Participation;
import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.strategies.ParticipationDescriptor;
import com.github.robozonky.api.strategies.PurchaseStrategy;
import com.github.robozonky.app.daemon.LoanCache;
import com.github.robozonky.app.daemon.Portfolio;
import com.github.robozonky.common.Tenant;
import com.github.robozonky.util.NumberUtil;

public class Purchasing extends StrategyExecutor<Participation, PurchaseStrategy> {

    private static final long[] NO_LONGS = new long[0];
    private final Tenant auth;
    private final AtomicReference<long[]> lastChecked = new AtomicReference<>(NO_LONGS);

    public Purchasing(final Supplier<Optional<PurchaseStrategy>> strategy, final Tenant auth) {
        super(strategy);
        this.auth = auth;
    }

    private static ParticipationDescriptor toDescriptor(final Participation p, final Tenant auth) {
        return new ParticipationDescriptor(p, () -> LoanCache.get().getLoan(p.getLoanId(), auth));
    }

    @Override
    protected boolean isBalanceUnderMinimum(final int current) { // there is no minimum in purchasing
        return false;
    }

    @Override
    protected boolean hasMarketplaceUpdates(final Collection<Participation> marketplace) {
        final long[] idsFromMarketplace = marketplace.stream().mapToLong(Participation::getId).toArray();
        final long[] presentWhenLastChecked = lastChecked.getAndSet(idsFromMarketplace);
        return NumberUtil.hasAdditions(presentWhenLastChecked, idsFromMarketplace);
    }

    @Override
    protected Collection<Investment> execute(final Portfolio portfolio, final PurchaseStrategy strategy,
                                             final Collection<Participation> marketplace) {
        final Collection<ParticipationDescriptor> participations = marketplace.parallelStream()
                .map(d -> toDescriptor(d, auth))
                .collect(Collectors.toList());
        return PurchasingSession.purchase(portfolio, auth, participations, strategy);
    }
}
