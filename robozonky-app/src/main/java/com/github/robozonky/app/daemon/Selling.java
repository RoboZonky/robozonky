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

import static com.github.robozonky.app.events.impl.EventFactory.sellingCompletedLazy;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.ws.rs.InternalServerErrorException;

import org.apache.logging.log4j.Logger;

import com.github.robozonky.api.remote.entities.Investment;
import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.api.remote.enums.LoanHealth;
import com.github.robozonky.api.strategies.InvestmentDescriptor;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.api.strategies.RecommendedInvestment;
import com.github.robozonky.api.strategies.SellStrategy;
import com.github.robozonky.app.events.impl.EventFactory;
import com.github.robozonky.app.tenant.PowerTenant;
import com.github.robozonky.internal.jobs.TenantPayload;
import com.github.robozonky.internal.remote.Select;
import com.github.robozonky.internal.remote.Zonky;
import com.github.robozonky.internal.tenant.Tenant;

/**
 * Implements selling of {@link Investment}s on the secondary marketplace.
 */
final class Selling implements TenantPayload {

    private static final Logger LOGGER = Audit.selling();

    private static Optional<Investment> processSale(final PowerTenant tenant, final RecommendedInvestment r,
            final SoldParticipationCache sold) {
        final InvestmentDescriptor d = r.descriptor();
        final Investment i = d.item();
        final int loanId = i.getLoanId();
        try {
            final boolean isRealRun = !tenant.getSessionInfo()
                .isDryRun();
            if (isRealRun) {
                LOGGER.debug("Will send sell request for loan #{}.", loanId);
                var call = d.sellInfo()
                    .map(sellInfo -> (Consumer<Zonky>) zonky -> zonky.sell(i, sellInfo))
                    .orElseGet(() -> z -> z.sell(i));
                tenant.run(call);
            } else {
                LOGGER.debug("Will not send a real sell request for loan #{}, dry run.", loanId);
            }
            sold.markAsOffered(loanId);
            tenant.fire(EventFactory.saleOffered(i, d.related()));
            LOGGER.info("Offered to sell investment in loan #{}.", loanId);
            return Optional.of(i);
        } catch (final InternalServerErrorException ex) { // The sell endpoint has been seen to throw these.
            LOGGER.warn("Failed offering to sell investment in loan #{}.", loanId, ex);
            return Optional.empty();
        }
    }

    private static void sell(final PowerTenant tenant, final SellStrategy strategy) {
        final Select sellable = Select.unrestricted()
            .equalsPlain("delinquent", "true")
            .equalsPlain("onSmp", "CAN_BE_OFFERED_ONLY")
            .equals("status", "ACTIVE");
        final SoldParticipationCache sold = SoldParticipationCache.forTenant(tenant);
        LOGGER.debug("Starting to query for sellable investments.");
        final Set<InvestmentDescriptor> eligible = tenant.call(zonky -> zonky.getInvestments(sellable))
            .parallel() // this list is potentially very long, and investment pages take long to load; speed this up
            .filter(i -> sold.getOffered()
                .noneMatch(id -> id == i.getLoanId())) // to enable dry run
            .filter(i -> !sold.wasOnceSold(i.getLoanId()))
            .map(i -> i.getLoanHealthInfo()
                .map(healthInfo -> {
                    Supplier<Loan> loanSupplier = () -> tenant.getLoan(i.getLoanId());
                    if (healthInfo == LoanHealth.HEALTHY) {
                        return new InvestmentDescriptor(i, loanSupplier);
                    } else {
                        return new InvestmentDescriptor(i, loanSupplier, () -> tenant.getSellInfo(i.getId()));
                    }
                })
                .orElseThrow())
            .collect(Collectors.toSet());
        final PortfolioOverview overview = tenant.getPortfolio()
            .getOverview();
        tenant.fire(EventFactory.sellingStarted(overview));
        var recommended = strategy.recommend(eligible, overview, tenant.getSessionInfo())
            .peek(r -> tenant.fire(EventFactory.saleRecommended(r)));
        var throttled = new SellingThrottle().apply(recommended, overview);
        final Collection<Investment> investmentsSold = throttled
            .map(r -> processSale(tenant, r, sold))
            .flatMap(Optional::stream)
            .collect(Collectors.toSet());
        tenant.fire(sellingCompletedLazy(() -> EventFactory.sellingCompleted(investmentsSold,
                tenant.getPortfolio()
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
