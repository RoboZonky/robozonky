/*
 * Copyright 2017 The RoboZonky Project
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

package com.github.robozonky.app.investing;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import com.github.robozonky.api.confirmations.ConfirmationProvider;
import com.github.robozonky.api.notifications.ExecutionCompletedEvent;
import com.github.robozonky.api.notifications.ExecutionStartedEvent;
import com.github.robozonky.api.notifications.InvestmentDelegatedEvent;
import com.github.robozonky.api.notifications.InvestmentMadeEvent;
import com.github.robozonky.api.notifications.InvestmentRejectedEvent;
import com.github.robozonky.api.notifications.InvestmentRequestedEvent;
import com.github.robozonky.api.notifications.InvestmentSkippedEvent;
import com.github.robozonky.api.notifications.LoanRecommendedEvent;
import com.github.robozonky.api.remote.ControlApi;
import com.github.robozonky.api.remote.entities.BlockedAmount;
import com.github.robozonky.api.remote.entities.Investment;
import com.github.robozonky.api.remote.enums.TransactionCategory;
import com.github.robozonky.api.strategies.InvestmentStrategy;
import com.github.robozonky.api.strategies.LoanDescriptor;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.api.strategies.RecommendedLoan;
import com.github.robozonky.app.Events;
import com.github.robozonky.app.portfolio.Portfolio;
import com.github.robozonky.common.remote.Zonky;
import com.github.robozonky.internal.api.Defaults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a single investment session over a certain marketplace, consisting of several attempts to invest into
 * given loan.
 * <p>
 * Instances of this class are supposed to be short-lived, as the marketplace and Zonky account balance can change
 * externally at any time. Essentially, one remote marketplace check should correspond to one instance of this class.
 */
class Session implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(Session.class);
    private static final AtomicReference<Session> INSTANCE = new AtomicReference<>(null);
    private final List<LoanDescriptor> loansStillAvailable;
    private final Collection<Investment> investmentsMadeNow = new LinkedHashSet<>();
    private final Investor investor;
    private final SessionState state;
    private PortfolioOverview portfolioOverview;

    private Session(final Set<LoanDescriptor> marketplace, final Investor.Builder proxy, final Zonky zonky) {
        this.investor = proxy.build(zonky);
        this.state = new SessionState(marketplace);
        this.loansStillAvailable = marketplace.stream()
                .filter(l -> state.getDiscardedLoans().stream().noneMatch(l2 -> l.item().getId() == l2.item().getId()))
                .filter(l -> Portfolio.INSTANCE.getPending().noneMatch(i -> l.item().getId() == i.getLoanId()))
                .collect(Collectors.toList());
        this.portfolioOverview = Portfolio.INSTANCE.calculateOverview(zonky, investor.isDryRun());
    }

    /**
     * Create a new instance of this class. Only one instance is expected to exist at a time.
     * <p>
     * At any given time, there may only be 1 instance of this class that is being used. This is due to the fact that
     * the sessions share state through a file, and therefore multiple concurrent sessions would interfere with one
     * another.
     * @param investor Confirmation layer around the investment API.
     * @param api Authenticated access to Zonky for data retrieval.
     * @param marketplace Loans that are available in the marketplace.
     * @return
     * @throws IllegalStateException When another {@link Session} instance was not {@link #close()}d.
     */
    public synchronized static Session create(final Investor.Builder investor, final Zonky api,
                                              final Collection<LoanDescriptor> marketplace) {
        if (Session.INSTANCE.get() != null) {
            throw new IllegalStateException("Investment session already exists.");
        }
        final Session s = new Session(new LinkedHashSet<>(marketplace), investor, api);
        Session.INSTANCE.set(s);
        return s;
    }

    private void invest(final InvestmentStrategy strategy) {
        boolean invested;
        do {
            invested = strategy.recommend(getAvailable(), getPortfolioOverview())
                    .peek(r -> Events.fire(new LoanRecommendedEvent(r)))
                    .anyMatch(this::invest); // keep trying until investment opportunities are exhausted
        } while (invested);
    }


    static Collection<Investment> invest(final Investor.Builder investor, final Zonky api,
                                         final Collection<LoanDescriptor> loans, final InvestmentStrategy strategy) {
        try (final Session session = Session.create(investor, api, loans)) {
            final int balance = session.getPortfolioOverview().getCzkAvailable();
            Events.fire(new ExecutionStartedEvent(loans, session.getPortfolioOverview()));
            if (balance >= Defaults.MINIMUM_INVESTMENT_IN_CZK && !session.getAvailable().isEmpty()) {
                session.invest(strategy);
            }
            final Collection<Investment> result = session.getResult();
            Events.fire(new ExecutionCompletedEvent(result, session.getPortfolioOverview()));
            return Collections.unmodifiableCollection(result);
        }
    }

    private synchronized void ensureOpen() {
        final Session s = Session.INSTANCE.get();
        if (!Objects.equals(s, this)) {
            throw new IllegalStateException("Session already closed.");
        }
    }

    /**
     * Get information about the portfolio, which is up to date relative to the current point in the session.
     * @return Portfolio.
     */
    public synchronized PortfolioOverview getPortfolioOverview() {
        return portfolioOverview;
    }

    /**
     * Get loans that are available to be evaluated by the strategy. These are loans that come from the marketplace,
     * minus loans that are already invested into or discarded due to the {@link ConfirmationProvider} mechanism.
     * @return Loans in the marketplace in which the user could potentially invest. Unmodifiable.
     */
    public synchronized Collection<LoanDescriptor> getAvailable() {
        return Collections.unmodifiableList(new ArrayList<>(loansStillAvailable));
    }

    /**
     * Get investments made during this session.
     * @return Investments made so far during this session. Unmodifiable.
     */
    public synchronized List<Investment> getResult() {
        return Collections.unmodifiableList(new ArrayList<>(investmentsMadeNow));
    }

    /**
     * Request {@link ControlApi} to invest in a given loan, leveraging the {@link ConfirmationProvider}.
     * @param recommendation Loan to invest into.
     * @return True if investment successful. The investment is reflected in {@link #getResult()}.
     * @throws IllegalStateException When already {@link #close()}d.
     */
    public synchronized boolean invest(final RecommendedLoan recommendation) {
        ensureOpen();
        final LoanDescriptor loan = recommendation.descriptor();
        final int loanId = loan.item().getId();
        if (portfolioOverview.getCzkAvailable() < recommendation.amount().intValue()) {
            // should not be allowed by the calling code
            return false;
        }
        Events.fire(new InvestmentRequestedEvent(recommendation));
        final boolean seenBefore = state.getSeenLoans().stream().anyMatch(l -> l.item().getId() == loanId);
        final ZonkyResponse response = investor.invest(recommendation, seenBefore);
        Session.LOGGER.debug("Response for loan {}: {}.", loanId, response);
        final String providerId = investor.getConfirmationProviderId().orElse("-");
        final int balance = portfolioOverview.getCzkAvailable();
        switch (response.getType()) {
            case REJECTED:
                return investor.getConfirmationProviderId().map(c -> {
                    Events.fire(new InvestmentRejectedEvent(recommendation, balance, providerId));
                    // rejected through a confirmation provider => forget
                    discard(loan);
                    return false;
                }).orElseGet(() -> {
                    // rejected due to no confirmation provider => make available for direct investment later
                    Events.fire(new InvestmentSkippedEvent(recommendation));
                    Session.LOGGER.debug("Loan #{} protected by CAPTCHA, will check back later.", loanId);
                    skip(loan);
                    return false;
                });
            case DELEGATED:
                Events.fire(new InvestmentDelegatedEvent(recommendation, balance, providerId));
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
                final Investment i = new Investment(recommendation.descriptor().item(), confirmedAmount);
                markSuccessfulInvestment(i);
                Events.fire(new InvestmentMadeEvent(i, portfolioOverview.getCzkAvailable(), investor.isDryRun()));
                return true;
            case SEEN_BEFORE: // still protected by CAPTCHA
                return false;
            default:
                throw new IllegalStateException("Not possible.");
        }
    }

    private synchronized void markSuccessfulInvestment(final Investment i) {
        investmentsMadeNow.add(i);
        loansStillAvailable.removeIf(l -> l.item().getId() == i.getLoanId());
        final BlockedAmount b = new BlockedAmount(i.getLoanId(), i.getAmount(), TransactionCategory.INVESTMENT);
        Portfolio.INSTANCE.newBlockedAmount(investor.getZonky(), b);
        final BigDecimal newBalance = BigDecimal.valueOf(portfolioOverview.getCzkAvailable()).subtract(i.getAmount());
        portfolioOverview = Portfolio.INSTANCE.calculateOverview(newBalance);
    }

    private synchronized void discard(final LoanDescriptor loan) {
        skip(loan);
        state.discard(loan);
    }

    private synchronized void skip(final LoanDescriptor loan) {
        loansStillAvailable.removeIf(l -> Objects.equals(loan, l));
        state.skip(loan);
    }

    @Override
    public synchronized void close() {
        Session.INSTANCE.set(null); // the session can no longer be used
    }
}
