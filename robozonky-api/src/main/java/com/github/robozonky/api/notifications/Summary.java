package com.github.robozonky.api.notifications;

import java.time.OffsetDateTime;
import java.util.stream.Stream;

import com.github.robozonky.api.strategies.PortfolioOverview;

public interface Summary {

    int getCashInTotal();

    int getCashInFromDeposits();

    int getCashOutTotal();

    int getCashOutFromFees();

    int getCashOutFromWithdrawals();

    PortfolioOverview getPortfolioOverview();

    Stream<LoanAndInvestment> getOutgoingInvestments();

    Stream<LoanAndInvestment> getIncomingInvestments();

    OffsetDateTime getCreatedOn();

}
