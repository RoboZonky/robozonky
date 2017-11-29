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
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum Portfolio {

    INSTANCE;

    private final Logger LOGGER = LoggerFactory.getLogger(Portfolio.class);
    private final AtomicReference<Collection<Investment>> investments = new AtomicReference<>(),
            investmentsPending = new AtomicReference<>();
    private final AtomicReference<SortedMap<Integer, Loan>> loanCache = new AtomicReference<>();
    /**
     * Never iterate over directly, always use {@link #getUpdaters()}.
     */
    private final Set<PortfolioBased> updaters = new CopyOnWriteArraySet<>();
    private final AtomicBoolean ranOnce = new AtomicBoolean(false), isUpdating = new AtomicBoolean(false);

    Portfolio() {
        reset();
    }

    private static <T> Stream<T> getStream(final AtomicReference<Collection<T>> source,
                                           final Function<Stream<T>, Stream<T>> modifier) {
        if (source.get() == null) {
            return Stream.empty();
        } else {
            return modifier.apply(source.get().stream());
        }
    }

    private static <T> Stream<T> getStream(final AtomicReference<Collection<T>> source) {
        return getStream(source, Function.identity());
    }

    private Investment toInvestment(final Zonky zonky, final BlockedAmount blockedAmount) {
        final Loan l = getLoan(zonky, blockedAmount.getLoanId());
        return new Investment(l, blockedAmount.getAmount().intValue());
    }

    private Stream<PortfolioBased> getUpdaters() {
        /*
         * core updaters to get the full picture. checks for blocked amounts immediately after every update of the
         * portfolio, also checks for delinquencies.
         */
        final Stream<PortfolioBased> core = Stream.of(BlockedAmounts.INSTANCE, Delinquents.INSTANCE);
        final Stream<PortfolioBased> external = updaters.stream();
        return Stream.concat(core, external);
    }

    public void registerUpdater(final PortfolioBased updater) {
        LOGGER.debug("Registering dependent: {}.", updater);
        updaters.add(updater);
    }

    public void update(final Zonky zonky) {
        if (this.isUpdating.getAndSet(true)) {
            LOGGER.trace("Update ignored due to already being updated.");
            return;
        } else {
            try {
                LOGGER.trace("Started.");
                init();
                final Collection<Investment> online = zonky.getInvestments().collect(Collectors.toList());
                investments.set(online);
                LOGGER.debug("Loaded {} investments from Zonky.", online.size());
                getUpdaters().forEach((u) -> {
                    LOGGER.trace("Running dependent: {}.", u);
                    u.accept(this, zonky);
                });
                LOGGER.trace("Finished.");
                this.ranOnce.set(true);
            } finally { // never end if a intermediate state
                this.isUpdating.set(false);
            }
        }
    }

    public void newBlockedAmounts(final Zonky zonky, final SortedSet<BlockedAmount> blockedAmounts) {
        blockedAmounts.forEach(ba -> newBlockedAmount(zonky, ba));
    }

    public boolean wasOnceSold(final Loan loan) {
        // first find the loan in question, then check if it's being sold or was already sold
        return getStream(investments)
                .filter(i -> i.getLoanId() == loan.getId())
                .anyMatch(i -> i.isOnSmp() || i.getStatus() == InvestmentStatus.SOLD);
    }

    public void newBlockedAmount(final Zonky zonky, final BlockedAmount blockedAmount) {
        final Predicate<Investment> equalsBlockedAmount = i -> i.getLoanId() == blockedAmount.getLoanId();
        switch (blockedAmount.getCategory()) {
            case INVESTMENT: // potential new investment detected
            case SMP_BUY: // new participation purchase notified from within RoboZonky
                investmentsPending.updateAndGet(pending -> {
                    final Investment newcomer = toInvestment(zonky, blockedAmount);
                    if (pending == null) {
                        return new ArrayList<>(Collections.singleton(newcomer)); // must be a modifiable collection...
                    }
                    if (pending.stream().noneMatch(equalsBlockedAmount)) {
                        pending.add(newcomer); // ... because here it gets modified
                    }
                    return pending;
                });
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

    public boolean isUpdating() {
        return !this.ranOnce.get() || this.isUpdating.get();
    }

    public Stream<Investment> getActiveWithPaymentStatus(final Set<PaymentStatus> statuses) {
        return getActive().filter(i -> statuses.stream().anyMatch(s -> Objects.equals(s, i.getPaymentStatus())));
    }

    public Stream<Investment> getActiveWithPaymentStatus(final PaymentStatuses statuses) {
        return getActiveWithPaymentStatus(statuses.getPaymentStatuses());
    }

    public Stream<Investment> getActiveForSecondaryMarketplace() {
        return getActive().filter(Investment::isCanBeOffered).filter(i -> !i.isOnSmp());
    }

    public Stream<Investment> getActive() {
        return getStream(investments, s -> s.filter((Investment i) -> i.getStatus() == InvestmentStatus.ACTIVE));
    }

    public Stream<Investment> getPending() {
        return getStream(investmentsPending);
    }

    public PortfolioOverview calculateOverview(final BigDecimal balance) {
        final Stream<Investment> allInvestment =
                Stream.concat(getActiveWithPaymentStatus(PaymentStatus.getActive()), getPending());
        return PortfolioOverview.calculate(balance, allInvestment);
    }

    public PortfolioOverview calculateOverview(final Zonky zonky, final boolean isDryRun) {
        final BigDecimal balance = isDryRun ? ApiUtil.getDryRunBalance(zonky) : ApiUtil.getLiveBalance(zonky);
        return calculateOverview(balance);
    }

    public Loan getLoan(final Zonky zonky, final int loanId) {
        return loanCache.get().compute(loanId, (key, value) -> {
            if (value != null) {
                return value;
            }
            return zonky.getLoan(loanId);
        });
    }

    public Optional<Loan> getLoan(final int loanId) {
        return Optional.ofNullable(loanCache.get().get(loanId));
    }

    private void init() {
        investments.set(null);
        investmentsPending.set(null);
        loanCache.set(new TreeMap<>());
    }

    /**
     * For test purposes only.
     */
    public void reset() {
        init();
        updaters.clear();
        ranOnce.set(false);
        isUpdating.set(false);
    }

}
