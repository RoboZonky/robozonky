/*
 * Copyright 2021 The RoboZonky Project
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

import static com.github.robozonky.app.events.impl.EventFactory.sellingCompletedLazy;

import java.util.Optional;
import java.util.function.Supplier;

import javax.ws.rs.InternalServerErrorException;

import org.apache.logging.log4j.Logger;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.remote.entities.Investment;
import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.api.strategies.InvestmentDescriptor;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.api.strategies.SellStrategy;
import com.github.robozonky.app.events.impl.EventFactory;
import com.github.robozonky.app.tenant.PowerTenant;
import com.github.robozonky.internal.jobs.TenantPayload;
import com.github.robozonky.internal.remote.Zonky;
import com.github.robozonky.internal.remote.entities.InvestmentImpl;
import com.github.robozonky.internal.tenant.Tenant;

/**
 * Implements selling of {@link InvestmentImpl}s on the secondary marketplace.
 */
final class Selling implements TenantPayload {

    private static final Logger LOGGER = Audit.selling();

    private static Optional<Investment> processSale(final PowerTenant tenant, final RecommendedInvestment r,
            final SoldParticipationCache sold) {
        final InvestmentDescriptor d = r.descriptor();
        final Investment i = d.item();
        final int loanId = i.getLoan()
            .getId();
        try {
            final boolean isRealRun = !tenant.getSessionInfo()
                .isDryRun();
            if (isRealRun) {
                LOGGER.debug("Will send sell request for loan #{}.", loanId);
                tenant.run(zonky -> zonky.sell(i));
            } else {
                LOGGER.debug("Will not send a real sell request for loan #{}, dry run.", loanId);
            }
            sold.markAsOffered(i.getId());
            tenant.fire(EventFactory.saleOffered(i, d.related()));
            LOGGER.info("Offered to sell investment in loan #{}.", loanId);
            return Optional.of(i);
        } catch (final InternalServerErrorException ex) { // The sell endpoint has been seen to throw these.
            LOGGER.warn("Failed offering to sell investment in loan #{}.", loanId, ex);
            return Optional.empty();
        }
    }

    private static void sell(final PowerTenant tenant, final SellStrategy strategy) {
        final PortfolioOverview overview = tenant.getPortfolio()
            .getOverview();
        tenant.fire(EventFactory.sellingStarted(overview));
        LOGGER.debug("Starting to query for sellable investments.");
        final SoldParticipationCache sold = SoldParticipationCache.forTenant(tenant);
        var recommended = tenant.call(Zonky::getSellableInvestments)
            .parallel()
            .filter(investment -> { // Only sell if the remaining amount is more than 1. Be nice to people.
                var remainingInterest = investment.getInterest()
                    .getUnpaid();
                var remainingPrincipal = investment.getPrincipal()
                    .getUnpaid();
                var remaining = remainingInterest.add(remainingPrincipal);
                return (remaining.compareTo(Money.from(1)) > 0);
            })
            .filter(investment -> sold.getOffered()
                .noneMatch(investmentId -> investmentId == investment.getId())) // To enable dry run.
            .filter(i -> !sold.wasOnceSold(i.getId()))
            .map(i -> {
                Supplier<Loan> loanSupplier = () -> tenant.getLoan(i.getLoan()
                    .getId());
                return new InvestmentDescriptor(i, loanSupplier);
            })

            .filter(i -> strategy.recommend(i, () -> tenant.getPortfolio()
                .getOverview(), tenant.getSessionInfo()))
            .map(RecommendedInvestment::new);
        new SellingThrottle().apply(recommended, overview)
            .forEach(r -> processSale(tenant, r, sold));
        tenant.fire(sellingCompletedLazy(() -> EventFactory.sellingCompleted(tenant.getPortfolio()
            .getOverview())));
    }

    @Override
    public void accept(final Tenant tenant) {
        var canAccessSmp = tenant.getSessionInfo()
            .canAccessSmp();
        if (canAccessSmp) {
            tenant.getSellStrategy()
                .ifPresentOrElse(s -> sell((PowerTenant) tenant, s),
                        () -> LOGGER.debug("Not selling anything as selling strategy is missing."));
        } else {
            LOGGER.debug("Access to marketplace disabled by Zonky.");
        }
    }
}
