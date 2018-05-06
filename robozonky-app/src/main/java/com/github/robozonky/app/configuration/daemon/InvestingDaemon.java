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
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.strategies.InvestmentStrategy;
import com.github.robozonky.api.strategies.LoanDescriptor;
import com.github.robozonky.app.authentication.Tenant;
import com.github.robozonky.app.investing.Investing;
import com.github.robozonky.app.investing.Investor;
import com.github.robozonky.app.portfolio.Portfolio;
import com.github.robozonky.app.util.LoanCache;
import com.github.robozonky.common.remote.Select;

class InvestingDaemon extends DaemonOperation {

    /**
     * Will make sure that the endpoint only loads loans that are on the marketplace, and not the entire history.
     */
    private static final Select SELECT = new Select().greaterThan("remainingInvestment", 0);
    private final Investing investing;

    public InvestingDaemon(final Consumer<Throwable> shutdownCall, final Tenant auth,
                           final Investor investor, final Supplier<Optional<InvestmentStrategy>> strategy,
                           final PortfolioSupplier portfolio, final Duration refreshPeriod) {
        super(shutdownCall, auth, portfolio, refreshPeriod);
        this.investing = new Investing(investor, strategy, auth);
    }

    @Override
    protected boolean isEnabled(final Tenant authenticated) {
        return !authenticated.getRestrictions().isCannotInvest();
    }

    @Override
    protected void execute(final Portfolio portfolio, final Tenant authenticated) {
        // don't query anything unless we have enough money to invest
        final int balance = portfolio.getRemoteBalance().get().intValue();
        final int minimum = authenticated.getRestrictions().getMinimumInvestmentAmount();
        if (balance < minimum) {
            LOGGER.debug("Asleep as there is not enough available balance. ({} < {})", balance, minimum);
            return;
        }
        // query marketplace for investment opportunities
        final Collection<LoanDescriptor> loans = authenticated.call(zonky -> zonky.getAvailableLoans(SELECT))
                .parallel()
                .filter(l -> !l.getMyInvestment().isPresent()) // re-investing would fail
                .map(l -> {
                    /*
                     * Loan is first retrieved from the authenticated API. This way, we get all available
                     * information, such as borrower nicknames from other loans made by the same person.
                     */
                    final Loan complete = authenticated.call(zonky -> LoanCache.INSTANCE.getLoan(l.getId(), zonky));
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
        investing.apply(portfolio, loans);
    }
}
