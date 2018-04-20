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

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import com.github.robozonky.api.notifications.InvestmentSoldEvent;
import com.github.robozonky.api.remote.entities.BlockedAmount;
import com.github.robozonky.api.remote.entities.Statistics;
import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.app.Events;
import com.github.robozonky.app.authentication.Authenticated;
import com.github.robozonky.app.util.LoanCache;
import com.github.robozonky.common.remote.Select;
import com.github.robozonky.common.remote.Zonky;
import org.eclipse.collections.api.set.primitive.MutableIntSet;
import org.eclipse.collections.impl.set.mutable.primitive.IntHashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Portfolio {

    private final static Logger LOGGER = LoggerFactory.getLogger(Portfolio.class);
    private final Statistics statistics;
    private final Map<Rating, BigDecimal> blockedAmountsBalance = new EnumMap<>(Rating.class);
    private final MutableIntSet loansSold;
    private final RemoteBalance balance;
    private final AtomicReference<Collection<BlockedAmount>> blockedAmounts =
            new AtomicReference<>(Collections.emptyList());

    Portfolio(final RemoteBalance balance) {
        this(Statistics.empty(), new int[0], balance);
    }

    Portfolio(final Statistics statistics, final int[] idsOfSoldLoans,
              final RemoteBalance balance) {
        this.statistics = statistics;
        this.loansSold = IntHashSet.newSetWith(idsOfSoldLoans);
        this.balance = balance;
    }

    /**
     * Return a new instance of the class, loading information about all investments present and past from the Zonky
     * interface. This operation may take a while, as there may easily be hundreds or thousands of such investments.
     * @param auth The API to be used to retrieve the data from Zonky.
     * @param balance Tracker for the presently available balance.
     * @return Empty in case there was a remote error.
     */
    public static Portfolio create(final Authenticated auth, final RemoteBalance balance) {
        final int[] sold = auth.call(zonky -> zonky.getInvestments(new Select().equals("status", "SOLD")))
                .mapToInt(Investment::getLoanId)
                .distinct()
                .toArray();
        return new Portfolio(auth.call(Zonky::getStatistics), sold, balance);
    }

    /**
     * Whether or not a given loan is, or at any point in time was, placed on the secondary marketplace by the current
     * user and bought by someone else.
     * @param loanId Loan in question.
     * @return True if the loan had been sold at least once before.
     */
    public boolean wasOnceSold(final int loanId) {
        return loansSold.contains(loanId);
    }

    private Optional<Investment> lookup(final Loan loan, final Authenticated auth) {
        return loan.getMyInvestment().flatMap(i -> auth.call(zonky -> zonky.getInvestment(i.getId())));
    }

    Investment lookupOrFail(final Loan loan, final Authenticated auth) {
        return lookup(loan, auth)
                .orElseThrow(() -> new IllegalStateException("Investment not found for loan " + loan.getId()));
    }

    public void updateBlockedAmounts(final Authenticated auth) {
        final Collection<BlockedAmount> presentBlockedAmounts =
                auth.call(zonky -> zonky.getBlockedAmounts().collect(Collectors.toList()));
        final Collection<BlockedAmount> previousBlockedAmounts = blockedAmounts.getAndSet(presentBlockedAmounts);
        presentBlockedAmounts.stream()
                .filter(ba -> !previousBlockedAmounts.contains(ba))
                .forEach(ba -> newBlockedAmount(auth, ba));
    }

    /**
     * Update the internal representation of the remote portfolio by introducing a new {@link BlockedAmount}. This can
     * happen in several ways:
     *
     * <ul>
     * <li>RoboZonky makes a new investment or purchase and notifies this class directly.</li>
     * <li>Periodic check of the remote portfolio status reveals a new operation made outside of RoboZonky.</li>
     * </ul>
     * @param auth The API used to query the remote server for any extra information about the blocked amount.
     * @param blockedAmount Blocked amount to register.
     */
    void newBlockedAmount(final Authenticated auth, final BlockedAmount blockedAmount) {
        LOGGER.debug("Processing blocked amount: #{}.", blockedAmount);
        final int loanId = blockedAmount.getLoanId();
        final Loan l = auth.call(zonky -> LoanCache.INSTANCE.getLoan(loanId, zonky));
        switch (blockedAmount.getCategory()) {
            case INVESTMENT: // potential new investment detected
            case SMP_BUY: // new participation purchased
                blockedAmountsBalance.compute(l.getRating(), (r, old) -> {
                    final BigDecimal start = old == null ? BigDecimal.ZERO : old;
                    return start.add(blockedAmount.getAmount());
                });
                LOGGER.debug("Registered a new investment to loan #{}.", loanId);
                return;
            case SMP_SALE_FEE: // potential new participation sale detected
                final Investment i = lookupOrFail(l, auth);
                blockedAmountsBalance.compute(l.getRating(), (r, old) -> {
                    final BigDecimal start = old == null ? BigDecimal.ZERO : old;
                    return start.subtract(i.getRemainingPrincipal());
                });
                // notify of the fact that the participation had been sold on the Zonky web
                final PortfolioOverview po = calculateOverview();
                Events.fire(new InvestmentSoldEvent(i, l, po));
                return;
            default: // no other notable events
                return;
        }
    }

    public RemoteBalance getRemoteBalance() {
        return balance;
    }

    public PortfolioOverview calculateOverview() {
        return PortfolioOverview.calculate(getRemoteBalance().get(), statistics, blockedAmountsBalance,
                                           Delinquents.getAmountsAtRisk());
    }
}
