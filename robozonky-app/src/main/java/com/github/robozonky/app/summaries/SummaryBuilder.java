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

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.robozonky.api.notifications.LoanAndInvestment;
import com.github.robozonky.api.notifications.Summary;
import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.internal.tenant.Tenant;

final class SummaryBuilder {

    private static final Comparator<Investment> LEAST_RECENT_FIRST =
            Comparator.comparing(Investment::getInvestmentDate)
                    .thenComparing(Investment::getId);

    private final Tenant tenant;
    private Supplier<Stream<CashFlow>> cashFlows = Stream::empty;
    private Supplier<Stream<Investment>> outgoingInvestments = Stream::empty;
    private Supplier<Stream<Investment>> incomingInvestments = Stream::empty;

    public SummaryBuilder(final Tenant tenant) {
        this.tenant = tenant;
    }

    private static Stream<Investment> deduplicate(final Stream<Investment> investmentStream) {
        return investmentStream
                .collect(Collectors.groupingBy(Investment::getLoanId, HashMap::new, Collectors.toList()))
                .values()
                .stream()
                .map(l -> l.get(0));
    }

    private Collection<LoanAndInvestment> expand(final Stream<Investment> investmentStream) {
        return investmentStream
                .parallel() // load the loans from Zonky in parallel
                .sorted(LEAST_RECENT_FIRST)
                .map(i -> new LoanAndInvestmentImpl(i, tenant.getLoan(i.getLoanId())))
                .collect(Collectors.toList());
    }

    public SummaryBuilder addCashFlows(final Supplier<Stream<CashFlow>> cashFlows) {
        this.cashFlows = cashFlows;
        return this;
    }

    public SummaryBuilder addIncomingInvestments(final Supplier<Stream<Investment>> investments) {
        this.incomingInvestments = investments;
        return this;
    }

    public SummaryBuilder addOutgoingInvestments(final Supplier<Stream<Investment>> investments) {
        this.outgoingInvestments = investments;
        return this;
    }

    public Summary build() {
        final CashFlowSummary cashFlowSummary = CashFlowSummary.from(cashFlows.get());
        final Collection<LoanAndInvestment> incoming = expand(deduplicate(incomingInvestments.get()));
        final Collection<LoanAndInvestment> outgoing = expand(deduplicate(outgoingInvestments.get()));
        return new SummaryImpl(tenant.getPortfolio().getOverview(), cashFlowSummary, incoming, outgoing);
    }
}
