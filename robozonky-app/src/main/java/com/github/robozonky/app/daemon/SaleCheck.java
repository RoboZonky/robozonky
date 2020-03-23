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
import com.github.robozonky.api.remote.enums.InvestmentStatus;
import com.github.robozonky.app.tenant.PowerTenant;
import com.github.robozonky.internal.jobs.TenantPayload;
import com.github.robozonky.internal.tenant.Tenant;

final class SaleCheck implements TenantPayload {

    private static final Logger LOGGER = LogManager.getLogger(SaleCheck.class);

    private synchronized Optional<Investment> retrieveInvestmentIfSold(final SoldParticipationCache cache,
            final Tenant tenant, final int loanId) {
        final Optional<Investment> i = tenant.call(z -> z.getInvestmentByLoanId(loanId));
        if (i.isPresent()) {
            final Investment actual = i.get();
            if (actual.getStatus() == InvestmentStatus.SOLD) {
                return Optional.of(actual);
            } else if (actual.isOnSmp()) {
                LOGGER.debug("Investment for loan #{} is still on SMP.", loanId);
                return Optional.empty();
            } else {
                LOGGER.info("Investment for loan #{} was not sold.", loanId);
                cache.unmarkAsOffered(loanId);
                return Optional.empty();
            }
        } else {
            LOGGER.warn("Investment for loan #{} not found in the API.", loanId);
            cache.unmarkAsOffered(loanId);
            return Optional.empty();
        }
    }

    @Override
    public void accept(final Tenant tenant) {
        final SoldParticipationCache cache = SoldParticipationCache.forTenant(tenant);
        cache.getOffered()
            .mapToObj(id -> retrieveInvestmentIfSold(cache, tenant, id))
            .flatMap(Optional::stream)
            .forEach(sold -> {
                final int loanId = sold.getLoanId();
                cache.markAsSold(loanId);
                ((PowerTenant) tenant).fire(investmentSoldLazy(() -> {
                    final Loan l = tenant.getLoan(loanId);
                    return investmentSold(sold, l, tenant.getPortfolio()
                        .getOverview());
                }));
            });
    }
}
