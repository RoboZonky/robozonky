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

package com.github.robozonky.app.daemon;

import static com.github.robozonky.app.events.impl.EventFactory.executionCompleted;
import static com.github.robozonky.app.events.impl.EventFactory.executionCompletedLazy;
import static com.github.robozonky.app.events.impl.EventFactory.executionStarted;
import static com.github.robozonky.app.events.impl.EventFactory.executionStartedLazy;
import static com.github.robozonky.app.events.impl.EventFactory.investmentMade;
import static com.github.robozonky.app.events.impl.EventFactory.investmentMadeLazy;

import java.util.stream.Stream;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.api.strategies.InvestmentStrategy;
import com.github.robozonky.api.strategies.LoanDescriptor;
import com.github.robozonky.app.tenant.PowerTenant;

/**
 * Represents a single investment session over a certain marketplace, consisting of several attempts to invest into
 * given loan.
 * <p>
 * Instances of this class are supposed to be short-lived, as the marketplace and Zonky account balance can change
 * externally at any time. Essentially, one remote marketplace check should correspond to one instance of this class.
 */
final class InvestingSession extends AbstractSession<RecommendedLoan, LoanDescriptor, Loan> {

    private final Investor investor;

    InvestingSession(final Stream<LoanDescriptor> marketplace, final PowerTenant tenant) {
        super(marketplace, tenant, d -> d.item()
            .getId(), "discardedLoans", Audit.investing());
        this.investor = Investor.build(tenant);
    }

    public static Stream<Loan> invest(final PowerTenant tenant, final Stream<LoanDescriptor> loans,
            final InvestmentStrategy strategy) {
        final InvestingSession s = new InvestingSession(loans, tenant);
        s.tenant.fire(executionStartedLazy(() -> executionStarted(tenant.getPortfolio()
            .getOverview())));
        s.invest(strategy);
        // make sure we get fresh portfolio reference here
        s.tenant.fire(executionCompletedLazy(() -> executionCompleted(tenant.getPortfolio()
            .getOverview())));
        return s.getResult();
    }

    private void invest(final InvestmentStrategy strategy) {
        logger.debug("Starting the investing mechanism with balance upper bound of {}.",
                tenant.getKnownBalanceUpperBound());
        getAvailable()
            .flatMap(i -> strategy.recommend(i, () -> tenant.getPortfolio()
                .getOverview(), tenant.getSessionInfo())
                .map(amount -> new RecommendedLoan(i, amount))
                .stream())
            .takeWhile(this::isBalanceAcceptable) // no need to try if we don't have enough money
            .forEach(this::accept); // keep trying until investment opportunities are exhausted
    }

    private boolean successfulInvestment(final RecommendedLoan recommendation, final Money amount) {
        final Loan l = recommendation.descriptor()
            .item();
        result.add(l);
        tenant.getPortfolio()
            .simulateCharge(l.getId(), l.getRating(), amount);
        tenant.setKnownBalanceUpperBound(tenant.getKnownBalanceUpperBound()
            .subtract(amount));
        discard(recommendation.descriptor()); // never show again
        tenant.fire(investmentMadeLazy(() -> investmentMade(l, amount, tenant.getPortfolio()
            .getOverview())));
        logger.info("Invested {} into loan #{}.", amount, l.getId());
        return true;
    }

    @Override
    protected boolean accept(final RecommendedLoan recommendation) {
        if (!isBalanceAcceptable(recommendation)) {
            logger.debug("Will not invest in {} due to balance ({}) likely too low.", recommendation,
                    tenant.getKnownBalanceUpperBound());
            return false;
        }
        logger.debug("Will attempt to invest in {}.", recommendation);
        try {
            investor.invest(recommendation);
            return successfulInvestment(recommendation, recommendation.amount());
        } catch (BadRequestException ex) {
            var response = FailureTypeUtil.getResponseEntity(ex.getResponse());
            switch (response) {
                case "TOO_MANY_REQUESTS":
                    // HTTP 429 needs to terminate investing and throw failure up to the availability algorithm.
                    throw new IllegalStateException("HTTP 429 Too Many Requests caught during purchasing.", ex);
                case "INSUFFICIENT_BALANCE":
                    logger.debug("Failed investing {}. We don't have sufficient balance.", recommendation.amount());
                    tenant.setKnownBalanceUpperBound(recommendation.amount()
                        .subtract(1));
                    return false;
                default:
                    logger.debug("Failed investing {} into loan #{}. Reason given: {}.", recommendation.amount(),
                            recommendation.descriptor()
                                .item()
                                .getId(),
                            response);
                    return false;
            }
        } catch (NotFoundException ex) {
            logger.debug("Failed investing into loan #{}, not found.", recommendation.descriptor()
                .item()
                .getId());
            return false;
        } catch (Exception ex) {
            throw new IllegalStateException("Unknown exception caught during investing.", ex);
        }
    }
}
