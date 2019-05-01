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

package com.github.robozonky.notifications.listeners;

import java.util.Map;
import java.util.stream.Collectors;

import com.github.robozonky.api.notifications.LoanAndInvestment;
import com.github.robozonky.api.notifications.Summary;
import com.github.robozonky.api.notifications.WeeklySummaryEvent;
import com.github.robozonky.notifications.AbstractTargetHandler;
import com.github.robozonky.notifications.SupportedListener;

import static java.util.Map.entry;

public class WeeklySummaryEventListener extends AbstractListener<WeeklySummaryEvent> {

    public WeeklySummaryEventListener(final SupportedListener listener, final AbstractTargetHandler handler) {
        super(listener, handler);
    }

    private static Map<String, Object> describe(final LoanAndInvestment investment) {
        return Util.getLoanData(investment.getInvestment(), investment.getLoan());
    }

    @Override
    String getSubject(final WeeklySummaryEvent event) {
        return "Týdenní souhrn aktivity na Vašem Zonky účtu";
    }

    @Override
    String getTemplateFileName() {
        return "weekly-summary.ftl";
    }

    @Override
    protected Map<String, Object> getData(final WeeklySummaryEvent event) {
        final Summary summary = event.getSummary();
        return Map.ofEntries(
                entry("inTotal", summary.getCashInTotal()),
                entry("inFromDeposits", summary.getCashInFromDeposits()),
                entry("outTotal", summary.getCashOutTotal()),
                entry("total", summary.getCashInTotal() - summary.getCashOutTotal()),
                entry("totalDepositsAndWithdrawals",
                      summary.getCashInFromDeposits() - summary.getCashOutFromWithdrawals()),
                entry("outFromFees", summary.getCashOutFromFees()),
                entry("outFromWithdrawals", summary.getCashOutFromWithdrawals()),
                entry("portfolio", Util.summarizePortfolioStructure(summary.getPortfolioOverview())),
                entry("incomingInvestments", summary.getArrivingInvestments()
                        .map(WeeklySummaryEventListener::describe)
                        .collect(Collectors.toList())),
                entry("outgoingInvestments", summary.getLeavingInvestments()
                        .map(WeeklySummaryEventListener::describe)
                        .collect(Collectors.toList()))
        );
    }
}
