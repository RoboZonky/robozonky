/*
 * Copyright 2020 The RoboZonky Project
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

import java.time.Duration;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.remote.entities.RiskPortfolio;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.internal.async.Reloadable;
import com.github.robozonky.internal.tenant.RemotePortfolio;
import com.github.robozonky.internal.tenant.Tenant;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static java.util.stream.Collectors.toMap;

class RemotePortfolioImpl implements RemotePortfolio {

    private static final Logger LOGGER = LogManager.getLogger(RemotePortfolioImpl.class);
    private final Reloadable<RemoteData> remoteData;
    private final AtomicReference<Map<Integer, Blocked>> syntheticByLoanId = new AtomicReference<>(new HashMap<>(0));
    private final AtomicReference<PortfolioOverview> portfolioOverview = new AtomicReference<>();
    private final boolean isDryRun;

    public RemotePortfolioImpl(final Tenant tenant) {
        this.isDryRun = tenant.getSessionInfo().isDryRun();
        this.remoteData = Reloadable.with(() -> RemoteData.load(tenant))
                .reloadAfter(Duration.ofMinutes(5))
                .finishWith(this::refresh)
                .build();
    }

    private static void includeAmount(Map<Rating, Money> amounts, Rating rating, Money amount) {
        amounts.compute(rating, (__, currentAmount) -> currentAmount == null ? amount : currentAmount.add(amount));
    }

    private void refresh(final RemoteData data) {
        refreshSynthetics(old -> old.entrySet().stream()
                .filter(e -> e.getValue().isValid(data))
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue)));
    }

    @Override
    public void simulateCharge(final int loanId, final Rating rating, final Money amount) {
        refreshSynthetics(old -> {
            final Map<Integer, Blocked> result = new LinkedHashMap<>(old);
            /*
             * synthetic blocked amounts are persistent only during dry runs; otherwise all synthetics will be removed
             * after a remote update of blocked amounts.
             */
            result.put(loanId, new Blocked(loanId, amount, rating, isDryRun));
            return result;
        });
    }

    private void refreshSynthetics(UnaryOperator<Map<Integer, Blocked>> refresher) {
        LOGGER.debug("Current synthetics: {}.", syntheticByLoanId.get());
        var updatedSynthetics = syntheticByLoanId.updateAndGet(refresher);
        // Force re-fetch of portfolio data now that we have registered a change.
        portfolioOverview.set(null);
        LOGGER.debug("New synthetics: {}", updatedSynthetics);
    }

    RemoteData getRemoteData() {
        return remoteData.get().getOrElseThrow(t -> new IllegalStateException("Failed fetching remote portfolio.", t));
    }

    @Override
    public Map<Rating, Money> getTotal() {
        var data = getRemoteData(); // use the same data for the entirety of this method
        LOGGER.debug("Remote data used: {}.", data);
        final Map<Rating, Money> amounts = data.getStatistics().getRiskPortfolio().stream()
                .collect(toMap(RiskPortfolio::getRating, portfolio -> portfolio.getDue().add(portfolio.getUnpaid()),
                               Money::add, () -> new EnumMap<>(Rating.class)));
        LOGGER.debug("Remote portfolio: {}.", amounts);
        data.getBlocked().forEach((id, blocked) -> includeAmount(amounts, blocked._1, blocked._2));
        LOGGER.debug("Plus remote blocked: {}.", amounts);
        syntheticByLoanId.get().values().stream()
                .filter(blocked -> blocked.isValid(data))
                .forEach(blocked -> includeAmount(amounts, blocked.getRating(), blocked.getAmount()));
        LOGGER.debug("Grand total incl. synthetics: {}.", amounts);
        return Collections.unmodifiableMap(amounts);
    }

    @Override
    public PortfolioOverview getOverview() {
        PortfolioOverview old = portfolioOverview.get();
        if (old != null) {
            return old;
        }
        PortfolioOverview current = new PortfolioOverviewImpl(this);
        boolean haveNew = portfolioOverview.compareAndSet(null, current);
        if (haveNew) {
            LOGGER.debug("New portfolio overview: {}.", current);
        }
        return current;
    }
}
