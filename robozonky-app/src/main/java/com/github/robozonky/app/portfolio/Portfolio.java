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
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.robozonky.api.notifications.InvestmentSoldEvent;
import com.github.robozonky.api.remote.entities.BlockedAmount;
import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.remote.enums.InvestmentStatus;
import com.github.robozonky.api.remote.enums.PaymentStatus;
import com.github.robozonky.api.remote.enums.PaymentStatuses;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.app.Events;
import com.github.robozonky.app.authentication.Authenticated;
import com.github.robozonky.app.util.LoanCache;
import com.github.robozonky.common.remote.Select;
import com.github.robozonky.common.remote.Zonky;
import org.eclipse.collections.api.set.primitive.MutableIntSet;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.set.mutable.primitive.IntHashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Internal representation of the user portfolio on Zonky. Refer to {@link #create(Zonky, RemoteBalance)} as the
 * entry point.
 */
public class Portfolio {

    private final static Logger LOGGER = LoggerFactory.getLogger(Portfolio.class);
    private final Collection<Investment> investments, investmentsPending = new FastList<>(0);
    private final Map<Rating, BigDecimal> blockedAmountsBalance = new EnumMap<>(Rating.class);
    private final MutableIntSet loansSold;
    private final RemoteBalance balance;

    Portfolio(final RemoteBalance balance) {
        this(Collections.emptyList(), new int[0], balance);
    }

    Portfolio(final Collection<Investment> investments, final int[] idsOfSoldLoans, final RemoteBalance balance) {
        this.investments = new FastList<>(investments);
        this.loansSold = IntHashSet.newSetWith(idsOfSoldLoans);
        this.balance = balance;
    }

    /**
     * Return a new instance of the class, loading information about all investments present and past from the Zonky
     * interface. This operation may take a while, as there may easily be hundreds or thousands of such investments.
     * @param zonky The API to be used to retrieve the data from Zonky.
     * @param balance Tracker for the presently available balance.
     * @return Empty in case there was a remote error.
     */
    public static Portfolio create(final Zonky zonky, final RemoteBalance balance) {
        final Collection<Investment> online = zonky.getInvestments().parallel().collect(Collectors.toList());
        LOGGER.debug("Loading sold investments from Zonky.");
        final int[] sold = zonky.getInvestments(new Select().equals("status", "SOLD"))
                .mapToInt(Investment::getLoanId)
                .distinct()
                .toArray();
        final Portfolio p = new Portfolio(online, sold, balance);
        LOGGER.debug("Loaded {} investments from Zonky.", online.size());
        return p;
    }

    private static <T> Stream<T> getStream(final Collection<T> source, final Function<Stream<T>, Stream<T>> modifier) {
        if (source == null || source.isEmpty()) {
            return Stream.empty();
        } else {
            return modifier.apply(source.stream());
        }
    }

    private static <T> Stream<T> getStream(final Collection<T> source) {
        return getStream(source, Function.identity());
    }

    private static boolean isLoanRelated(final Investment i, final int loanId) {
        return i.getLoanId() == loanId;
    }

    private static boolean isLoanRelated(final Investment i, final BlockedAmount blockedAmount) {
        return isLoanRelated(i, blockedAmount.getLoanId());
    }

    private static Investment toInvestment(final Authenticated authenticated, final BlockedAmount blockedAmount) {
        final Loan l = authenticated.call(zonky -> LoanCache.INSTANCE.getLoan(blockedAmount.getLoanId(), zonky));
        return Investment.fresh(l, blockedAmount.getAmount());
    }

    /**
     * Whether or not a given loan is, or at any point in time was, placed on the secondary marketplace by the current
     * user and bought by someone else.
     * @param loan Loan in question.
     * @return True if the loan had been sold at least once before.
     */
    public boolean wasOnceSold(final Loan loan) {
        return loansSold.contains(loan.getId());
    }

    Optional<Investment> lookup(final Loan loan, final Authenticated auth) {
        return loan.getMyInvestment().flatMap(i -> auth.call(zonky -> zonky.getInvestment(i.getId())));
    }

    Investment lookupOrFail(final Loan loan, final Authenticated auth) {
        return lookup(loan, auth)
                .orElseThrow(() -> new IllegalStateException("Investment not found for loan " + loan.getId()));
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
    public void newBlockedAmount(final Authenticated auth, final BlockedAmount blockedAmount) {
        switch (blockedAmount.getCategory()) {
            case INVESTMENT: // potential new investment detected
            case SMP_BUY: // new participation purchased
                final Investment newcomer = toInvestment(auth, blockedAmount);
                if (investmentsPending.stream().noneMatch(i -> isLoanRelated(i, blockedAmount))) {
                    investmentsPending.add(newcomer);
                }
                return;
            case SMP_SALE_FEE: // potential new participation sale detected
                final Loan l = auth.call(zonky -> LoanCache.INSTANCE.getLoan(blockedAmount.getLoanId(), zonky));
                final Investment i = lookupOrFail(l, auth);
                blockedAmountsBalance.compute(l.getRating(), (r, old) -> {
                    final BigDecimal start = old == null ? BigDecimal.ZERO : old;
                    return start.subtract(i.getRemainingPrincipal());
                });
                loansSold.add(l.getId());
                // notify of the fact that the participation had been sold on the Zonky web
                final PortfolioOverview po = calculateOverview();
                Events.fire(new InvestmentSoldEvent(i, l, po));
                return;
            default: // no other notable events
                return;
        }
    }

    private Stream<Investment> getActiveWithPaymentStatus(final PaymentStatuses statuses) {
        return getActive().filter(i -> statuses.getPaymentStatuses().stream()
                .anyMatch(s -> Objects.equals(s, i.getPaymentStatus().orElse(null))));
    }

    private Stream<Investment> getActive() {
        return getStream(investments, s -> s.filter((Investment i) -> i.getStatus() == InvestmentStatus.ACTIVE));
    }

    public Stream<Investment> getPending() {
        return getStream(investmentsPending);
    }

    public RemoteBalance getRemoteBalance() {
        return balance;
    }

    public PortfolioOverview calculateOverview() {
        final Supplier<Stream<Investment>> investments =
                () -> Stream.concat(getActiveWithPaymentStatus(PaymentStatus.getActive()), getPending());
        return PortfolioOverview.calculate(getRemoteBalance().get(), investments);
    }
}
