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

package com.github.robozonky.app.portfolio;

import java.math.BigDecimal;
import java.util.Collection;

import com.github.robozonky.api.notifications.InvestmentSoldEvent;
import com.github.robozonky.api.remote.entities.Statistics;
import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.app.Events;
import com.github.robozonky.app.authentication.Tenant;
import com.github.robozonky.app.util.LoanCache;
import com.github.robozonky.common.remote.Zonky;

/**
 * Fresh instances are created using {@link #create(Tenant, RemoteBalance)}. Daily Zonky morning updates should be
 * performed using {@link #reloadFromZonky(Tenant, RemoteBalance)}.
 */
public class Portfolio {

    private final Statistics statistics;
    private final RemoteBalance balance;
    private final TransactionLog transactions;

    Portfolio(final RemoteBalance balance) {
        this(Statistics.empty(), balance);
    }

    Portfolio(final Statistics statistics, final RemoteBalance balance) {
        this.transactions = new TransactionLog();
        this.statistics = statistics;
        this.balance = balance;
    }

    Portfolio(final Tenant tenant, final Collection<Synthetic> synthetics, final RemoteBalance balance) {
        this.transactions = new TransactionLog(tenant, synthetics);
        this.statistics = tenant.call(Zonky::getStatistics);
        this.balance = balance;
    }

    /**
     * Return a new instance of the class, loading information about all investments present and past from the Zonky
     * interface. This operation may take a while, as there may easily be hundreds or thousands of such investments.
     * @param tenant The API to be used to retrieve the data from Zonky.
     * @param balance Tracker for the presently available balance.
     * @return Empty in case there was a remote error.
     */
    public static Portfolio create(final Tenant tenant, final RemoteBalance balance) {
        return new Portfolio(tenant.call(Zonky::getStatistics), balance);
    }

    private static Investment lookupOrFail(final Loan loan, final Tenant auth) {
        return auth.call(zonky -> zonky.getInvestment(loan))
                .orElseThrow(() -> new IllegalStateException("Investment not found for loan " + loan.getId()));
    }

    public Portfolio reloadFromZonky(final Tenant tenant, final RemoteBalance balance) {
        return new Portfolio(tenant, transactions.getSynthetics(), balance);
    }

    public void updateTransactions(final Tenant tenant) {
        final Collection<Integer> idsOfNewlySoldLoans = transactions.update(statistics, tenant);
        final PortfolioOverview po = calculateOverview();
        for (final int loanId : idsOfNewlySoldLoans) { // notify of loans that were just detected as sold
            final Loan l = LoanCache.INSTANCE.getLoan(loanId, tenant);
            final Investment i = lookupOrFail(l, tenant);
            Events.fire(new InvestmentSoldEvent(i, l, po));
        }
    }

    private void simulateOperation(final Tenant tenant, final int loanId, final BigDecimal amount) {
        transactions.addNewSynthetic(tenant, loanId, amount);
        balance.update(amount.negate());
    }

    public void simulateInvestment(final Tenant tenant, final int loanId, final BigDecimal amount) {
        simulateOperation(tenant, loanId, amount);
    }

    public void simulatePurchase(final Tenant tenant, final int loanId, final BigDecimal amount) {
        simulateOperation(tenant, loanId, amount);
    }

    Statistics getStatistics() {
        return statistics;
    }

    public RemoteBalance getRemoteBalance() {
        return balance;
    }

    public PortfolioOverview calculateOverview() {
        return PortfolioOverview.calculate(getRemoteBalance().get(), statistics, transactions.getAdjustments(),
                                           Delinquencies.getAmountsAtRisk());
    }
}
