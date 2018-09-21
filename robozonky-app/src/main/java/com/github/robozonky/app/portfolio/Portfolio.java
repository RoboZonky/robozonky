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

package com.github.robozonky.app.portfolio;

import java.math.BigDecimal;
import java.util.function.Supplier;

import com.github.robozonky.api.remote.entities.Statistics;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.app.authentication.Tenant;
import com.github.robozonky.common.remote.Zonky;

public class Portfolio {

    private final Statistics statistics;
    private final RemoteBalance balance;
    private final Supplier<BlockedAmountProcessor> blockedAmounts;

    Portfolio(final Supplier<BlockedAmountProcessor> blockedAmounts, final Statistics statistics,
              final RemoteBalance balance) {
        this.blockedAmounts = blockedAmounts;
        this.statistics = statistics;
        this.balance = balance;
    }

    /**
     * Return a new instance of the class, loading information about all investments present and past from the Zonky
     * interface. This operation may take a while, as there may easily be hundreds or thousands of such investments.
     * @param tenant The API to be used to retrieve the data from Zonky.
     * @param transfers This will be initialized lazily as otherwise black-box system integration tests which test
     * the CLI would always end up calling Zonky and thus failing due to lack of authentication.
     * @return Empty in case there was a remote error.
     */
    public static Portfolio create(final Tenant tenant, final Supplier<BlockedAmountProcessor> transfers) {
        return new Portfolio(transfers, tenant.call(Zonky::getStatistics), RemoteBalance.create(tenant));
    }

    public static Portfolio create(final Tenant tenant, final Supplier<BlockedAmountProcessor> transfers,
                                   final RemoteBalance balance) {
        return new Portfolio(transfers, tenant.call(Zonky::getStatistics), balance);
    }

    public void simulateCharge(final int loanId, final Rating rating, final BigDecimal amount) {
        blockedAmounts.get().simulateCharge(loanId, rating, amount);
        balance.update(amount.negate());
    }

    public RemoteBalance getRemoteBalance() {
        return balance;
    }

    // FIXME only recalculate when balance and or blocked amounts actually change, otherwise wasters CPU and memory
    public PortfolioOverview calculateOverview() {
        return PortfolioOverview.calculate(balance.get(), statistics, blockedAmounts.get().getAdjustments(),
                                           Delinquencies.getAmountsAtRisk());
    }
}
