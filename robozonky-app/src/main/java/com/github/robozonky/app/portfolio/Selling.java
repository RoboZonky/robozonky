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
import com.github.robozonky.api.remote.entities.Investment;
import com.github.robozonky.api.strategies.InvestmentDescriptor;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.api.strategies.RecommendedInvestment;
import com.github.robozonky.api.strategies.SellStrategy;
import com.github.robozonky.app.Events;
import com.github.robozonky.app.authentication.Authenticated;
import com.github.robozonky.app.util.DaemonRuntimeExceptionHandler;
import com.github.robozonky.common.remote.Zonky;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Selling implements PortfolioDependant {

    private static final Logger LOGGER = LoggerFactory.getLogger(Selling.class);

    private final Supplier<Optional<SellStrategy>> strategy;
    private final boolean isDryRun;

    public Selling(final Supplier<Optional<SellStrategy>> strategy, final boolean isDryRun) {
        this.strategy = strategy;
        this.isDryRun = isDryRun;
    }

    private static PortfolioOverview newPortfolioOverview(final Portfolio portfolio, final boolean isDryRun,
                                                          final Authenticated auth) {
        return auth.call(zonky -> portfolio.calculateOverview(zonky, isDryRun));
    }

    private InvestmentDescriptor getDescriptor(final Portfolio portfolio, final Investment i,
                                               final Authenticated auth) {
        return auth.call(zonky -> new InvestmentDescriptor(i, portfolio.getLoan(zonky, i.getLoanId())));
    }

    private Optional<Investment> processInvestment(final Zonky zonky, final RecommendedInvestment r) {
        try {
            Events.fire(new SaleRequestedEvent(r));
            final Investment i = r.descriptor().item();
            if (isDryRun) {
                LOGGER.debug("Not sending sell request for loan #{} due to dry run.", i.getLoanId());
            } else {
                LOGGER.debug("Sending sell request for loan #{}.", i.getLoanId());
                zonky.sell(i);
                i.setIsOnSmp(true); // send the investment to secondary marketplace; Portfolio class may use it later
                LOGGER.trace("Request over.");
            }
            Events.fire(new SaleOfferedEvent(i, isDryRun)); // only executes on actual successful sale
            return Optional.of(i);
        } catch (final Throwable t) { // prevent failure in one operation from trying other operations
            new DaemonRuntimeExceptionHandler().handle(t);
            return Optional.empty();
        }
    }

    private void sell(final Portfolio portfolio, final SellStrategy strategy, final Authenticated auth) {
        final PortfolioOverview overview = newPortfolioOverview(portfolio, isDryRun, auth);
        final Set<InvestmentDescriptor> eligible = portfolio.getActiveForSecondaryMarketplace().parallel()
                .map(i -> getDescriptor(portfolio, i, auth))
                .collect(Collectors.toSet());
        Events.fire(new SellingStartedEvent(eligible, overview));
        final Collection<Investment> investmentsSold = strategy.recommend(eligible, overview)
                .peek(r -> Events.fire(new SaleRecommendedEvent(r)))
                .map(r -> auth.call(zonky -> processInvestment(zonky, r)))
                .flatMap(o -> o.map(Stream::of).orElse(Stream.empty()))
                .collect(Collectors.toSet());
        Events.fire(new SellingCompletedEvent(investmentsSold, newPortfolioOverview(portfolio, isDryRun, auth)));
    }

    @Override
    public void accept(final Portfolio portfolio, final Authenticated auth) {
        strategy.get().ifPresent(s -> sell(portfolio, s, auth));
    }
}
