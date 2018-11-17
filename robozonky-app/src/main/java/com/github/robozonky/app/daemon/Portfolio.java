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

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import com.github.robozonky.api.remote.entities.Statistics;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.common.Tenant;
import com.github.robozonky.common.remote.Zonky;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Portfolio {

    private static final Logger LOGGER = LoggerFactory.getLogger(Portfolio.class);

    private final AtomicReference<PortfolioOverview> portfolioOverview = new AtomicReference<>();
    private final AtomicReference<Map<Rating, BigDecimal>> amountsAtRisk = new AtomicReference<>(
            Collections.emptyMap());
    private final Statistics statistics;
    private final Tenant tenant;
    private final Supplier<BlockedAmountProcessor> blockedAmounts;

    private Portfolio(final Supplier<BlockedAmountProcessor> blockedAmounts, final Tenant tenant) {
        this.blockedAmounts = blockedAmounts;
        this.tenant = tenant;
        this.statistics = tenant.call(Zonky::getStatistics);
    }

    public static Portfolio create(final Tenant tenant, final Supplier<BlockedAmountProcessor> transfers) {
        return new Portfolio(transfers, tenant);
    }

    public void simulateCharge(final int loanId, final Rating rating, final BigDecimal amount) {
        LOGGER.debug("Simulating charge for loan #{} ({}), {} CZK.", loanId, rating, amount);
        blockedAmounts.get().simulateCharge(loanId, rating, amount);
        tenant.getBalance().update(amount.negate(), !tenant.getSessionInfo().isDryRun());
    }

    public void amountsAtRiskUpdated(final Map<Rating, BigDecimal> newAmountsAtRisk) {
        LOGGER.debug("New amounts at risk: {}.", newAmountsAtRisk);
        amountsAtRisk.set(newAmountsAtRisk);
        portfolioOverview.set(null);
    }

    public PortfolioOverview getOverview() {
        return portfolioOverview.updateAndGet(old -> {
            if (old == null) {
                final PortfolioOverview current = PortfolioOverviewImpl.calculate(tenant.getBalance(), statistics,
                                                                                  blockedAmounts.get().getAdjustments(),
                                                                                  amountsAtRisk.get());
                LOGGER.debug("Calculated: {}.", current);
                return current;
            }
            return old;
        });
    }
}
