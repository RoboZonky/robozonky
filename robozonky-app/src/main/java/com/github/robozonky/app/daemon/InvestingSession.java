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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.github.robozonky.api.remote.ControlApi;
import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.entities.sanitized.MarketplaceLoan;
import com.github.robozonky.api.strategies.InvestmentStrategy;
import com.github.robozonky.api.strategies.LoanDescriptor;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.api.strategies.RecommendedLoan;
import com.github.robozonky.app.tenant.PowerTenant;
import com.github.robozonky.internal.remote.InvestmentFailureType;
import com.github.robozonky.internal.tenant.Tenant;
import io.vavr.control.Either;
import jdk.jfr.Event;
import org.apache.logging.log4j.Logger;

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
final class InvestingSession {

    private static final Logger LOGGER = Audit.investing();
    private final Collection<LoanDescriptor> loansStillAvailable;
    private final List<Investment> investmentsMadeNow = new ArrayList<>(0);
    private final Investor investor;
    private final SessionState<LoanDescriptor> discarded;
    private final PowerTenant tenant;

    InvestingSession(final Collection<LoanDescriptor> marketplace, final Investor investor, final PowerTenant tenant) {
        this.investor = investor;
        this.tenant = tenant;
        this.discarded = newSessionState(tenant, marketplace, "discardedLoans");
        this.loansStillAvailable = new ArrayList<>(marketplace);
    }

    private static SessionState<LoanDescriptor> newSessionState(final Tenant tenant,
                                                                final Collection<LoanDescriptor> marketplace,
                                                                final String key) {
        return new SessionState<>(tenant, marketplace, d -> d.item().getId(), key);
    }

    public static Collection<Investment> invest(final Investor investor, final PowerTenant tenant,
                                                final Collection<LoanDescriptor> loans,
                                                final InvestmentStrategy strategy) {
        final InvestingSession s = new InvestingSession(loans, investor, tenant);
        final PortfolioOverview portfolioOverview = tenant.getPortfolio().getOverview();
        s.tenant.fire(executionStartedLazy(() -> executionStarted(loans, portfolioOverview)));
        if (!s.getAvailable().isEmpty()) {
            final Event event = new InvestingSessionJfrEvent();
            try {
                event.begin();
                s.invest(strategy);
            } finally {
                event.commit();
            }
        }
        final Collection<Investment> result = s.getResult();
        // make sure we get fresh portfolio reference here
        s.tenant.fire(executionCompletedLazy(() -> executionCompleted(result, tenant.getPortfolio().getOverview())));
        return Collections.unmodifiableCollection(result);
    }

    private void invest(final InvestmentStrategy strategy) {
        LOGGER.debug("Starting the investing mechanism with balance upper bound of {} CZK.",
                     tenant.getKnownBalanceUpperBound());
        boolean invested;
        do {
            invested = strategy.recommend(getAvailable(), tenant.getPortfolio().getOverview(), tenant.getRestrictions())
                    .peek(r -> tenant.fire(loanRecommended(r)))
                    .filter(this::isBalanceAcceptable) // no need to try if we don't have enough money
                    .anyMatch(this::invest); // keep trying until investment opportunities are exhausted
        } while (invested);
    }

    /**
     * Get loans that are available to be evaluated by the strategy. These are loans that come from the marketplace,
     * minus loans that are already invested into.
     * @return Loans in the marketplace in which the user could potentially invest. Unmodifiable.
     */
    Collection<LoanDescriptor> getAvailable() {
        loansStillAvailable.removeIf(discarded::contains);
        return Collections.unmodifiableCollection(loansStillAvailable);
    }

    /**
     * Get investments made during this session.
     * @return Investments made so far during this session. Unmodifiable.
     */
    List<Investment> getResult() {
        return Collections.unmodifiableList(investmentsMadeNow);
    }

    private boolean successfulInvestment(final RecommendedLoan recommendation, final BigDecimal amount) {
        final int confirmedAmount = amount.intValue();
        final MarketplaceLoan l = recommendation.descriptor().item();
        final Investment i = Investment.fresh(l, confirmedAmount);
        investmentsMadeNow.add(i);
        tenant.getPortfolio().simulateCharge(i.getLoanId(), i.getRating(), i.getOriginalPrincipal());
        tenant.setKnownBalanceUpperBound(tenant.getKnownBalanceUpperBound() - confirmedAmount);
        discard(recommendation.descriptor()); // never show again
        tenant.fire(investmentMadeLazy(() -> investmentMade(i, l, tenant.getPortfolio().getOverview())));
        return true;
    }

    private boolean isBalanceAcceptable(final RecommendedLoan loan) {
        return loan.amount().intValue() <= tenant.getKnownBalanceUpperBound();
    }

    private boolean unsuccessfulInvestment(final RecommendedLoan recommendation, final InvestmentFailureType failureType) {
        if (failureType == InvestmentFailureType.INSUFFICIENT_BALANCE) {
            LOGGER.debug("Failed investing into {}. We don't have enough account balance.", recommendation);
            tenant.setKnownBalanceUpperBound(recommendation.amount().intValue() - 1);
        }
        tenant.fire(investmentSkipped(recommendation));
        return false;
    }

    /**
     * Request {@link ControlApi} to invest in a given loan.
     * @param recommendation Loan to invest into.
     * @return True if investment successful. The investment is reflected in {@link #getResult()}.
     */
    boolean invest(final RecommendedLoan recommendation) {
        if (!isBalanceAcceptable(recommendation)) {
            LOGGER.debug("Will not invest in {} due to balance ({} CZK) likely too low.", recommendation,
                         tenant.getKnownBalanceUpperBound());
            return false;
        }
        LOGGER.debug("Will attempt to invest in {}.", recommendation);
        final LoanDescriptor loan = recommendation.descriptor();
        final int loanId = loan.item().getId();
        tenant.fire(investmentRequested(recommendation));
        final Either<InvestmentFailureType, BigDecimal> response = investor.invest(recommendation);
        LOGGER.debug("Response for loan {}: {}.", loanId, response);
        return response.fold(failure -> unsuccessfulInvestment(recommendation, failure),
                             amount -> successfulInvestment(recommendation, amount));
    }

    private void discard(final LoanDescriptor loan) {
        discarded.put(loan);
    }
}
