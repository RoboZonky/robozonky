/*
 * Copyright 2017 Lukáš Petrovický
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.triceo.robozonky.api.remote.entities.Investment;
import com.github.triceo.robozonky.api.remote.entities.Loan;
import com.github.triceo.robozonky.api.remote.entities.Statistics;
import com.github.triceo.robozonky.api.remote.enums.InvestmentStatus;
import com.github.triceo.robozonky.api.remote.enums.PaymentStatus;
import com.github.triceo.robozonky.api.remote.enums.PaymentStatuses;
import com.github.triceo.robozonky.api.strategies.PortfolioOverview;
import com.github.triceo.robozonky.app.util.DaemonRuntimeExceptionHandler;
import com.github.triceo.robozonky.common.remote.Zonky;
import com.github.triceo.robozonky.internal.api.Retriever;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum Portfolio {

    INSTANCE;

    private static final Logger LOGGER = LoggerFactory.getLogger(Investment.class);
    private final AtomicReference<List<Investment>> investments = new AtomicReference<>(Collections.emptyList()),
            investmentsPending = new AtomicReference<>(Collections.emptyList());
    private final AtomicReference<SortedMap<Integer, Loan>> loanCache = new AtomicReference<>(
            Collections.emptySortedMap());
    private final Map<Consumer<Zonky>, Portfolio.UpdateType> updaters = new ConcurrentHashMap<>();
    private final AtomicBoolean isUpdating = new AtomicBoolean(false);

    public void registerUpdater(final Consumer<Zonky> updater) {
        registerUpdater(updater, Portfolio.UpdateType.FULL);
    }

    public void registerUpdater(final Consumer<Zonky> updater, final Portfolio.UpdateType updateType) {
        updaters.put(updater, updateType);
    }

    public void update(final Zonky zonky, final Portfolio.UpdateType updateType) {
        if (this.isUpdating.getAndSet(true)) {
            LOGGER.trace("Update ignored due to already being updated.");
            return;
        } else try {
            LOGGER.trace("Update started: {}.", updateType);
            if (updateType == Portfolio.UpdateType.FULL) {
                loanCache.set(new TreeMap<>());
                investments.set(zonky.getInvestments().collect(Collectors.toList()));
            }
            investmentsPending.set(Util.retrieveInvestmentsRepresentedByBlockedAmounts(zonky));
            updaters.forEach((u, requiredType) -> {
                if (requiredType == Portfolio.UpdateType.FULL && updateType == Portfolio.UpdateType.PARTIAL) {
                    return;
                }
                LOGGER.trace("Running dependent: {}", u);
                u.accept(zonky);
            });
            LOGGER.trace("Finished.");
        } catch (final Throwable t) { // users should know
            new DaemonRuntimeExceptionHandler().handle(t);
        } finally {
            this.isUpdating.set(false);
        }
    }

    public boolean isUpdating() {
        return this.isUpdating.get();
    }


    public void update(final Zonky zonky) {
        update(zonky, Portfolio.UpdateType.FULL);
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

    public PortfolioOverview calculateOverview(final Zonky zonky, final BigDecimal balance) {
        final Stream<Investment> allInvestment = Stream.concat(getActive(), getPending());
        final Statistics stats = zonky.getStatistics();
        return PortfolioOverview.calculate(balance, stats, allInvestment);
    }

    public PortfolioOverview calculateOverview(final Zonky zonky) {
        return calculateOverview(zonky, zonky.getWallet().getAvailableBalance());
    }

    public Loan getLoan(final Zonky zonky, final int loanId) {
        return loanCache.get().compute(loanId, (key, value) -> {
            if (value != null) {
                return value;
            }
            return Retriever.retrieve(() -> Optional.of(zonky.getLoan(key)))
                    .orElseThrow(() -> new IllegalStateException("Loan retrieval failed."));
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
        loanCache.set(new TreeMap<>());
        updaters.clear();
    }

    public enum UpdateType {

        FULL,
        PARTIAL

    }

}
