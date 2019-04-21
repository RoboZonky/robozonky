package com.github.robozonky.app.summaries;

import java.util.Collection;
import java.util.HashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.robozonky.api.notifications.Summary;
import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.strategies.PortfolioOverview;

final class SummaryBuilder {

    private Supplier<Stream<CashFlow>> cashFlows = Stream::empty;
    private Supplier<Stream<Investment>> outgoingInvestments = Stream::empty;
    private Supplier<Stream<Investment>> incomingInvestments = Stream::empty;

    private static Collection<Investment> deduplicate(final Stream<Investment> investmentStream) {
        return investmentStream
                .collect(Collectors.groupingBy(Investment::getLoanId, HashMap::new, Collectors.toList()))
                .values()
                .stream()
                .map(l -> l.get(0))
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

    public Summary build(final PortfolioOverview portfolioOverview) {
        final CashFlowSummary cashFlowSummary = CashFlowSummary.from(cashFlows.get());
        final Collection<Investment> incoming = deduplicate(incomingInvestments.get());
        final Collection<Investment> outgoing = deduplicate(outgoingInvestments.get());
        return new SummaryImpl(portfolioOverview, cashFlowSummary, incoming, outgoing);
    }
}
