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

package com.github.robozonky.app.tenant;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.robozonky.api.remote.entities.OverallPortfolio;
import com.github.robozonky.api.remote.entities.RiskPortfolio;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.common.tenant.RemotePortfolio;
import com.github.robozonky.common.tenant.Tenant;
import com.github.robozonky.internal.util.BigDecimalCalculator;
import com.github.robozonky.util.Reloadable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class RemotePortfolioImpl implements RemotePortfolio {

    private static final Logger LOGGER = LoggerFactory.getLogger(RemotePortfolioImpl.class);

    private final Reloadable<RemoteData> data;
    private final AtomicReference<Map<Integer, Blocked>> syntheticByLoanId =
            new AtomicReference<>(new LinkedHashMap<>(0));
    private final Reloadable<PortfolioOverview> portfolioOverview;

    public RemotePortfolioImpl(final Tenant tenant) {
        this.data = Reloadable.of(() -> RemoteData.load(tenant), Duration.ofMinutes(5), this::refresh);
        this.portfolioOverview = Reloadable.of(() -> new PortfolioOverviewImpl(getBalance(), getTotal(), getAtRisk()),
                                               Duration.ofMinutes(5));
    }

    private static BigDecimal sum(final OverallPortfolio portfolio) {
        return BigDecimalCalculator.plus(portfolio.getDue(), portfolio.getUnpaid());
    }

    private static BigDecimal sum(final Collection<Blocked> blockedAmounts) {
        return blockedAmounts.stream().map(Blocked::getAmount).reduce(BigDecimal.ZERO, BigDecimalCalculator::plus);
    }

    private void refresh(final RemoteData data) {
        // remove synthetic charges that are replaced by actual remote blocked amounts
        final Map<Integer, Blocked> real = data.getBlocked();
        final Map<Integer, Blocked> updatedSynthetics = syntheticByLoanId.updateAndGet(old -> old.entrySet().stream()
                .filter(e -> !real.containsKey(e.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
        LOGGER.debug("New synthetics: {}.", updatedSynthetics);
        // force overview recalculation now that we have the latest data
        portfolioOverview.clear();
    }

    @Override
    public void simulateCharge(final int loanId, final Rating rating, final BigDecimal amount) {
        final Map<Integer, Blocked> updatedSynthetics = syntheticByLoanId.updateAndGet(old -> {
            final Map<Integer, Blocked> result = new LinkedHashMap<>(old);
            result.put(loanId, new Blocked(amount, rating));
            return result;
        });
        LOGGER.debug("Synthetic added. New synthetics: {}", updatedSynthetics);
        // force overview recalculation now that we have the latest data
        portfolioOverview.clear();
    }

    private RemoteData getRemoteData() {
        return data.get().getOrElseThrow(t -> new IllegalStateException("Failed fetching remote data.", t));
    }

    @Override
    public BigDecimal getBalance() {
        final BigDecimal allBlocked = sum(syntheticByLoanId.get().values());
        return getRemoteData().getWallet().getAvailableBalance().subtract(allBlocked);
    }

    @Override
    public Map<Rating, BigDecimal> getTotal() {
        final Map<Rating, BigDecimal> amounts = getRemoteData().getStatistics().getRiskPortfolio().stream()
                .collect(Collectors.toMap(RiskPortfolio::getRating,
                                          RemotePortfolioImpl::sum,
                                          BigDecimal::add, // should not be necessary
                                          () -> new EnumMap<>(Rating.class)));
        final Stream<Blocked> blocked = Stream.concat(syntheticByLoanId.get().values().stream(),
                                                      getRemoteData().getBlocked().values().stream());
        blocked.forEach(b -> {
            final Rating r = b.getRating();
            final BigDecimal amount = b.getAmount();
            amounts.put(r, amounts.getOrDefault(r, BigDecimal.ZERO).add(amount));
        });
        return Collections.unmodifiableMap(amounts);
    }

    @Override
    public Map<Rating, BigDecimal> getAtRisk() {
        return getRemoteData().getAtRisk();
    }

    @Override
    public PortfolioOverview getOverview() {
        return portfolioOverview.get()
                .getOrElseThrow(t -> new IllegalStateException("Failed calculating portfolio overview.", t));
    }
}
