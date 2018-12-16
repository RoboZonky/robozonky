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
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.robozonky.api.remote.entities.RawInvestment;
import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.strategies.InvestmentDescriptor;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.api.strategies.RecommendedInvestment;
import com.github.robozonky.api.strategies.SellStrategy;
import com.github.robozonky.app.daemon.LoanCache;
import com.github.robozonky.app.events.Events;
import com.github.robozonky.app.events.SessionEvents;
import com.github.robozonky.app.events.impl.EventFactory;
import com.github.robozonky.common.Tenant;
import com.github.robozonky.common.remote.Select;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements selling of {@link RawInvestment}s on the secondary marketplace.
 */
public class Selling implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(Selling.class);

    private final Tenant tenant;

    public Selling(final Tenant tenant) {
        this.tenant = tenant;
    }

    private static InvestmentDescriptor getDescriptor(final Investment i, final Tenant auth) {
        return new InvestmentDescriptor(i, () -> LoanCache.get().getLoan(i, auth));
    }

    private Optional<Investment> processSale(final RecommendedInvestment r, final SessionState<Investment> sold) {
        final SessionEvents evt = Events.forSession(tenant.getSessionInfo());
        evt.fire(EventFactory.saleRequested(r));
        final Investment i = r.descriptor().item();
        final boolean isRealRun = !tenant.getSessionInfo().isDryRun();
        LOGGER.debug("Will send sell request for loan #{}: {}.", i.getLoanId(), isRealRun);
        if (isRealRun) {
            tenant.run(z -> z.sell(i));
            LOGGER.trace("Request over.");
        }
        sold.put(i); // make sure dry run never tries to sell this again in this instance
        evt.fire(EventFactory.saleOffered(i, r.descriptor().related()));
        return Optional.of(i);
    }

    private void sell(final SellStrategy strategy) {
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
        final SessionEvents evt = Events.forSession(tenant.getSessionInfo());
        evt.fire(EventFactory.sellingStarted(eligible, overview));
        final Collection<Investment> investmentsSold = strategy.recommend(eligible, overview)
                .peek(r -> evt.fire(EventFactory.saleRecommended(r)))
                .map(r -> processSale(r, sold))
                .flatMap(o -> o.map(Stream::of).orElse(Stream.empty()))
                .collect(Collectors.toSet());
        evt.fire(EventFactory.sellingCompleted(investmentsSold, overview));
    }

    /**
     * Execute the strategy on a given portfolio.
     */
    @Override
    public void run() {
        tenant.getSellStrategy().ifPresent(this::sell);
    }
}
