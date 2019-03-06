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

import com.github.robozonky.api.confirmations.ConfirmationProvider;
import com.github.robozonky.api.remote.ControlApi;
import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.entities.sanitized.MarketplaceLoan;
import com.github.robozonky.api.strategies.InvestmentStrategy;
import com.github.robozonky.api.strategies.LoanDescriptor;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.api.strategies.RecommendedLoan;
import com.github.robozonky.app.tenant.PowerTenant;
import com.github.robozonky.common.tenant.Tenant;
import io.vavr.control.Either;
import jdk.jfr.Event;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.github.robozonky.app.events.impl.EventFactory.executionCompleted;
import static com.github.robozonky.app.events.impl.EventFactory.executionCompletedLazy;
import static com.github.robozonky.app.events.impl.EventFactory.executionStarted;
import static com.github.robozonky.app.events.impl.EventFactory.executionStartedLazy;
import static com.github.robozonky.app.events.impl.EventFactory.investmentDelegated;
import static com.github.robozonky.app.events.impl.EventFactory.investmentMade;
import static com.github.robozonky.app.events.impl.EventFactory.investmentMadeLazy;
import static com.github.robozonky.app.events.impl.EventFactory.investmentRejected;
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

    private static final Logger LOGGER = LogManager.getLogger(InvestingSession.class);
    private final Collection<LoanDescriptor> loansStillAvailable;
    private final List<Investment> investmentsMadeNow = new ArrayList<>(0);
    private final Investor investor;
    private final SessionState<LoanDescriptor> discarded, seen;
    private final PowerTenant tenant;

    InvestingSession(final Collection<LoanDescriptor> marketplace, final Investor investor, final PowerTenant tenant) {
        this.investor = investor;
        this.tenant = tenant;
        this.discarded = newSessionState(tenant, marketplace, "discardedLoans");
        this.seen = newSessionState(tenant, marketplace, "seenLoans");
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
        final long balance = portfolioOverview.getCzkAvailable().longValue();
        s.tenant.fire(executionStartedLazy(() -> executionStarted(loans, portfolioOverview)));
        if (balance >= tenant.getRestrictions().getMinimumInvestmentAmount() && !s.getAvailable().isEmpty()) {
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
        boolean invested;
        do {
            invested = strategy.recommend(getAvailable(), tenant.getPortfolio().getOverview(), tenant.getRestrictions())
                    .peek(r -> tenant.fire(loanRecommended(r)))
                    .anyMatch(this::invest); // keep trying until investment opportunities are exhausted
        } while (invested);
    }

    /**
     * Get loans that are available to be evaluated by the strategy. These are loans that come from the marketplace,
     * minus loans that are already invested into or discarded due to the {@link ConfirmationProvider} mechanism.
     * @return Loans in the marketplace in which the user could potentially invest. Unmodifiable.
     */
    Collection<LoanDescriptor> getAvailable() {
        loansStillAvailable.removeIf(d -> seen.contains(d) || discarded.contains(d));
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
        markSuccessfulInvestment(i);
        discard(recommendation.descriptor()); // never show again
        tenant.fire(investmentMadeLazy(() -> investmentMade(i, l, tenant.getPortfolio().getOverview())));
        return true;
    }

    private boolean unsuccessfulInvestment(final RecommendedLoan recommendation, final InvestmentFailure reason) {
        final String providerId = investor.getConfirmationProvider().map(ConfirmationProvider::getId).orElse("-");
        final LoanDescriptor loan = recommendation.descriptor();
        switch (reason) {
            case FAILED:
                discard(loan);
                break;
            case REJECTED:
                if (investor.getConfirmationProvider().isPresent()) {
                    tenant.fire(investmentRejected(recommendation, providerId));
                    // rejected through a confirmation provider => forget
                    discard(loan);
                } else {
                    // rejected due to no confirmation provider => make available for direct investment later
                    tenant.fire(investmentSkipped(recommendation));
                    final int loanId = loan.item().getId();
                    LOGGER.debug("Loan #{} protected by CAPTCHA, will check back later.", loanId);
                    skip(loan);
                }
                break;
            case DELEGATED:
                tenant.fire(investmentDelegated(recommendation, providerId));
                if (recommendation.isConfirmationRequired()) {
                    // confirmation required, delegation successful => forget
                    discard(loan);
                } else {
                    // confirmation not required, delegation successful => make available for direct investment later
                    skip(loan);
                }
                break;
            case SEEN_BEFORE: // still protected by CAPTCHA
                break;
            default:
                throw new IllegalStateException("Not possible.");
        }
        return false;
    }

    /**
     * Request {@link ControlApi} to invest in a given loan, leveraging the {@link ConfirmationProvider}.
     * @param recommendation Loan to invest into.
     * @return True if investment successful. The investment is reflected in {@link #getResult()}.
     */
    boolean invest(final RecommendedLoan recommendation) {
        LOGGER.debug("Will attempt to invest in {}.", recommendation);
        final LoanDescriptor loan = recommendation.descriptor();
        final int loanId = loan.item().getId();
        if (tenant.getPortfolio().getBalance().compareTo(recommendation.amount()) < 0) {
            // should not be allowed by the calling code
            LOGGER.debug("Balance was less than recommendation.");
            return false;
        }
        tenant.fire(investmentRequested(recommendation));
        final boolean seenBefore = seen.contains(loan);
        final Either<InvestmentFailure, BigDecimal> response = investor.invest(recommendation, seenBefore);
        LOGGER.debug("Response for loan {}: {}.", loanId, response);
        return response.fold(reason -> unsuccessfulInvestment(recommendation, reason),
                             amount -> successfulInvestment(recommendation, amount));
    }

    private void markSuccessfulInvestment(final Investment i) {
        investmentsMadeNow.add(i);
        tenant.getPortfolio().simulateCharge(i.getLoanId(), i.getRating(), i.getOriginalPrincipal());
    }

    private void discard(final LoanDescriptor loan) {
        skip(loan);
        discarded.put(loan);
    }

    private void skip(final LoanDescriptor loan) {
        seen.put(loan);
    }
}
