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
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

import com.github.robozonky.api.remote.entities.BlockedAmount;
import com.github.robozonky.api.remote.entities.Statistics;
import com.github.robozonky.api.remote.entities.Transaction;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.remote.enums.TransactionCategory;
import com.github.robozonky.api.remote.enums.TransactionOrientation;
import com.github.robozonky.app.authentication.Tenant;
import com.github.robozonky.common.remote.Select;
import com.github.robozonky.common.remote.Zonky;
import org.eclipse.collections.api.set.primitive.IntSet;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.eclipse.collections.impl.set.mutable.primitive.IntHashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class TransactionLog {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionLog.class);
    private static final Collection<TransactionCategory> TRANSACTION_CATEGORIES =
            Arrays.asList(TransactionCategory.SMP_BUY, TransactionCategory.SMP_SELL), BLOCKED_AMOUNT_CATEGORIES =
            Arrays.asList(TransactionCategory.INVESTMENT);

    private final Map<Rating, BigDecimal> adjustments = new EnumMap<>(Rating.class);
    private final Collection<BlockedAmount> blockedAmounts = UnifiedSet.newSet(0);
    private final Collection<Transaction> transactionsIn = UnifiedSet.newSet(0), transactionsOut = UnifiedSet.newSet(0);
    private final Collection<Synthetic> synthetics = UnifiedSet.newSet(0);

    public TransactionLog() {

    }

    public TransactionLog(final Tenant tenant, final Collection<Synthetic> survivingSynthetics) {
        LOGGER.debug("Survived synthetic transactions: {}", survivingSynthetics);
        survivingSynthetics.forEach(s -> addNewSynthetic(tenant, s.getLoanId(), s.getAmount()));
    }

    private static Rating getLoanRating(final Tenant tenant, final int loanId) {
        return tenant.call(z -> z.getLoan(loanId).getRating());
    }

    public Collection<Synthetic> getSynthetics() {
        return Collections.unmodifiableCollection(synthetics);
    }

    private synchronized void updateRatingShare(final Tenant tenant, final IntSupplier loanId,
                                                final Supplier<BigDecimal> amount) {
        final Rating r = getLoanRating(tenant, loanId.getAsInt());
        adjustments.compute(r, (k, v) -> {
            final BigDecimal a = amount.get();
            return v == null ? a : v.add(a);
        });
    }

    public synchronized Map<Rating, BigDecimal> getAdjustments() {
        return Collections.unmodifiableMap(adjustments);
    }

    public synchronized void addNewSynthetic(final Tenant tenant, final int loanId, final BigDecimal amount) {
        final Synthetic s = new Synthetic(loanId, amount);
        LOGGER.debug("Adding new synthetic transaction: {}.", s);
        synthetics.add(s);
        updateRatingShare(tenant, s::getLoanId, s::getAmount);
        LOGGER.debug("New adjustments: {}.", adjustments);
    }

    private boolean processTransaction(final Tenant tenant, final Transaction t) {
        final boolean isInward = t.getOrientation() == TransactionOrientation.IN;
        final Collection<Transaction> transactions = isInward ? transactionsIn : transactionsOut;
        final Collection<Synthetic> synthetics = isInward ? Collections.emptyList() : this.synthetics;
        final boolean unseenTransaction = transactions.add(t);
        if (!unseenTransaction) { // already processed this; don't do anything
            return false;
        }
        final boolean hadCorrespondingSynthetic = synthetics.removeIf(s -> Synthetic.equals(s, t));
        if (hadCorrespondingSynthetic) {  // transaction added, synthetic removed, no change overall
            return false;
        }
        LOGGER.debug("There was no corresponding synthetic, updating rating shares.");
        updateRatingShare(tenant, t::getLoanId,
                          isInward ? () -> t.getAmount().negate() : t::getAmount);
        return (t.getCategory() == TransactionCategory.SMP_SELL); // SMP_SELL means something was sold
    }

    private void processBlockedAmount(final Tenant tenant, final BlockedAmount ba) {
        final boolean unseenBlockedAmount = blockedAmounts.add(ba);
        if (!unseenBlockedAmount) { // already processed this; don't do anything
            return;
        }
        final boolean hadCorrespondingSynthetic = synthetics.removeIf(s -> Synthetic.equals(s, ba));
        if (hadCorrespondingSynthetic) { // blockation added, synthetic removed, no change overall
            return;
        }
        LOGGER.debug("There was no corresponding synthetic, updating rating shares.");
        updateRatingShare(tenant, ba::getLoanId, ba::getAmount);
    }

    public synchronized int[] update(final Statistics statistics, final Tenant tenant) {
        // all new transactions have the date at the beginning of the day when the timestamp was taken
        final LocalDate lastZonkyUpdate = statistics.getTimestamp().toLocalDate();
        // read all transactions that happened after last Zonky refresh
        final Select onlyAfterZonkyUpdate = new Select()
                .greaterThanOrEquals("transaction.transactionDate", lastZonkyUpdate);
        final IntSet newlySold = IntHashSet.newSetWith();
        tenant.call(zonky -> zonky.getTransactions(onlyAfterZonkyUpdate))
                .filter(t -> TRANSACTION_CATEGORIES.contains(t.getCategory()))
                .forEach(t -> {
                    LOGGER.debug("Processing transaction: {}.", t);
                    final boolean wasSold = processTransaction(tenant, t);
                    if (wasSold) {
                        LOGGER.debug("Transaction marked as a newly sold participation.");
                        ((IntHashSet) newlySold).add(t.getLoanId());
                    }
                });
        // read all blocked amounts that are known to us
        tenant.call(Zonky::getBlockedAmounts)
                .filter(ba -> BLOCKED_AMOUNT_CATEGORIES.contains(ba.getCategory()))
                .forEach(ba -> {
                    LOGGER.debug("Processing blocked amount: {}.", ba);
                    processBlockedAmount(tenant, ba);
                });
        LOGGER.debug("New adjustments: {}.", adjustments);
        return newlySold.toArray();
    }
}
