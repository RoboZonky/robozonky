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

package com.github.robozonky.app.daemon;

import java.time.Duration;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.github.robozonky.api.remote.entities.Participation;
import com.github.robozonky.api.strategies.PurchaseStrategy;
import com.github.robozonky.app.daemon.operations.Purchasing;
import com.github.robozonky.app.daemon.transactions.SoldParticipationCache;
import com.github.robozonky.common.Tenant;
import com.github.robozonky.common.remote.Select;

class PurchasingDaemon extends DaemonOperation {

    private final Purchasing purchasing;
    private final SoldParticipationCache soldParticipationCache;

    public PurchasingDaemon(final Consumer<Throwable> shutdownCall, final Tenant auth,
                            final Supplier<Optional<PurchaseStrategy>> strategy, final PortfolioSupplier portfolio,
                            final Duration refreshPeriod) {
        super(shutdownCall, auth, portfolio, refreshPeriod);
        this.purchasing = new Purchasing(strategy, auth);
        this.soldParticipationCache = SoldParticipationCache.forTenant(auth);
    }

    @Override
    protected boolean isEnabled(final Tenant authenticated) {
        return !authenticated.getRestrictions().isCannotAccessSmp();
    }

    @Override
    protected void execute(final Portfolio portfolio, final Tenant authenticated) {
        final long balance = portfolio.getRemoteBalance().get().longValue();
        if (balance <= 0) {
            LOGGER.debug("Asleep as there is not enough available balance. ({} < {})", balance, 0);
            return;
        }
        final Select s = new Select()
                .lessThanOrEquals("remainingPrincipal", balance)
                .equalsPlain("willNotExceedLoanInvestmentLimit", "true");
        final Collection<Participation> applicable = authenticated.call(zonky -> zonky.getAvailableParticipations(s))
                .filter(p -> { // never re-purchase what was once sold
                    final int loanId = p.getLoanId();
                    final boolean wasSoldBefore = soldParticipationCache.wasOnceSold(loanId);
                    if (wasSoldBefore) {
                        LOGGER.debug("Ignoring loan #{} as the user had already sold it before.", loanId);
                    }
                    return !wasSoldBefore;
                })
                .collect(Collectors.toList());
        purchasing.apply(portfolio, applicable);
    }
}
