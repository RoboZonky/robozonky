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
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import com.github.robozonky.api.remote.entities.Statistics;
import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.common.Tenant;
import com.github.robozonky.common.remote.Zonky;
import com.github.robozonky.internal.util.BigDecimalCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.reducing;

public class Portfolio {

    private static final Logger LOGGER = LoggerFactory.getLogger(Portfolio.class);

    private final AtomicReference<PortfolioOverview> portfolioOverview = new AtomicReference<>();
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

    private static Map<Rating, BigDecimal> getAmountsAtRisk(final Tenant tenant) {
        return tenant.call(Zonky::getDelinquentInvestments)
                .parallel() // possibly many pages' worth of results; fetch in parallel
                .collect(groupingBy(Investment::getRating,
                                    () -> new EnumMap<>(Rating.class),
                                    mapping(i -> {
                                        final BigDecimal principalNotYetReturned = i.getRemainingPrincipal()
                                                .subtract(i.getPaidInterest())
                                                .subtract(i.getPaidPenalty())
                                                .max(BigDecimal.ZERO);
                                        LOGGER.debug("Delinquent: {} CZK in loan #{}, investment #{}.",
                                                     principalNotYetReturned, i.getLoanId(), i.getId());
                                        return principalNotYetReturned;
                                    }, reducing(BigDecimal.ZERO, BigDecimalCalculator::plus))));
    }

    public void simulateCharge(final int loanId, final Rating rating, final BigDecimal amount) {
        LOGGER.debug("Simulating charge for loan #{} ({}), {} CZK.", loanId, rating, amount);
        blockedAmounts.get().simulateCharge(loanId, rating, amount);
        tenant.getBalance().update(amount.negate(), !tenant.getSessionInfo().isDryRun());
        portfolioOverview.set(null);
    }

    public PortfolioOverview getOverview() {
        return portfolioOverview.updateAndGet(old -> {
            if (old != null) {
                return old;
            }
            final Map<Rating, BigDecimal> atRisk = getAmountsAtRisk(tenant);
            final PortfolioOverview current = PortfolioOverviewImpl.calculate(tenant.getBalance(), statistics,
                                                                              blockedAmounts.get().getAdjustments(),
                                                                              atRisk);
            LOGGER.debug("Calculated: {}.", current);
            return current;
        });
    }
}
