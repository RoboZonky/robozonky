package com.github.robozonky.app.summaries;

import java.time.OffsetDateTime;
import java.util.stream.Stream;

import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.strategies.PortfolioOverview;

public interface Summary {

    int getCashInTotal();

    int getCashInFromDeposits();

    int getCashOutTotal();

    int getCashOutFromFees();

    int getCashOutFromWithdrawals();

    PortfolioOverview getPortfolioOverview();

    Stream<Investment> getOutgoingInvestments();

    Stream<Investment> getIncomingInvestments();

    OffsetDateTime getCreatedOn();

}
