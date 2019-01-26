/*
 * Copyright 2019 The RoboZonky Project
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

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.robozonky.api.remote.entities.RawInvestment;
import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.strategies.InvestmentDescriptor;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.api.strategies.RecommendedInvestment;
import com.github.robozonky.api.strategies.SellStrategy;
import com.github.robozonky.app.events.impl.EventFactory;
import com.github.robozonky.app.tenant.PowerTenant;
import com.github.robozonky.common.jobs.TenantPayload;
import com.github.robozonky.common.remote.Select;
import com.github.robozonky.common.tenant.Tenant;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.github.robozonky.app.events.impl.EventFactory.sellingCompletedLazy;
import static com.github.robozonky.app.events.impl.EventFactory.sellingStartedLazy;

/**
 * Implements selling of {@link RawInvestment}s on the secondary marketplace.
 */
final class Selling implements TenantPayload {

    private static final Logger LOGGER = LogManager.getLogger(Selling.class);

    private static InvestmentDescriptor getDescriptor(final Investment i, final Tenant tenant) {
        return new InvestmentDescriptor(i, () -> tenant.getLoan(i.getLoanId()));
    }

    private static Optional<Investment> processSale(final PowerTenant tenant, final RecommendedInvestment r,
                                                    final SessionState<Investment> sold) {
        tenant.fire(EventFactory.saleRequested(r));
        final Investment i = r.descriptor().item();
        final boolean isRealRun = !tenant.getSessionInfo().isDryRun();
        LOGGER.debug("Will send sell request for loan #{}: {}.", i.getLoanId(), isRealRun);
        if (isRealRun) {
            tenant.run(z -> z.sell(i));
            LOGGER.info("Offered to sell investment in loan #{}.", i.getLoanId());
        }
        sold.put(i); // make sure dry run never tries to sell this again in this instance
        tenant.fire(EventFactory.saleOffered(i, r.descriptor().related()));
        return Optional.of(i);
    }

    private static void sell(final PowerTenant tenant, final SellStrategy strategy) {
        final Select sellable = new Select()
                .equalsPlain("onSmp", "CAN_BE_OFFERED_ONLY")
                .equals("status", "ACTIVE"); // this is how Zonky queries for this
        final Set<Investment> marketplace = tenant.call(zonky -> zonky.getInvestments(sellable))
                .parallel()
                .collect(Collectors.toSet());
        final SessionState<Investment> sold = new SessionState<>(tenant, marketplace, Investment::getLoanId,
                                                                 "soldInvestments");
        final Set<InvestmentDescriptor> eligible = marketplace.stream()
                .filter(i -> !sold.contains(i)) // to make dry run function properly
                .map(i -> getDescriptor(i, tenant))
                .collect(Collectors.toSet());
        final PortfolioOverview overview = tenant.getPortfolio().getOverview();
        tenant.fire(sellingStartedLazy(() -> EventFactory.sellingStarted(eligible, overview)));
        final Collection<Investment> investmentsSold = strategy.recommend(eligible, overview)
                .peek(r -> tenant.fire(EventFactory.saleRecommended(r)))
                .map(r -> processSale(tenant, r, sold))
                .flatMap(o -> o.map(Stream::of).orElse(Stream.empty()))
                .collect(Collectors.toSet());
        tenant.fire(sellingCompletedLazy(() -> EventFactory.sellingCompleted(investmentsSold,
                                                                             tenant.getPortfolio().getOverview())));
    }

    @Override
    public void accept(final Tenant tenant) {
        tenant.getSellStrategy().ifPresent(s -> sell((PowerTenant) tenant, s));
    }
}
