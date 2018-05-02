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

package com.github.robozonky.app.portfolio;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.robozonky.api.notifications.SaleOfferedEvent;
import com.github.robozonky.api.notifications.SaleRecommendedEvent;
import com.github.robozonky.api.notifications.SaleRequestedEvent;
import com.github.robozonky.api.notifications.SellingCompletedEvent;
import com.github.robozonky.api.notifications.SellingStartedEvent;
import com.github.robozonky.api.remote.entities.RawInvestment;
import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.strategies.InvestmentDescriptor;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.api.strategies.RecommendedInvestment;
import com.github.robozonky.api.strategies.SellStrategy;
import com.github.robozonky.app.Events;
import com.github.robozonky.app.authentication.Tenant;
import com.github.robozonky.app.util.LoanCache;
import com.github.robozonky.app.util.SessionState;
import com.github.robozonky.common.remote.Select;
import com.github.robozonky.common.remote.Zonky;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements selling of {@link RawInvestment}s on the secondary marketplace. Use {@link #Selling(Supplier, boolean)} as
 * entry point.
 */
public class Selling implements PortfolioDependant {

    private static final Logger LOGGER = LoggerFactory.getLogger(Selling.class);

    private final Supplier<Optional<SellStrategy>> strategy;
    private final boolean isDryRun;

    /**
     * @param strategy Will be used to retrieve the strategy when needed.
     * @param isDryRun Whether or not to actually perform the remote selling operation or to just pretend.
     */
    public Selling(final Supplier<Optional<SellStrategy>> strategy, final boolean isDryRun) {
        this.strategy = strategy;
        this.isDryRun = isDryRun;
    }

    private InvestmentDescriptor getDescriptor(final Investment i, final Tenant auth) {
        return auth.call(zonky -> new InvestmentDescriptor(i, LoanCache.INSTANCE.getLoan(i, zonky)));
    }

    private Optional<Investment> processSale(final Zonky zonky, final RecommendedInvestment r,
                                             final SessionState<Investment> sold) {
        Events.fire(new SaleRequestedEvent(r));
        final Investment i = r.descriptor().item();
        if (isDryRun) {
            LOGGER.debug("Not sending sell request for loan #{} due to dry run.", i.getLoanId());
            sold.put(i); // make sure dry run never tries to sell this again in this instance
        } else {
            LOGGER.debug("Sending sell request for loan #{}.", i.getLoanId());
            zonky.sell(i);
            LOGGER.trace("Request over.");
        }
        Events.fire(new SaleOfferedEvent(i, r.descriptor().related()));
        return Optional.of(i);
    }

    private void sell(final Portfolio portfolio, final SellStrategy strategy, final Tenant tenant) {
        final Select s = new Select()
                .equalsPlain("onSmp", "CAN_BE_OFFERED_ONLY")
                .equals("status", "ACTIVE"); // this is how Zonky queries for this
        final SessionState<Investment> sold = new SessionState<>(tenant, Investment::getLoanId, "soldInvestments");
        final Set<InvestmentDescriptor> eligible = tenant.call(zonky -> zonky.getInvestments(s))
                .parallel()
                .filter(i -> !sold.contains(i)) // to make dry run function properly
                .map(i -> getDescriptor(i, tenant))
                .collect(Collectors.toSet());
        final PortfolioOverview overview = portfolio.calculateOverview();
        Events.fire(new SellingStartedEvent(eligible, overview));
        final Collection<Investment> investmentsSold = strategy.recommend(eligible, overview)
                .peek(r -> Events.fire(new SaleRecommendedEvent(r)))
                .map(r -> tenant.call(zonky -> processSale(zonky, r, sold)))
                .flatMap(o -> o.map(Stream::of).orElse(Stream.empty()))
                .collect(Collectors.toSet());
        Events.fire(new SellingCompletedEvent(investmentsSold, portfolio.calculateOverview()));
    }

    /**
     * Execute the strategy on a given portfolio. Won't do anything if the supplier in
     * {@link #Selling(Supplier, boolean)} returns and empty {@link Optional}.
     * @param portfolio Portfolio of investments to choose from.
     * @param auth Will be used to create remote connections to the Zonky server.
     */
    @Override
    public void accept(final Portfolio portfolio, final Tenant auth) {
        strategy.get().ifPresent(s -> sell(portfolio, s, auth));
    }
}
