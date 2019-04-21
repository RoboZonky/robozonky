package com.github.robozonky.app.summaries;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Stream;

import com.github.robozonky.api.notifications.LoanAndInvestment;
import com.github.robozonky.api.notifications.Summary;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.internal.test.DateUtil;

final class SummaryImpl implements Summary {

    private final OffsetDateTime createdOn = DateUtil.offsetNow();
    private final PortfolioOverview portfolioOverview;
    private final CashFlowSummary cashFlowSummary;
    private final Collection<LoanAndInvestment> incomingInvestments;
    private final Collection<LoanAndInvestment> outgoingInvestments;

    public SummaryImpl(final PortfolioOverview portfolioOverview, final CashFlowSummary cashFlowSummary,
                       final Collection<LoanAndInvestment> incoming, final Collection<LoanAndInvestment> outgoing) {
        this.portfolioOverview = portfolioOverview;
        this.cashFlowSummary = cashFlowSummary;
        this.incomingInvestments = new ArrayList<>(incoming);
        this.outgoingInvestments = new ArrayList<>(outgoing);
    }

    @Override
    public int getCashInTotal() {
        return cashFlowSummary.getInTotal();
    }

    @Override
    public int getCashInFromDeposits() {
        return cashFlowSummary.getInFromDeposits();
    }

    @Override
    public int getCashOutTotal() {
        return cashFlowSummary.getOutTotal();
    }

    @Override
    public int getCashOutFromFees() {
        return cashFlowSummary.getOutFromFees();
    }

    @Override
    public int getCashOutFromWithdrawals() {
        return cashFlowSummary.getOutFromWithdrawals();
    }

    @Override
    public PortfolioOverview getPortfolioOverview() {
        return portfolioOverview;
    }

    @Override
    public Stream<LoanAndInvestment> getOutgoingInvestments() {
        return incomingInvestments.stream();
    }

    @Override
    public Stream<LoanAndInvestment> getIncomingInvestments() {
        return outgoingInvestments.stream();
    }

    @Override
    public OffsetDateTime getCreatedOn() {
        return createdOn;
    }
}
