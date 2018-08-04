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
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.stream.Collectors;

import com.github.robozonky.api.remote.entities.BlockedAmount;
import com.github.robozonky.api.remote.entities.Statistics;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.app.authentication.Tenant;
import com.github.robozonky.app.configuration.daemon.TransactionalPortfolio;
import com.github.robozonky.app.util.LoanCache;
import com.github.robozonky.common.remote.Select;
import com.github.robozonky.common.remote.Zonky;

public class Carrier {

    private Transactions transactions;

    private Carrier(final Transactions transactions) {
        this.transactions = transactions;
    }

    private static void updateFromZonky(final Tenant tenant, final Transactions transactions) {
        // load all transactions
        final Select fromBeginning = new Select()
                .greaterThanOrEquals("transaction.transactionDate", transactions.getMinimalEpoch());
        tenant.call(z -> z.getTransactions(fromBeginning)).forEach(transactions::fromZonky);
        // load all blocked amounts
        tenant.call(Zonky::getBlockedAmounts).forEach(transactions::fromZonky);
    }

    public static Carrier create(final Tenant tenant, final Statistics statistics) {
        // set all transactions to have happened in the previous epoch
        final OffsetDateTime zonkyUpdate = statistics.getTimestamp();
        final Transactions transactions = new Transactions(zonkyUpdate.minus(Duration.ofHours(1)));
        updateFromZonky(tenant, transactions);
        // now move to the current epoch
        final Transactions currentTransactions = transactions.rebase(zonkyUpdate);
        return new Carrier(currentTransactions);
    }

    /**
     * Returns the sum of investments which the robot has made, but which we have not yet received back from the API
     * in terms of {@link Statistics} update.
     * @return Sum of synthetic transactions that will eventually be replaced by proper {@link BlockedAmount}s.
     */
    public BigDecimal getUndetectedBlockedBalance() {
        return transactions.getUnprocessed()
                .filter(t -> t.getSource() == TransactionSource.SYNTHETIC)
                .map(SourceAgnosticTransaction::getAmount)
                .map(BigDecimal::abs)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public PortfolioOverview getPortfolioOverview(final Tenant tenant, final BigDecimal onlineBalance,
                                                  final Statistics onlineStatistics) {
        final BigDecimal actualBalance = onlineBalance.subtract(getUndetectedBlockedBalance());
        final Map<Rating, BigDecimal> unprocessedPortfolioAdjustments = transactions.getUnprocessed()
                .filter(t -> t.getLoanId() > 0) // skip blocked amounts and fees
                .collect(Collectors.groupingBy(t -> LoanCache.INSTANCE.getLoan(t.getLoanId(), tenant).getRating(),
                                               Collectors.reducing(BigDecimal.ZERO,
                                                                   SourceAgnosticTransaction::getAmount,
                                                                   BigDecimal::subtract)));
        return PortfolioOverview.calculate(actualBalance, onlineStatistics, unprocessedPortfolioAdjustments,
                                           Delinquencies.getAmountsAtRisk());
    }

    public void simulateInvestment(final int loanId, final BigDecimal amount) {
        transactions.fromInvestment(loanId, amount);
    }

    public void simulatePurchase(final int loanId, final BigDecimal amount) {
        transactions.fromPurchase(loanId, amount);
    }

    public void reloadFromZonky(final TransactionalPortfolio portfolio) {
        final Portfolio folio = portfolio.getPortfolio();
        final Statistics statistics = folio.getStatistics();
        transactions = transactions.rebase(statistics.getTimestamp());
        final Tenant tenant = portfolio.getTenant();
        updateFromZonky(tenant, transactions);
        LoanRepaidProcessor.INSTANCE.accept(transactions.getUnprocessed(), portfolio);
        ParticipationSoldProcessor.INSTANCE.accept(transactions.getUnprocessed(), portfolio);
    }
}
