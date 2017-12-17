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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.robozonky.api.notifications.InvestmentSoldEvent;
import com.github.robozonky.api.remote.entities.BlockedAmount;
import com.github.robozonky.api.remote.entities.Investment;
import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.api.remote.enums.InvestmentStatus;
import com.github.robozonky.api.remote.enums.PaymentStatus;
import com.github.robozonky.api.remote.enums.PaymentStatuses;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.app.Events;
import com.github.robozonky.app.util.ApiUtil;
import com.github.robozonky.common.remote.Zonky;
import com.github.robozonky.internal.api.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Internal representation of the user portfolio on Zonky. Refer to {@link #create(Zonky)} as the entry point.
 */
public class Portfolio {

    private final static Logger LOGGER = LoggerFactory.getLogger(Portfolio.class);
    private final Collection<Investment> investments, investmentsPending = new ArrayList<>(0);
    private final ConcurrentMap<Integer, Loan> loanCache = new ConcurrentHashMap<>(0);

    Portfolio() {
        this(Collections.emptyList());
    }

    Portfolio(final Collection<Investment> investments) {
        this.investments = new ArrayList<>(investments);
    }

    /**
     * Return a new instance of the class, loading information about all investments present and past from the Zonky
     * interface. This operation may take a while, as there may easily be hundreds or thousands of such investments.
     * @param zonky The API to be used to retrieve the data from Zonky.
     * @return Empty in case there was a remote error.
     */
    public static Portfolio create(final Zonky zonky) {
        final Collection<Investment> online = zonky.getInvestments().collect(Collectors.toList());
        final Portfolio p = new Portfolio(online);
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

    private Investment toInvestment(final Zonky zonky, final BlockedAmount blockedAmount) {
        final Loan l = getLoan(zonky, blockedAmount.getLoanId());
        return new Investment(l, blockedAmount.getAmount().intValue());
    }

    /**
     * Whether or not a given loan is, or at any point in time was, placed on the secondary marketplace by the current
     * user and bought by someone else.
     * @param loan Loan in question.
     * @return True if the loan had been sold at least once before.
     */
    public boolean wasOnceSold(final Loan loan) {
        // first find the loan in question, then check if it's being sold or was already sold
        return getStream(investments)
                .filter(i -> i.getLoanId() == loan.getId())
                .anyMatch(i -> i.isOnSmp() || i.getStatus() == InvestmentStatus.SOLD);
    }

    /**
     * Update the internal representation of the remote portfolio by introducing a new {@link BlockedAmount}. This can
     * happen in several ways:
     * <p>
     * <ul>
     * <li>RoboZonky makes a new investment or purchase and notifies this class directly.</li>
     * <li>Periodic check of the remote portfolio status reveals a new operation made outside of RoboZonky.</li>
     * </ul>
     * @param zonky The API used to query the remote server for any extra information about the blocked amount.
     * @param blockedAmount Blocked amount to register.
     */
    public void newBlockedAmount(final Zonky zonky, final BlockedAmount blockedAmount) {
        final Predicate<Investment> equalsBlockedAmount = i -> i.getLoanId() == blockedAmount.getLoanId();
        switch (blockedAmount.getCategory()) {
            case INVESTMENT: // potential new investment detected
            case SMP_BUY: // new participation purchased
                final Investment newcomer = toInvestment(zonky, blockedAmount);
                if (investmentsPending.stream().noneMatch(equalsBlockedAmount)) {
                    investmentsPending.add(newcomer);
                }
                return;
            case SMP_SALE_FEE: // potential new participation sale detected
                // before daily update is run, the newly sold participation will show as active
                getActive()
                        .filter(equalsBlockedAmount)
                        .peek(i -> { // notify of the fact that the participation had been sold on the Zonky web
                            final int balance = zonky.getWallet().getAvailableBalance().intValue();
                            Events.fire(new InvestmentSoldEvent(i, balance));
                        })
                        .forEach(i -> {
                            i.setIsOnSmp(false);
                            i.setStatus(InvestmentStatus.SOLD);
                        });
                return;
            default: // no other notable events
                return;
        }
    }

    public Stream<Investment> getActiveWithPaymentStatus(final PaymentStatuses statuses) {
        return getActive().filter(i -> statuses.getPaymentStatuses().stream()
                .anyMatch(s -> Objects.equals(s, i.getPaymentStatus())));
    }

    /**
     * Get investments that the Zonky server will allow to be sold, which are not already present on the secondary
     * marketplace.
     * @return
     */
    public Stream<Investment> getActiveForSecondaryMarketplace() {
        return getActive()
                .filter(Investment::isCanBeOffered)
                .filter(i -> !i.isInWithdrawal())
                .filter(i -> !i.isOnSmp());
    }

    public Stream<Investment> getActive() {
        return getStream(investments, s -> s.filter((Investment i) -> i.getStatus() == InvestmentStatus.ACTIVE));
    }

    public Stream<Investment> getPending() {
        return getStream(investmentsPending);
    }

    /**
     * Summarize portfolio data, knowing the available balance.
     * @param balance Balance available in the Zonky account.
     * @return Summary portfolio data.
     */
    public PortfolioOverview calculateOverview(final BigDecimal balance) {
        final Stream<Investment> allInvestment =
                Stream.concat(getActiveWithPaymentStatus(PaymentStatus.getActive()), getPending());
        return PortfolioOverview.calculate(balance, allInvestment);
    }

    /**
     * Summarize portfolio data, retrieving the balance from remote server.
     * @param zonky API to use to retrieve the data.
     * @param isDryRun Whether or not to use {@link Settings.Key#DEFAULTS_DRY_RUN_BALANCE}.
     * @return Summary portfolio data.
     */
    public PortfolioOverview calculateOverview(final Zonky zonky, final boolean isDryRun) {
        final BigDecimal balance = isDryRun ? ApiUtil.getDryRunBalance(zonky) : ApiUtil.getLiveBalance(zonky);
        return calculateOverview(balance);
    }

    /**
     * Retrieve a full Loan instance from local cache, falling back to the server if not already present.
     * @param zonky API to use to retrieve the data.
     * @param loanId ID of the loan to retrieve.
     * @return Copy of the loan data at time of retrieval.
     */
    public Loan getLoan(final Zonky zonky, final int loanId) {
        return loanCache.compute(loanId, (key, value) -> {
            if (value != null) {
                return value;
            }
            return zonky.getLoan(loanId);
        });
    }

    /**
     * Retrieve a full Loan instance from local cache.
     * @param loanId ID of the loan to retrieve.
     * @return Empty if loan not present in local cache.
     */
    public Optional<Loan> getLoan(final int loanId) {
        return Optional.ofNullable(loanCache.get(loanId));
    }
}
