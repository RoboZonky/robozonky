/*
 * Copyright 2017 Lukáš Petrovický
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

package com.github.triceo.robozonky.app.investing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.github.triceo.robozonky.api.Refreshable;
import com.github.triceo.robozonky.api.confirmations.ConfirmationProvider;
import com.github.triceo.robozonky.api.notifications.ExecutionCompletedEvent;
import com.github.triceo.robozonky.api.notifications.ExecutionStartedEvent;
import com.github.triceo.robozonky.api.notifications.InvestmentDelegatedEvent;
import com.github.triceo.robozonky.api.notifications.InvestmentMadeEvent;
import com.github.triceo.robozonky.api.notifications.InvestmentRejectedEvent;
import com.github.triceo.robozonky.api.notifications.InvestmentRequestedEvent;
import com.github.triceo.robozonky.api.notifications.InvestmentSkippedEvent;
import com.github.triceo.robozonky.api.remote.ControlApi;
import com.github.triceo.robozonky.api.remote.PortfolioApi;
import com.github.triceo.robozonky.api.remote.entities.BlockedAmount;
import com.github.triceo.robozonky.api.remote.entities.Investment;
import com.github.triceo.robozonky.api.remote.entities.Statistics;
import com.github.triceo.robozonky.api.strategies.LoanDescriptor;
import com.github.triceo.robozonky.api.strategies.PortfolioOverview;
import com.github.triceo.robozonky.api.strategies.Recommendation;
import com.github.triceo.robozonky.app.Events;
import com.github.triceo.robozonky.common.remote.AuthenticatedZonky;
import com.github.triceo.robozonky.internal.api.Defaults;
import com.github.triceo.robozonky.internal.api.Retriever;
import com.github.triceo.robozonky.internal.api.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a single investment session over a certain marketplace, consisting of several attempts to invest into
 * given marketplace.
 *
 * Instances of this class are supposed to be short-lived, as the marketplace and Zonky account balance can change
 * externally at any time. Essentially, one remote marketplace check should correspond to one instance of this class.
 *
 */
class Session implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(Session.class);
    private static final AtomicReference<Session> INSTANCE = new AtomicReference<>(null);

    /**
     * Create a new instance of this class. Only one instance is expected to exist at a time.
     *
     * At any given time, there may only be 1 instance of this class that is being used. This is due to the fact that
     * the sessions share state through a file, and therefore multiple concurrent sessions would interfere with one
     * another.
     *
     * @param investor Confirmation layer around the investment API.
     * @param api Authenticated access to Zonky for data retrieval.
     * @param marketplace Loans that are available in the marketplace.
     * @throws IllegalStateException When another {@link Session} instance was not {@link #close()}d.
     * @return
     */
    public synchronized static Session create(final Investor.Builder investor, final AuthenticatedZonky api,
                                              final Collection<LoanDescriptor> marketplace) {
        if (Session.INSTANCE.get() != null) {
            throw new IllegalStateException("Investment session already exists.");
        }
        final Session s = new Session(new LinkedHashSet<>(marketplace), investor, api);
        Session.INSTANCE.set(s);
        return s;
    }

    static BigDecimal getLiveBalance(final AuthenticatedZonky api) {
        return api.getWallet().getAvailableBalance();
    }

    static BigDecimal getDryRunBalance(final AuthenticatedZonky api) {
        final int balance = Settings.INSTANCE.getDefaultDryRunBalance();
        return (balance > -1) ? BigDecimal.valueOf(balance) : Session.getLiveBalance(api);
    }

    static Collection<Investment> invest(final Investor.Builder investor, final AuthenticatedZonky api,
                                         final InvestmentCommand command) {
        try (final Session session = Session.create(investor, api, command.getLoans())) {
            final int balance = session.getPortfolioOverview().getCzkAvailable();
            Events.fire(new ExecutionStartedEvent(investor.getUsername(), command.getLoans(), balance));
            if (balance >= Defaults.MINIMUM_INVESTMENT_IN_CZK && !session.getAvailableLoans().isEmpty()) {
                command.accept(session);
            }
            final PortfolioOverview portfolio = session.getPortfolioOverview();
            Session.LOGGER.info("Current value of portfolio is {} CZK, annual expected yield is {} % ({} CZK).",
                    portfolio.getCzkInvested(),
                    portfolio.getRelativeExpectedYield().scaleByPowerOfTen(2).setScale(2, RoundingMode.HALF_EVEN),
                    portfolio.getCzkExpectedYield());
            final Collection<Investment> result = session.getInvestmentsMade();
            Events.fire(new ExecutionCompletedEvent(investor.getUsername(), result, portfolio.getCzkAvailable()));
            return Collections.unmodifiableCollection(result);
        }
    }

    /**
     * Blocked amounts represent marketplace in various stages. Either the user has invested and the loan has not yet been
     * funded to 100 % ("na tržišti"), or the user invested and the loan has been funded ("na cestě"). In the latter
     * case, the loan has already disappeared from the marketplace, which means that it will not be available for
     * investing any more. As far as I know, the next stage is "v pořádku", the blocked amount is cleared and the loan
     * becomes an active investment.
     *
     * Based on that, this method deals with the first case - when the loan is still available for investing, but we've
     * already invested as evidenced by the blocked amount. It also unnecessarily deals with the second case, since
     * that is represented by a blocked amount as well. But that does no harm.
     *
     * In case user has made repeated investments into a particular loan, this will show up as multiple blocked amounts.
     * The method needs to handle this as well.
     *
     * @param api Authenticated Zonky API to read data from.
     * @return Every blocked amount represents a future investment. This method returns such investments.
     */
    static List<Investment> retrieveInvestmentsRepresentedByBlockedAmounts(final AuthenticatedZonky api) {
        // first group all blocked amounts by the loan ID and sum them
        final Map<Integer, Integer> amountsBlockedByLoans =
                api.getBlockedAmounts()
                        .filter(blocked -> blocked.getLoanId() > 0) // 0 == Zonky investors' fee
                        .collect(Collectors.groupingBy(BlockedAmount::getLoanId,
                                Collectors.summingInt(BlockedAmount::getAmount)));
        // and then fetch all the marketplace in parallel, converting them into investments
        return amountsBlockedByLoans.entrySet().parallelStream()
                .map(entry ->
                        Retriever.retrieve(() -> Optional.of(api.getLoan(entry.getKey())))
                                .map(l -> new Investment(l, entry.getValue()))
                                .orElseThrow(() -> new RuntimeException("Loan retrieval failed."))
                ).collect(Collectors.toList());
    }

    /**
     * Zonky API may return {@link PortfolioApi#statistics()} as null if the account has no previous investments.
     *
     * @param api API to execute the operation.
     * @return Either what the API returns, or an empty object.
     */
    private static Statistics retrieveStatistics(final AuthenticatedZonky api) {
        final Statistics returned = api.getStatistics();
        return returned == null ? new Statistics() : returned;
    }

    private final List<LoanDescriptor> loansStillAvailable;
    private final Collection<Investment> allInvestments, investmentsMadeNow = new LinkedHashSet<>(0);
    private final Refreshable<PortfolioOverview> portfolioOverview;
    private Investor investor;
    private BigDecimal balance;
    private final SessionState state;

    private Session(final Set<LoanDescriptor> marketplace, final Investor.Builder proxy, final AuthenticatedZonky zonky) {
        this.investor = proxy.build(zonky);
        balance = this.investor.isDryRun() ? Session.getDryRunBalance(zonky) : Session.getLiveBalance(zonky);
        Session.LOGGER.info("Starting account balance: {} CZK.", balance);
        state = new SessionState(marketplace);
        allInvestments = Session.retrieveInvestmentsRepresentedByBlockedAmounts(zonky);
        loansStillAvailable = marketplace.stream()
                .filter(l -> state.getDiscardedLoans().stream()
                        .noneMatch(l2 -> l.getLoan().getId() == l2.getLoan().getId()))
                .filter(l -> allInvestments.stream().noneMatch(i -> l.getLoan().getId() == i.getLoanId()))
                .collect(Collectors.toList());
        portfolioOverview = new Refreshable<PortfolioOverview>() {

            private final Statistics stats = Session.retrieveStatistics(zonky);

            @Override
            protected Supplier<Optional<String>> getLatestSource() {
                return () -> Optional.of(investmentsMadeNow.toString());
            }

            @Override
            protected Optional<PortfolioOverview> transform(final String source) {
                return Optional.of(PortfolioOverview.calculate(balance, stats, allInvestments));
            }

        };
        portfolioOverview.run(); // load initial portfolio overview so that strategy can use it
    }

    private synchronized void ensureOpen() {
        final Session s = Session.INSTANCE.get();
        if (s != null && !Objects.equals(s, this)) {
            throw new IllegalStateException("Session already closed.");
        }
    }

    /**
     * Get information about the portfolio, which is up to date relative to the current point in the session.
     *
     * @return Portfolio.
     */
    public synchronized PortfolioOverview getPortfolioOverview() {
        return portfolioOverview.getLatestBlocking();
    }

    /**
     * Get marketplace that are available to be evaluated by the strategy. These are marketplace that come from the marketplace,
     * minus marketplace that are already invested into or discarded due to the {@link ConfirmationProvider} mechanism.
     *
     * @return Loans in the marketplace in which the user could potentially invest. Unmodifiable.
     */
    public synchronized Collection<LoanDescriptor> getAvailableLoans() {
        return Collections.unmodifiableList(new ArrayList<>(this.loansStillAvailable));
    }

    /**
     * Get investments made during this session.
     *
     * @return Investments made so far during this session. Unmodifiable.
     */
    public synchronized List<Investment> getInvestmentsMade() {
        return Collections.unmodifiableList(new ArrayList<>(this.investmentsMadeNow));
    }

    /**
     * Request {@link ControlApi} to invest in a given loan, leveraging the {@link ConfirmationProvider}.
     *
     * @param recommendation Loan to invest into.
     * @return True if investment successful. The investment is reflected in {@link #getInvestmentsMade()}.
     * @throws IllegalStateException When already {@link #close()}d.
     */
    public synchronized boolean invest(final Recommendation recommendation) {
        this.ensureOpen();
        final LoanDescriptor loan = recommendation.getLoanDescriptor();
        final int loanId = loan.getLoan().getId();
        if (balance.intValue() < recommendation.getRecommendedInvestmentAmount()) {
            // should not be allowed by the calling code
            return false;
        }
        Events.fire(new InvestmentRequestedEvent(recommendation));
        final boolean seenBefore = state.getSeenLoans().stream().anyMatch(l -> l.getLoan().getId() == loanId);
        final ZonkyResponse response = investor.invest(recommendation, seenBefore);
        Session.LOGGER.debug("Response for loan {}: {}.", loanId, response);
        final String providerId = investor.getConfirmationProviderId().orElse("-");
        switch (response.getType()) {
            case REJECTED:
                return investor.getConfirmationProviderId().map(c -> {
                    Events.fire(new InvestmentRejectedEvent(recommendation, balance.intValue(), providerId));
                    // rejected through a confirmation provider => forget
                    this.discard(loan);
                    return false;
                }).orElseGet(() -> {
                    // rejected due to no confirmation provider => make available for direct investment later
                    Events.fire(new InvestmentSkippedEvent(recommendation));
                    Session.LOGGER.debug("Loan #{} protected by CAPTCHA, will check back later.", loanId);
                    this.skip(loan);
                    return false;
                });
            case DELEGATED:
                Events.fire(new InvestmentDelegatedEvent(recommendation, balance.intValue(), providerId));
                if (recommendation.isConfirmationRequired()) {
                    // confirmation required, delegation successful => forget
                    this.discard(loan);
                } else {
                    // confirmation not required, delegation successful => make available for direct investment later
                    this.skip(loan);
                }
                return false;
            case INVESTED:
                final int confirmedAmount = response.getConfirmedAmount().getAsInt();
                final Investment i = new Investment(recommendation.getLoanDescriptor().getLoan(), confirmedAmount);
                this.markSuccessfulInvestment(i);
                Events.fire(new InvestmentMadeEvent(i, balance.intValue(), investor.isDryRun()));
                return true;
            case SEEN_BEFORE:
                Events.fire(new InvestmentSkippedEvent(recommendation));
                return false;
            default:
                throw new IllegalStateException("Not possible.");
        }
    }

    private synchronized void markSuccessfulInvestment(final Investment i) {
        this.allInvestments.add(i);
        this.investmentsMadeNow.add(i);
        this.loansStillAvailable.removeIf(l -> l.getLoan().getId() == i.getLoanId());
        this.balance = balance.subtract(BigDecimal.valueOf(i.getAmount()));
        portfolioOverview.run(); // refresh portfolio overview
    }

    private synchronized void discard(final LoanDescriptor loan) {
        this.skip(loan);
        state.discard(loan);
    }

    private synchronized void skip(final LoanDescriptor loan) {
        this.loansStillAvailable.removeIf(l -> Objects.equals(loan, l));
        state.skip(loan);
    }

    @Override
    public synchronized void close() {
        Session.INSTANCE.set(null); // the session can no longer be used
    }

}
