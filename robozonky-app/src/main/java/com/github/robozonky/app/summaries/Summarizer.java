/*
 * Copyright 2019 The RoboZonky Project
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

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Consumer;

import com.github.robozonky.api.notifications.Summary;
import com.github.robozonky.api.remote.entities.Transaction;
import com.github.robozonky.app.events.impl.EventFactory;
import com.github.robozonky.app.tenant.PowerTenant;
import com.github.robozonky.internal.api.jobs.TenantPayload;
import com.github.robozonky.internal.api.remote.Select;
import com.github.robozonky.internal.api.tenant.Tenant;
import com.github.robozonky.internal.test.DateUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

final class Summarizer implements TenantPayload {

    private static final Logger LOGGER = LogManager.getLogger(Summarizer.class);

    private static void run(final PowerTenant tenant) {
        // assemble processors
        final CashFlowProcessor cashFlow = new CashFlowProcessor();
        final LeavingInvestmentProcessor leavingInvestmentProcessor = new LeavingInvestmentProcessor(tenant);
        final ArrivingInvestmentProcessor newInvestmentProcessor = new ArrivingInvestmentProcessor(tenant);
        final Collection<Consumer<Transaction>> processors =
                Arrays.asList(cashFlow, leavingInvestmentProcessor, newInvestmentProcessor);
        // prepare transactions and process them with all the processors
        final LocalDate oneWeekAgo = DateUtil.localNow().toLocalDate().minusWeeks(1);
        LOGGER.debug("Will process transactions with date of {} and closer.", oneWeekAgo);
        final Select sinceLastTime = new Select().greaterThanOrEquals("transaction.transactionDate", oneWeekAgo);
        tenant.call(z -> z.getTransactions(sinceLastTime))
                .parallel() // there may be tens of thousands of them
                .forEach(t -> processors.forEach(p -> p.accept(t)));
        // now prepare the summary and trigger the event
        final Summary summary = new SummaryBuilder(tenant)
                .addCashFlows(cashFlow)
                .addIncomingInvestments(newInvestmentProcessor)
                .addOutgoingInvestments(leavingInvestmentProcessor)
                .build();
        tenant.fire(EventFactory.weeklySummary(summary));
    }

    @Override
    public void accept(final Tenant tenant) {
        run((PowerTenant) tenant);
    }
}
