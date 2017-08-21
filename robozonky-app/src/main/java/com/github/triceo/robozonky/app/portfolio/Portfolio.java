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

package com.github.triceo.robozonky.app.portfolio;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.triceo.robozonky.api.notifications.InvestmentSoldEvent;
import com.github.triceo.robozonky.api.remote.entities.BlockedAmount;
import com.github.triceo.robozonky.api.remote.entities.Investment;
import com.github.triceo.robozonky.api.remote.entities.Loan;
import com.github.triceo.robozonky.api.remote.enums.InvestmentStatus;
import com.github.triceo.robozonky.api.remote.enums.PaymentStatus;
import com.github.triceo.robozonky.api.remote.enums.PaymentStatuses;
import com.github.triceo.robozonky.api.strategies.PortfolioOverview;
import com.github.triceo.robozonky.app.Events;
import com.github.triceo.robozonky.app.util.ApiUtil;
import com.github.triceo.robozonky.app.util.DaemonRuntimeExceptionHandler;
import com.github.triceo.robozonky.common.remote.Zonky;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum Portfolio {

    INSTANCE;

    private static final Logger LOGGER = LoggerFactory.getLogger(Portfolio.class);
    private final AtomicReference<List<Investment>> investments = new AtomicReference<>(Collections.emptyList()),
            investmentsPending = new AtomicReference<>(Collections.emptyList());
    private final AtomicReference<SortedMap<Integer, Loan>> loanCache = new AtomicReference<>(initSortedMap());
    private final Set<Consumer<Zonky>> updaters = new CopyOnWriteArraySet<>();
    private final AtomicBoolean ranOnce = new AtomicBoolean(false), isUpdating = new AtomicBoolean(false);

    private static <R, S> SortedMap<R, S> initSortedMap() {
        return new ConcurrentSkipListMap<>();
    }

    private Investment toInvestment(final Zonky zonky, final BlockedAmount blockedAmount) {
        final Loan l = getLoan(zonky, blockedAmount.getLoanId());
        return new Investment(l, blockedAmount.getAmount().intValue());
    }

    public void registerUpdater(final Consumer<Zonky> updater) {
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
                loanCache.set(initSortedMap());
                // read all investments from Zonky
                investments.set(zonky.getInvestments().collect(Collectors.toList()));
                // and make updates based on which have been recently made or sold
                newBlockedAmounts(zonky, new TreeSet<>(zonky.getBlockedAmounts().collect(Collectors.toSet())));
                updaters.forEach((u) -> {
                    LOGGER.trace("Running dependent: {}.", u);
                    u.accept(zonky);
                });
                LOGGER.trace("Finished.");
            } catch (final Throwable t) { // users should know
                new DaemonRuntimeExceptionHandler().handle(t);
            } finally {
                this.isUpdating.set(false);
                this.ranOnce.set(true);
            }
        }
    }

    public void newBlockedAmounts(final Zonky zonky, final SortedSet<BlockedAmount> blockedAmounts) {
        for (final BlockedAmount ba : blockedAmounts) { // need to "replay" operations as they come in
            newBlockedAmount(zonky, ba);
        }
    }

    public void newBlockedAmount(final Zonky zonky, final BlockedAmount blockedAmount) {
        switch (blockedAmount.getCategory()) {
            case INVESTMENT: // potential new investment detected
            case SMP_BUY: // new participation purchase notified from within RoboZonky
                investmentsPending.updateAndGet(pending -> {
                    if (pending.stream().noneMatch(i -> i.getLoanId() == blockedAmount.getLoanId())) {
                        final List<Investment> result = new ArrayList<>(investmentsPending.get());
                        result.add(toInvestment(zonky, blockedAmount));
                        return result;
                    } else {
                        return pending;
                    }
                });
                return;
            case SMP_SALE_FEE: // potential new participation sale detected
                // before daily update is run, the newly sold participation will show as active
                getActive()
                        .filter(i -> i.getLoanId() == blockedAmount.getLoanId())
                        .peek(i -> {
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
        return investments.get().stream().filter(i -> i.getStatus() == InvestmentStatus.ACTIVE);
    }

    public Stream<Investment> getPending() {
        return investmentsPending.get().stream();
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

    /**
     * For test purposes only.
     */
    public void reset() {
        investmentsPending.set(Collections.emptyList());
        investments.set(Collections.emptyList());
        loanCache.set(initSortedMap());
        updaters.clear();
        ranOnce.set(false);
    }

}
