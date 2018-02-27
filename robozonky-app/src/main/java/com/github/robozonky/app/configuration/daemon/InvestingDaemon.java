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

package com.github.robozonky.app.configuration.daemon;

import java.time.Duration;
import java.util.Collection;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.remote.entities.sanitized.MarketplaceLoan;
import com.github.robozonky.api.strategies.InvestmentStrategy;
import com.github.robozonky.api.strategies.LoanDescriptor;
import com.github.robozonky.app.authentication.Authenticated;
import com.github.robozonky.app.investing.Investing;
import com.github.robozonky.app.investing.Investor;
import com.github.robozonky.app.portfolio.Portfolio;
import com.github.robozonky.app.util.LoanCache;
import com.github.robozonky.common.remote.Select;
import com.github.robozonky.internal.api.Defaults;

class InvestingDaemon extends DaemonOperation {

    /**
     * Will make sure that the endpoint only loads loans that are on the marketplace, and not the entire history.
     */
    private static final Select SELECT = new Select().greaterThan("remainingInvestment", 0);
    private final BiConsumer<Portfolio, Authenticated> investor;

    public InvestingDaemon(final Consumer<Throwable> shutdownCall, final Authenticated auth,
                           final Investor.Builder builder, final Supplier<Optional<InvestmentStrategy>> strategy,
                           final PortfolioSupplier portfolio, final Duration maximumSleepPeriod,
                           final Duration refreshPeriod) {
        super(shutdownCall, auth, portfolio, refreshPeriod);
        this.investor = (p, a) -> {
            // don't query anything unless we have enough money to invest
            final int balance = p.getRemoteBalance().get().intValue();
            final int minimum = Defaults.MINIMUM_INVESTMENT_IN_CZK;
            if (balance < minimum) {
                LOGGER.debug("Asleep as there is not enough available balance. ({} < {})", balance, minimum);
                return;
            }
            // query marketplace for investment opportunities
            final Investing i = new Investing(builder, strategy, a, maximumSleepPeriod);
            final Collection<MarketplaceLoan> loans =
                    a.call(zonky -> zonky.getAvailableLoans(SELECT)).collect(Collectors.toList());
            final Collection<LoanDescriptor> descriptors = loans.stream().parallel()
                    .map(l -> {
                        /*
                         * Loan is first retrieved from the authenticated API. This way, we get all available
                         * information, such as borrower nicknames from other loans made by the same person.
                         */
                        final Loan complete = a.call(zonky -> LoanCache.INSTANCE.getLoan(l.getId(), zonky));
                        /*
                         * We update the loan within the cache with latest information from the marketplace. This is
                         * done so that we don't cache stale loan information, such as how much of the loan is remaining
                         * to be invested.
                         */
                        Loan.updateFromMarketplace(complete, l);
                        return complete;
                    })
                    .map(LoanDescriptor::new)
                    .collect(Collectors.toList());
            // trigger the strategy
            i.apply(p, descriptors);
        };
    }

    @Override
    protected boolean isEnabled(final Authenticated authenticated) {
        return !authenticated.getRestrictions().isCannotInvest();
    }

    @Override
    protected BiConsumer<Portfolio, Authenticated> getInvestor() {
        return investor;
    }
}
