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

import static com.github.robozonky.app.summaries.Util.getAmountsSellable;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.notifications.ExtendedPortfolioOverview;
import com.github.robozonky.api.notifications.WeeklySummaryEvent;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.app.events.Events;
import com.github.robozonky.app.events.SessionEvents;
import com.github.robozonky.app.events.impl.EventFactory;
import com.github.robozonky.app.tenant.PowerTenant;
import com.github.robozonky.internal.jobs.TenantPayload;
import com.github.robozonky.internal.tenant.Tenant;
import com.github.robozonky.internal.util.functional.Tuple2;

final class Summarizer implements TenantPayload {

    private static final Logger LOGGER = LogManager.getLogger(Summarizer.class);

    private final boolean force;

    public Summarizer() {
        this(false);
    }

    Summarizer(final boolean force) {
        this.force = force;
    }

    private static ExtendedPortfolioOverview extend(final Tenant tenant) {
        final Tuple2<Map<Rating, Money>, Map<Rating, Money>> amountsSellable = getAmountsSellable(tenant);
        return ExtendedPortfolioOverviewImpl.extend(tenant.getPortfolio()
            .getOverview(),
                Util.getAmountsAtRisk(tenant), amountsSellable._1(), amountsSellable._2());
    }

    private static void run(final PowerTenant tenant) {
        final ExtendedPortfolioOverview portfolioOverview = extend(tenant);
        tenant.fire(EventFactory.weeklySummary(portfolioOverview));
    }

    @Override
    public void accept(final Tenant tenant) {
        PowerTenant powerTenant = (PowerTenant) tenant;
        SessionEvents sessionEvents = Events.forSession(powerTenant);
        boolean shouldTrigger = force || sessionEvents.isListenerRegistered(WeeklySummaryEvent.class);
        if (!shouldTrigger) {
            LOGGER.debug("Skipping on account of no event listener being configured to receive the results.");
            return;
        }
        run(powerTenant);
    }
}
