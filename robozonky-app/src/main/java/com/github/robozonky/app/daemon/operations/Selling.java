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
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.robozonky.api.remote.entities.RawInvestment;
import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.strategies.InvestmentDescriptor;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.api.strategies.RecommendedInvestment;
import com.github.robozonky.api.strategies.SellStrategy;
import com.github.robozonky.app.daemon.LoanCache;
import com.github.robozonky.app.daemon.Portfolio;
import com.github.robozonky.app.daemon.PortfolioDependant;
import com.github.robozonky.app.daemon.TransactionalPortfolio;
import com.github.robozonky.app.events.EventFactory;
import com.github.robozonky.common.Tenant;
import com.github.robozonky.common.remote.Select;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements selling of {@link RawInvestment}s on the secondary marketplace. Use {@link #Selling(Supplier)} as
 * entry point.
 */
public class Selling implements PortfolioDependant {

    private static final Logger LOGGER = LoggerFactory.getLogger(Selling.class);

    private final Supplier<Optional<SellStrategy>> strategy;

    /**
     * @param strategy Will be used to retrieve the strategy when needed.
     */
    public Selling(final Supplier<Optional<SellStrategy>> strategy) {
        this.strategy = strategy;
    }

    private static InvestmentDescriptor getDescriptor(final Investment i, final Tenant auth) {
        return new InvestmentDescriptor(i, () -> LoanCache.get().getLoan(i, auth));
    }

    private static Optional<Investment> processSale(final TransactionalPortfolio transactional,
                                                    final RecommendedInvestment r,
                                                    final SessionState<Investment> sold) {
        final Tenant tenant = transactional.getTenant();
        transactional.fire(EventFactory.saleRequested(r));
        final Investment i = r.descriptor().item();
        if (tenant.getSessionInfo().isDryRun()) {
            LOGGER.debug("Not sending sell request for loan #{} due to dry run.", i.getLoanId());
            sold.put(i); // make sure dry run never tries to sell this again in this instance
        } else {
            LOGGER.debug("Sending sell request for loan #{}.", i.getLoanId());
            tenant.run(z -> z.sell(i));
            LOGGER.trace("Request over.");
        }
        transactional.fire(EventFactory.saleOffered(i, r.descriptor().related()));
        return Optional.of(i);
    }

    private static void sell(final TransactionalPortfolio transactional, final SellStrategy strategy) {
        final Select sellable = new Select()
                .equalsPlain("onSmp", "CAN_BE_OFFERED_ONLY")
                .equals("status", "ACTIVE"); // this is how Zonky queries for this
        final Tenant tenant = transactional.getTenant();
        final SessionState<Investment> sold = new SessionState<>(tenant, Investment::getLoanId, "soldInvestments");
        final Set<InvestmentDescriptor> eligible = tenant.call(zonky -> zonky.getInvestments(sellable))
                .parallel()
                .filter(i -> !sold.contains(i)) // to make dry run function properly
                .map(i -> getDescriptor(i, tenant))
                .collect(Collectors.toSet());
        final Portfolio portfolio = transactional.getPortfolio();
        final PortfolioOverview overview = portfolio.getOverview();
        transactional.fire(EventFactory.sellingStarted(eligible, overview));
        final Collection<Investment> investmentsSold = strategy.recommend(eligible, overview)
                .peek(r -> transactional.fire(EventFactory.saleRecommended(r)))
                .map(r -> processSale(transactional, r, sold))
                .flatMap(o -> o.map(Stream::of).orElse(Stream.empty()))
                .collect(Collectors.toSet());
        transactional.fire(EventFactory.sellingCompleted(investmentsSold, portfolio.getOverview()));
    }

    /**
     * Execute the strategy on a given portfolio. Won't do anything if the supplier in
     * {@link #Selling(Supplier)} returns and empty {@link Optional}.
     * @param transactional Portfolio of investments to choose from.
     */
    @Override
    public void accept(final TransactionalPortfolio transactional) {
        strategy.get().ifPresent(s -> sell(transactional, s));
    }
}
