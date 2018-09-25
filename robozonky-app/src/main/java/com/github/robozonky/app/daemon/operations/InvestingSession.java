/*
 * Copyright 2018 The RoboZonky Project
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

package com.github.robozonky.app.daemon.operations;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.github.robozonky.api.confirmations.ConfirmationProvider;
import com.github.robozonky.api.notifications.Event;
import com.github.robozonky.api.notifications.ExecutionCompletedEvent;
import com.github.robozonky.api.notifications.ExecutionStartedEvent;
import com.github.robozonky.api.notifications.InvestmentDelegatedEvent;
import com.github.robozonky.api.notifications.InvestmentMadeEvent;
import com.github.robozonky.api.notifications.InvestmentRejectedEvent;
import com.github.robozonky.api.notifications.InvestmentRequestedEvent;
import com.github.robozonky.api.notifications.InvestmentSkippedEvent;
import com.github.robozonky.api.notifications.LoanRecommendedEvent;
import com.github.robozonky.api.remote.ControlApi;
import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.strategies.LoanDescriptor;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.api.strategies.RecommendedLoan;
import com.github.robozonky.app.Events;
import com.github.robozonky.app.authentication.Tenant;
import com.github.robozonky.app.daemon.Portfolio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a single investment session over a certain marketplace, consisting of several attempts to invest into
 * given loan.
 * <p>
 * Instances of this class are supposed to be short-lived, as the marketplace and Zonky account balance can change
 * externally at any time. Essentially, one remote marketplace check should correspond to one instance of this class.
 */
final class InvestingSession {

    private static final Logger LOGGER = LoggerFactory.getLogger(InvestingSession.class);
    private final Collection<LoanDescriptor> loansStillAvailable;
    private final List<Investment> investmentsMadeNow = new ArrayList<>(0);
    private final Investor investor;
    private final SessionState<LoanDescriptor> discarded, seen;
    private final Portfolio portfolio;
    private PortfolioOverview portfolioOverview;

    InvestingSession(final Portfolio portfolio, final Collection<LoanDescriptor> marketplace, final Investor investor,
                     final Tenant tenant) {
        this.investor = investor;
        this.discarded = newSessionState(tenant, marketplace, "discardedLoans");
        this.seen = newSessionState(tenant, marketplace, "seenLoans");
        this.loansStillAvailable = new ArrayList<>(marketplace);
        this.portfolio = portfolio;
        this.portfolioOverview = portfolio.getOverview();
    }

    private static SessionState<LoanDescriptor> newSessionState(final Tenant tenant,
                                                                final Collection<LoanDescriptor> marketplace,
                                                                final String key) {
        return new SessionState<>(tenant, marketplace, d -> d.item().getId(), key);
    }

    public static Collection<Investment> invest(final Portfolio portfolio, final Investor investor,
                                                final Tenant auth,
                                                final Collection<LoanDescriptor> loans,
                                                final RestrictedInvestmentStrategy strategy) {
        final InvestingSession session = new InvestingSession(portfolio, loans, investor, auth);
        final PortfolioOverview portfolioOverview = session.portfolioOverview;
        final int balance = portfolioOverview.getCzkAvailable().intValue();
        Events.fire(new ExecutionStartedEvent(loans, portfolioOverview));
        if (balance >= auth.getRestrictions().getMinimumInvestmentAmount() && !session.getAvailable().isEmpty()) {
            session.invest(strategy);
        }
        final Collection<Investment> result = session.getResult();
        // make sure we get fresh portfolio reference here
        Events.fire(new ExecutionCompletedEvent(result, session.portfolioOverview));
        return Collections.unmodifiableCollection(result);
    }

    private void invest(final RestrictedInvestmentStrategy strategy) {
        boolean invested;
        do {
            invested = strategy.apply(getAvailable(), portfolioOverview)
                    .peek(r -> Events.fire(new LoanRecommendedEvent(r)))
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

    /**
     * Request {@link ControlApi} to invest in a given loan, leveraging the {@link ConfirmationProvider}.
     * @param recommendation Loan to invest into.
     * @return True if investment successful. The investment is reflected in {@link #getResult()}.
     */
    boolean invest(final RecommendedLoan recommendation) {
        final LoanDescriptor loan = recommendation.descriptor();
        final int loanId = loan.item().getId();
        if (portfolioOverview.getCzkAvailable().compareTo(recommendation.amount()) < 0) {
            // should not be allowed by the calling code
            return false;
        }
        Events.fire(new InvestmentRequestedEvent(recommendation));
        final boolean seenBefore = seen.contains(loan);
        final ZonkyResponse response = investor.invest(recommendation, seenBefore);
        InvestingSession.LOGGER.debug("Response for loan {}: {}.", loanId, response);
        final String providerId = investor.getConfirmationProvider().map(ConfirmationProvider::getId).orElse("-");
        switch (response.getType()) {
            case REJECTED:
                return investor.getConfirmationProvider().map(c -> {
                    Events.fire(new InvestmentRejectedEvent(recommendation, providerId));
                    // rejected through a confirmation provider => forget
                    discard(loan);
                    return false;
                }).orElseGet(() -> {
                    // rejected due to no confirmation provider => make available for direct investment later
                    Events.fire(new InvestmentSkippedEvent(recommendation));
                    InvestingSession.LOGGER.debug(
                            "Loan #{} protected by CAPTCHA, will check back later.", loanId);
                    skip(loan);
                    return false;
                });
            case DELEGATED:
                final Event e = new InvestmentDelegatedEvent(recommendation, providerId);
                Events.fire(e);
                if (recommendation.isConfirmationRequired()) {
                    // confirmation required, delegation successful => forget
                    discard(loan);
                } else {
                    // confirmation not required, delegation successful => make available for direct investment later
                    skip(loan);
                }
                return false;
            case INVESTED:
                final int confirmedAmount = response.getConfirmedAmount().getAsInt();
                final Investment i = Investment.fresh(recommendation.descriptor().item(),
                                                      confirmedAmount);
                markSuccessfulInvestment(i);
                discard(recommendation.descriptor()); // never show again
                Events.fire(new InvestmentMadeEvent(i, loan.item(), portfolioOverview));
                return true;
            case SEEN_BEFORE: // still protected by CAPTCHA
                return false;
            default:
                throw new IllegalStateException("Not possible.");
        }
    }

    private void markSuccessfulInvestment(final Investment i) {
        investmentsMadeNow.add(i);
        portfolio.simulateCharge(i.getLoanId(), i.getRating(), i.getOriginalPrincipal());
        portfolioOverview = portfolio.getOverview();
    }

    private void discard(final LoanDescriptor loan) {
        skip(loan);
        discarded.put(loan);
    }

    private void skip(final LoanDescriptor loan) {
        seen.put(loan);
    }
}
