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

package com.github.robozonky.app.daemon;

import java.io.Closeable;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;

import com.github.robozonky.api.remote.entities.Statistics;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.common.Tenant;
import com.github.robozonky.common.remote.Zonky;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Portfolio implements Closeable {

    private static final Logger LOGGER = LoggerFactory.getLogger(Portfolio.class);

    private final AtomicReference<PortfolioOverview> portfolioOverview = new AtomicReference<>();
    private final AtomicReference<Map<Rating, BigDecimal>> amountsAtRisk = new AtomicReference<>(
            Collections.emptyMap());
    private final Statistics statistics;
    private final RemoteBalance balance;
    private final Supplier<BlockedAmountProcessor> blockedAmounts;

    private Portfolio(final Supplier<BlockedAmountProcessor> blockedAmounts, final Statistics statistics,
                      final Function<Portfolio, RemoteBalance> balance) {
        this.blockedAmounts = blockedAmounts;
        this.statistics = statistics;
        this.balance = balance.apply(this);
    }

    public static Portfolio create(final Tenant tenant, final Supplier<BlockedAmountProcessor> transfers) {
        return create(tenant, transfers, p -> RemoteBalance.create(tenant, p::balanceUpdated));
    }

    public static Portfolio create(final Tenant tenant, final Supplier<BlockedAmountProcessor> transfers,
                                   final Function<Portfolio, RemoteBalance> balance) {
        return new Portfolio(transfers, tenant.call(Zonky::getStatistics), balance);
    }

    public void simulateCharge(final int loanId, final Rating rating, final BigDecimal amount) {
        blockedAmounts.get().simulateCharge(loanId, rating, amount);
        balance.update(amount.negate()); // will result in balanceUpdated() getting called
    }

    public void balanceUpdated(final BigDecimal newBalance) {
        LOGGER.debug("New balance: {} CZK.", newBalance);
        portfolioOverview.set(null); // reset overview, so that it could be recalculated on-demand
    }

    public void amountsAtRiskUpdated(final Map<Rating, BigDecimal> newAmountsAtRisk) {
        LOGGER.debug("New amounts at risk: {}.", newAmountsAtRisk);
        amountsAtRisk.set(newAmountsAtRisk);
        portfolioOverview.set(null);
    }

    public RemoteBalance getRemoteBalance() {
        return balance;
    }

    public PortfolioOverview getOverview() {
        return portfolioOverview.updateAndGet(old -> {
            if (old == null) {
                final PortfolioOverview current = PortfolioOverviewImpl.calculate(balance.get(), statistics,
                                                                                  blockedAmounts.get().getAdjustments(),
                                                                                  amountsAtRisk.get());
                LOGGER.debug("Calculated: {}.", current);
                return current;
            }
            return old;
        });
    }

    @Override
    public void close() throws IOException {
        balance.close();
    }
}
