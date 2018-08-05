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
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.github.robozonky.api.remote.entities.BlockedAmount;
import com.github.robozonky.api.remote.entities.Statistics;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.app.authentication.Tenant;
import com.github.robozonky.app.configuration.daemon.Transactional;
import com.github.robozonky.app.util.LoanCache;
import com.github.robozonky.common.remote.Select;
import com.github.robozonky.common.remote.Zonky;
import com.github.robozonky.util.LazyInitialized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransferMonitor implements PortfolioDependant {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransferMonitor.class);

    private final LazyInitialized<Map<Rating, BigDecimal>> adjustments;
    private Transfers transfers;

    private TransferMonitor(final Transfers transfers) {
        this.transfers = transfers;
        this.adjustments = LazyInitialized.create(this::calculateAdjustments);
    }

    private static Rating getRating(final Tenant tenant, final int loanId) {
        return LoanCache.INSTANCE.getLoan(loanId, tenant).getRating();
    }

    private static boolean updateFromZonky(final Tenant tenant, final Transfers transfers) {
        // load all transactions
        final Select fromBeginning = new Select()
                .greaterThanOrEquals("transaction.transactionDate", transfers.getMinimalEpoch());
        final boolean updated = tenant.call(z -> z.getTransactions(fromBeginning))
                .filter(t -> t.getLoanId() > 0)
                .map(t -> transfers.fromZonky(t, () -> getRating(tenant, t.getLoanId())))
                .reduce(false, (a, b) -> a || b);
        // load all blocked amounts
        final boolean updated2 = tenant.call(Zonky::getBlockedAmounts)
                .filter(t -> t.getLoanId() > 0)
                .map(ba -> transfers.fromZonky(ba, () -> getRating(tenant, ba.getLoanId())))
                .reduce(false, (a, b) -> a || b);
        return (updated || updated2);
    }

    static TransferMonitor create(final Tenant tenant) {
        // set all transactions to have happened in the previous epoch
        final OffsetDateTime zonkyUpdate = tenant.call(Zonky::getStatistics).getTimestamp();
        final Transfers transactions = new Transfers(zonkyUpdate.minus(Duration.ofHours(1)));
        updateFromZonky(tenant, transactions);
        // now move to the current epoch
        final Transfers currentTransactions = transactions.rebase(zonkyUpdate);
        return new TransferMonitor(currentTransactions);
    }

    public static Supplier<TransferMonitor> createLazy(final Tenant tenant) {
        final LazyInitialized<TransferMonitor> monitor =
                LazyInitialized.create(() -> TransferMonitor.create(tenant));
        return monitor::get;
    }

    /**
     * Returns the sum of investments which the robot has made, but which we have not yet received back from the API
     * in terms of {@link Statistics} update.
     * @return Sum of synthetic transactions that will eventually be replaced by proper {@link BlockedAmount}s.
     */
    BigDecimal getUndetectedBlockedBalance() {
        return transfers.getUnprocessed()
                .filter(t -> t.getSource() == TransferSource.SYNTHETIC)
                .map(SourceAgnosticTransfer::getAmount)
                .map(BigDecimal::abs)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Map<Rating, BigDecimal> calculateAdjustments() {
        final Map<Rating, BigDecimal> result = transfers.getUnprocessed()
                .collect(Collectors.groupingBy(SourceAgnosticTransfer::getRating,
                                               Collectors.reducing(BigDecimal.ZERO,
                                                                   SourceAgnosticTransfer::getAmount,
                                                                   BigDecimal::subtract)));
        LOGGER.debug("New adjustments: {}", result);
        return result;
    }

    Map<Rating, BigDecimal> getAdjustments() {
        return adjustments.get();
    }

    void simulateInvestment(final int loanId, final Rating rating, final BigDecimal amount) {
        final boolean updated = transfers.fromInvestment(loanId, rating, amount);
        if (updated) {
            adjustments.reset();
        }
    }

    void simulatePurchase(final int loanId, final Rating rating, final BigDecimal amount) {
        final boolean updated = transfers.fromPurchase(loanId, rating, amount);
        if (updated) {
            adjustments.reset();
        }
    }

    @Override
    public void accept(final Transactional transactional) {
        final Portfolio folio = transactional.getPortfolio();
        final Statistics statistics = folio.getStatistics();
        transfers = transfers.rebase(statistics.getTimestamp());
        final Tenant tenant = transactional.getTenant();
        final boolean updated = updateFromZonky(tenant, transfers);
        if (updated) {
            adjustments.reset();
        }
        LoanRepaidProcessor.INSTANCE.accept(transfers.getUnprocessed(), transactional);
        ParticipationSoldProcessor.INSTANCE.accept(transfers.getUnprocessed(), transactional);
    }
}
