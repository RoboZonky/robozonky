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

package com.github.robozonky.app.summaries;

import java.util.Map;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.ExtendedPortfolioOverview;
import com.github.robozonky.app.events.impl.EventFactory;
import com.github.robozonky.app.tenant.PowerTenant;
import com.github.robozonky.internal.jobs.TenantPayload;
import com.github.robozonky.internal.tenant.Tenant;
import com.github.robozonky.internal.util.functional.Tuple2;

import static com.github.robozonky.app.summaries.Util.getAmountsSellable;

final class Summarizer implements TenantPayload {

    private static ExtendedPortfolioOverview extend(final Tenant tenant) {
        final Tuple2<Map<Rating, Money>, Map<Rating, Money>> amountsSellable = getAmountsSellable(tenant);
        return ExtendedPortfolioOverviewImpl.extend(tenant.getPortfolio().getOverview(),
                Util.getAmountsAtRisk(tenant), amountsSellable._1(), amountsSellable._2());
    }

    private static void run(final PowerTenant tenant) {
        final ExtendedPortfolioOverview portfolioOverview = extend(tenant);
        tenant.fire(EventFactory.weeklySummary(portfolioOverview));
    }

    @Override
    public void accept(final Tenant tenant) {
        run((PowerTenant) tenant);
    }
}
