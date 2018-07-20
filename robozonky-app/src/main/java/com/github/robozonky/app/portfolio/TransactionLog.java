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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

import com.github.robozonky.api.remote.entities.BlockedAmount;
import com.github.robozonky.api.remote.entities.Statistics;
import com.github.robozonky.api.remote.entities.Transaction;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.remote.enums.TransactionCategory;
import com.github.robozonky.api.remote.enums.TransactionOrientation;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.app.authentication.Tenant;
import com.github.robozonky.app.util.LoanCache;
import com.github.robozonky.common.remote.Select;
import com.github.robozonky.common.remote.Zonky;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Zonky update their portfolio data once per day at 3am. This means that {@link Statistics} will only ever be
 * up-to-date at that point in time and any operations performed after will not be reflrected in the rating shares of
 * {@link PortfolioOverview}.
 * <p>
 * Since it is absolutely imperative for the robot to have up-to-date statistics, this class will load
 * {@link Transaction}s and {@link BlockedAmount}s from Zonky to re-create statistics based on the latest available
 * information. Call {@link #update(Statistics, Tenant)} to put the instance into the most up-to-date state.
 * <p>
 * Portfolio-altering robot operations, such as investing or purchasing, need to call
 * {@link #addNewSynthetic(Tenant, int, BigDecimal)} to let the robot know to update the rating shares etc. This will
 * add a new {@link Synthetic} instance to the internal tracker, which may later be replaced by an actual
 * {@link Transaction} or {@link BlockedAmount} when {@link #update(Statistics, Tenant)} is called.
 * <p>
 * {@link Synthetic}s are also important for dry runs and as such, they need to be able to survive between multiple
 * subsequent instances of {@link TransactionLog}. This is accomplished using
 * {@link #TransactionLog(Tenant, Collection)}. If you're creating a fresh transaction log, using
 * {@link #TransactionLog()} will suffice.
 */
class TransactionLog {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionLog.class);
    private static final Collection<TransactionCategory> TRANSACTION_CATEGORIES =
            Arrays.asList(TransactionCategory.SMP_BUY, TransactionCategory.SMP_SELL), BLOCKED_AMOUNT_CATEGORIES =
            Arrays.asList(TransactionCategory.INVESTMENT);

    private final Map<Rating, BigDecimal> adjustments = new EnumMap<>(Rating.class);
    private final Collection<BlockedAmount> blockedAmounts = new HashSet<>(0);
    private final Collection<Transaction> transactionsIn = new HashSet<>(0), transactionsOut = new HashSet<>(0);
    private final Collection<Synthetic> synthetics = new HashSet<>(0);

    public TransactionLog() {

    }

    public TransactionLog(final Tenant tenant, final Collection<Synthetic> survivingSynthetics) {
        LOGGER.debug("Survived synthetic transactions: {}", survivingSynthetics);
        survivingSynthetics.forEach(s -> addNewSynthetic(tenant, s.getLoanId(), s.getAmount()));
    }

    private static Rating getLoanRating(final Tenant tenant, final int loanId) {
        return LoanCache.INSTANCE.getLoan(loanId, tenant).getRating();
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

    /**
     * Represents the difference in rating shares, compared to the last time Zonky system update was performed.
     * @return
     */
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
        final Collection<Synthetic> syntheticTransactions = isInward ? Collections.emptyList() : this.synthetics;
        final boolean unseenTransaction = transactions.add(t);
        if (!unseenTransaction) { // already processed this; don't do anything
            return false;
        }
        final boolean hadCorrespondingSynthetic = syntheticTransactions.removeIf(s -> Synthetic.equals(s, t));
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

    public synchronized Set<Integer> update(final Statistics statistics, final Tenant tenant) {
        LOGGER.debug("Pre-update adjustments: {}.", adjustments);
        // all new transactions have the date at the beginning of the day when the timestamp was taken
        final LocalDate lastZonkyUpdate = statistics.getTimestamp().toLocalDate();
        // read all transactions that happened after last Zonky refresh
        final Select onlyAfterZonkyUpdate = new Select()
                .greaterThanOrEquals("transaction.transactionDate", lastZonkyUpdate);
        final Set<Integer> newlySold = new HashSet<>(0);
        tenant.call(zonky -> zonky.getTransactions(onlyAfterZonkyUpdate))
                .filter(t -> TRANSACTION_CATEGORIES.contains(t.getCategory()))
                .forEach(t -> {
                    LOGGER.debug("Processing transaction: {}.", t);
                    final boolean wasSold = processTransaction(tenant, t);
                    if (wasSold) {
                        LOGGER.debug("Transaction marked as a newly sold participation.");
                        newlySold.add(t.getLoanId());
                    }
                });
        // read all blocked amounts that are known to us
        tenant.call(Zonky::getBlockedAmounts)
                .filter(ba -> BLOCKED_AMOUNT_CATEGORIES.contains(ba.getCategory()))
                .forEach(ba -> {
                    LOGGER.debug("Processing blocked amount: {}.", ba);
                    processBlockedAmount(tenant, ba);
                });
        LOGGER.debug("Post-update adjustments: {}.", adjustments);
        return Collections.unmodifiableSet(newlySold);
    }
}
