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

package com.github.robozonky.app.daemon;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;

import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.entities.sanitized.MarketplaceLoan;
import com.github.robozonky.api.strategies.InvestmentStrategy;
import com.github.robozonky.api.strategies.LoanDescriptor;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.api.strategies.RecommendedLoan;
import com.github.robozonky.app.tenant.PowerTenant;
import com.github.robozonky.internal.remote.InvestmentFailureType;
import io.vavr.control.Either;

import static com.github.robozonky.app.events.impl.EventFactory.executionCompleted;
import static com.github.robozonky.app.events.impl.EventFactory.executionCompletedLazy;
import static com.github.robozonky.app.events.impl.EventFactory.executionStarted;
import static com.github.robozonky.app.events.impl.EventFactory.executionStartedLazy;
import static com.github.robozonky.app.events.impl.EventFactory.investmentMade;
import static com.github.robozonky.app.events.impl.EventFactory.investmentMadeLazy;
import static com.github.robozonky.app.events.impl.EventFactory.investmentRequested;
import static com.github.robozonky.app.events.impl.EventFactory.investmentSkipped;
import static com.github.robozonky.app.events.impl.EventFactory.loanRecommended;

/**
 * Represents a single investment session over a certain marketplace, consisting of several attempts to invest into
 * given loan.
 * <p>
 * Instances of this class are supposed to be short-lived, as the marketplace and Zonky account balance can change
 * externally at any time. Essentially, one remote marketplace check should correspond to one instance of this class.
 */
final class InvestingSession extends AbstractSession<RecommendedLoan, LoanDescriptor, MarketplaceLoan> {

    private final Investor investor;

    InvestingSession(final Collection<LoanDescriptor> marketplace, final PowerTenant tenant) {
        super(marketplace, tenant, new SessionState<>(tenant, marketplace, d -> d.item().getId(), "discardedLoans"),
              Audit.investing());
        this.investor = Investor.build(tenant);
    }

    public static Collection<Investment> invest(final PowerTenant tenant,
                                                final Collection<LoanDescriptor> loans,
                                                final InvestmentStrategy strategy) {
        final InvestingSession s = new InvestingSession(loans, tenant);
        final PortfolioOverview portfolioOverview = tenant.getPortfolio().getOverview();
        s.tenant.fire(executionStartedLazy(() -> executionStarted(loans, portfolioOverview)));
        if (!s.getAvailable().isEmpty()) {
            s.invest(strategy);
        }
        final Collection<Investment> result = s.getResult();
        // make sure we get fresh portfolio reference here
        s.tenant.fire(executionCompletedLazy(() -> executionCompleted(result, tenant.getPortfolio().getOverview())));
        return Collections.unmodifiableCollection(result);
    }

    private void invest(final InvestmentStrategy strategy) {
        logger.debug("Starting the investing mechanism with balance upper bound of {} CZK.",
                     tenant.getKnownBalanceUpperBound());
        boolean invested;
        do {
            invested = strategy.recommend(getAvailable(), tenant.getPortfolio().getOverview(), tenant.getRestrictions())
                    .peek(r -> tenant.fire(loanRecommended(r)))
                    .filter(this::isBalanceAcceptable) // no need to try if we don't have enough money
                    .anyMatch(this::accept); // keep trying until investment opportunities are exhausted
        } while (invested);
    }

    private boolean successfulInvestment(final RecommendedLoan recommendation, final BigDecimal amount) {
        final int confirmedAmount = amount.intValue();
        final MarketplaceLoan l = recommendation.descriptor().item();
        final Investment i = Investment.fresh(l, confirmedAmount);
        result.add(i);
        tenant.getPortfolio().simulateCharge(i.getLoanId(), i.getRating(), i.getOriginalPrincipal());
        tenant.setKnownBalanceUpperBound(tenant.getKnownBalanceUpperBound() - confirmedAmount);
        discard(recommendation.descriptor()); // never show again
        tenant.fire(investmentMadeLazy(() -> investmentMade(i, l, tenant.getPortfolio().getOverview())));
        logger.info("Invested {} CZK into loan #{}.", confirmedAmount, l.getId());
        return true;
    }

    private boolean unsuccessfulInvestment(final RecommendedLoan recommendation,
                                           final InvestmentFailureType failureType) {
        if (failureType == InvestmentFailureType.TOO_MANY_REQUESTS) {
            // HTTP 429 needs to terminate investing and throw failure up to the availability algorithm.
            throw new IllegalStateException("HTTP 429 Too Many Requests caught during investing.");
        } else if (failureType == InvestmentFailureType.INSUFFICIENT_BALANCE) {
            tenant.setKnownBalanceUpperBound(recommendation.amount().longValue() - 1);
        }
        tenant.fire(investmentSkipped(recommendation));
        logger.debug("Failed investing {} CZK into loan #{}, reason: {}.",
                     recommendation.amount(), recommendation.descriptor().item().getId(), failureType);
        return false;
    }

    @Override
    protected boolean accept(final RecommendedLoan recommendation) {
        if (!isBalanceAcceptable(recommendation)) {
            logger.debug("Will not invest in {} due to balance ({} CZK) likely too low.", recommendation,
                         tenant.getKnownBalanceUpperBound());
            return false;
        }
        logger.debug("Will attempt to invest in {}.", recommendation);
        final LoanDescriptor loan = recommendation.descriptor();
        final int loanId = loan.item().getId();
        tenant.fire(investmentRequested(recommendation));
        final Either<InvestmentFailureType, BigDecimal> response = investor.invest(recommendation);
        return response.fold(failure -> unsuccessfulInvestment(recommendation, failure),
                             amount -> successfulInvestment(recommendation, amount));
    }
}
