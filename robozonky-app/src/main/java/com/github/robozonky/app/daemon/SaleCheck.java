/*
 * Copyright 2020 The RoboZonky Project
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

import static com.github.robozonky.app.events.impl.EventFactory.investmentSold;
import static com.github.robozonky.app.events.impl.EventFactory.investmentSoldLazy;

import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.robozonky.api.remote.entities.Investment;
import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.app.tenant.PowerTenant;
import com.github.robozonky.internal.jobs.TenantPayload;
import com.github.robozonky.internal.tenant.Tenant;

final class SaleCheck implements TenantPayload {

    private static final Logger LOGGER = LogManager.getLogger(SaleCheck.class);

    private synchronized Optional<Investment> retrieveInvestmentIfSold(final SoldParticipationCache cache,
            final Tenant tenant, final long investmentId) {
        // Don't use the cached version here, we need to know the very latest info.
        var investment = tenant.getInvestment(investmentId, true);
        if (investment == null) {
            throw new IllegalStateException("Investment #" + investmentId + " not found.");
        }
        var loanId = investment.getLoan()
            .getId();
        switch (investment.getSellStatus()) {
            case SOLD:
                return Optional.of(investment);
            case OFFERED:
                LOGGER.debug("Investment #{} for loan #{} is still on SMP.", investmentId, loanId);
                return Optional.empty();
            default:
                LOGGER.info("Investment #{} for loan #{} was not sold.", investmentId, loanId);
                cache.unmarkAsOffered(investmentId);
                return Optional.empty();
        }
    }

    @Override
    public void accept(final Tenant tenant) {
        final SoldParticipationCache cache = SoldParticipationCache.forTenant(tenant);
        cache.getOffered()
            .mapToObj(investmentId -> retrieveInvestmentIfSold(cache, tenant, investmentId))
            .flatMap(Optional::stream)
            .forEach(sold -> {
                cache.markAsSold(sold.getId());
                ((PowerTenant) tenant).fire(investmentSoldLazy(() -> {
                    final Loan l = tenant.getLoan(sold.getLoan()
                        .getId());
                    return investmentSold(sold, l, tenant.getPortfolio()
                        .getOverview());
                }));
            });
    }
}
