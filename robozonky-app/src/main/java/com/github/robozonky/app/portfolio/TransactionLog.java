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
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
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

    public TransactionLog(final Collection<Synthetic> survivingSynthetics) {
        synthetics.addAll(survivingSynthetics);
    }

    private static boolean equals(final Synthetic s, final Transaction t) {
        if (s.getLoanId() != t.getLoanId()) {
            return false;
        } else {
            return Objects.equals(s.getAmount().stripTrailingZeros(), t.getAmount().stripTrailingZeros());
        }
    }

    private static boolean equals(final Synthetic s, final BlockedAmount ba) {
        if (s.getLoanId() != ba.getLoanId()) {
            return false;
        } else {
            return Objects.equals(s.getAmount().stripTrailingZeros(), ba.getAmount().stripTrailingZeros());
        }
    }

    private static Rating getLoanRating(final Tenant tenant, final int loanId) {
        return tenant.call(z -> z.getLoan(loanId).getRating());
    }

    Collection<Synthetic> getSynthetics() {
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

    synchronized void addNewSynthetic(final Tenant tenant, final int loanId, final BigDecimal amount) {
        final Synthetic s = new Synthetic(loanId, amount, OffsetDateTime.now());
        LOGGER.debug("Adding new synthetic transaction: {}.", s);
        synthetics.add(s);
        updateRatingShare(tenant, s::getLoanId, s::getAmount);
        LOGGER.debug("New adjustments: {}.", adjustments);
    }

    public synchronized int[] update(final Statistics statistics, final Tenant tenant) {
        // all new transactions have the date at the beginning of the day when the timestamp was taken
        final LocalDate lastZonkyUpdate = statistics.getTimestamp().toLocalDate();
        // read all transactions that happened after last Zonky refresh
        final Select onlyAfterZonkyUpdate = new Select()
                .equalsPlain("transaction.transactionDate__gte",
                             DateTimeFormatter.ISO_DATE.format(lastZonkyUpdate));
        final IntSet newlySold = IntHashSet.newSetWith();
        tenant.call(zonky -> zonky.getTransactions(onlyAfterZonkyUpdate))
                .filter(t -> TRANSACTION_CATEGORIES.contains(t.getCategory()))
                .forEach(t -> {
                    final boolean isInward = t.getOrientation() == TransactionOrientation.IN;
                    final Collection<Transaction> transactions = isInward ? transactionsIn : transactionsOut;
                    final Collection<Synthetic> synthetics = isInward ? Collections.emptyList() : this.synthetics;
                    final boolean added = transactions.add(t);
                    if (added) {
                        if (t.getCategory() == TransactionCategory.SMP_SELL) {
                            ((IntHashSet) newlySold).add(t.getLoanId());
                        }
                        LOGGER.debug("New transaction: {}.", t);
                        updateRatingShare(tenant, t::getLoanId, isInward ? () -> t.getAmount().negate() : t::getAmount);
                        synthetics.removeIf(s -> equals(s, t));
                    }
                });
        // read all blocked amounts that are known to us
        tenant.call(Zonky::getBlockedAmounts)
                .filter(ba -> BLOCKED_AMOUNT_CATEGORIES.contains(ba.getCategory()))
                .forEach(ba -> {
                    final boolean added = blockedAmounts.add(ba);
                    if (added) {
                        LOGGER.debug("New blocked amount: {}.", ba);
                        updateRatingShare(tenant, ba::getLoanId, ba::getAmount);
                        synthetics.removeIf(s -> equals(s, ba));
                    }
                });
        LOGGER.debug("New adjustments: {}.", adjustments);
        return newlySold.toArray();
    }
}
