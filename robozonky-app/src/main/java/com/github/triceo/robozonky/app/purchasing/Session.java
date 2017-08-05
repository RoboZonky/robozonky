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

package com.github.triceo.robozonky.app.purchasing;

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
import com.github.triceo.robozonky.api.notifications.PurchaseMadeEvent;
import com.github.triceo.robozonky.api.notifications.PurchaseRequestedEvent;
import com.github.triceo.robozonky.api.notifications.PurchasingCompletedEvent;
import com.github.triceo.robozonky.api.notifications.PurchasingStartedEvent;
import com.github.triceo.robozonky.api.remote.entities.BlockedAmount;
import com.github.triceo.robozonky.api.remote.entities.Investment;
import com.github.triceo.robozonky.api.remote.entities.Loan;
import com.github.triceo.robozonky.api.remote.entities.Participation;
import com.github.triceo.robozonky.api.remote.entities.Statistics;
import com.github.triceo.robozonky.api.strategies.ParticipationDescriptor;
import com.github.triceo.robozonky.api.strategies.PortfolioOverview;
import com.github.triceo.robozonky.api.strategies.RecommendedParticipation;
import com.github.triceo.robozonky.app.Events;
import com.github.triceo.robozonky.common.remote.Zonky;
import com.github.triceo.robozonky.internal.api.Defaults;
import com.github.triceo.robozonky.internal.api.Retriever;
import com.github.triceo.robozonky.internal.api.Settings;
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
    private final List<ParticipationDescriptor> stillAvailable;
    private final Collection<Investment> allInvestments, investmentsMadeNow = new LinkedHashSet<>();
    private final Refreshable<PortfolioOverview> portfolioOverview;
    private final Zonky zonky;
    private final boolean isDryRun;
    private BigDecimal balance;

    private Session(final Set<ParticipationDescriptor> marketplace, final Zonky zonky, final boolean dryRun) {
        this.zonky = zonky;
        isDryRun = dryRun;
        balance = isDryRun ? Session.getDryRunBalance(zonky) : Session.getLiveBalance(zonky);
        Session.LOGGER.info("Starting account balance: {} CZK.", balance);
        allInvestments = Session.retrieveInvestmentsRepresentedByBlockedAmounts(zonky);
        stillAvailable = new ArrayList<>(marketplace);
        portfolioOverview = new Refreshable<PortfolioOverview>() {

            private final Statistics stats = zonky.getStatistics();

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

    public synchronized static Session create(final Zonky api, final Collection<ParticipationDescriptor> marketplace,
                                              final boolean dryRun) {
        if (Session.INSTANCE.get() != null) {
            throw new IllegalStateException("Purchasing session already exists.");
        }
        final Session s = new Session(new LinkedHashSet<>(marketplace), api, dryRun);
        Session.INSTANCE.set(s);
        return s;
    }

    static BigDecimal getLiveBalance(final Zonky api) {
        return api.getWallet().getAvailableBalance();
    }

    static BigDecimal getDryRunBalance(final Zonky api) {
        final int balance = Settings.INSTANCE.getDefaultDryRunBalance();
        return (balance > -1) ? BigDecimal.valueOf(balance) : Session.getLiveBalance(api);
    }

    static Collection<Investment> invest(final Zonky api, final InvestmentCommand command, final boolean dryRun) {
        final Collection<ParticipationDescriptor> items = command.getItems();
        try (final Session session = Session.create(api, items, dryRun)) {
            final int balance = session.getPortfolioOverview().getCzkAvailable();
            Events.fire(new PurchasingStartedEvent(items, session.getPortfolioOverview()));
            if (balance >= Defaults.MINIMUM_INVESTMENT_IN_CZK && !session.getAvailableParticipations().isEmpty()) {
                command.accept(session);
            }
            final PortfolioOverview portfolio = session.getPortfolioOverview();
            Session.LOGGER.info("Current value of portfolio is {} CZK, annual expected yield is {} % ({} CZK).",
                                portfolio.getCzkInvested(),
                                portfolio.getRelativeExpectedYield().scaleByPowerOfTen(2).setScale(2,
                                                                                                   RoundingMode
                                                                                                           .HALF_EVEN),
                                portfolio.getCzkExpectedYield());
            final Collection<Investment> result = session.getParticipationsBought();
            Events.fire(new PurchasingCompletedEvent(result, portfolio));
            return Collections.unmodifiableCollection(result);
        }
    }

    /**
     * Blocked amounts represent loans in various stages. Either the user has invested and the loan has not yet been
     * funded to 100 % ("na tržišti"), or the user invested and the loan has been funded ("na cestě"). In the latter
     * case, the loan has already disappeared from the marketplace, which means that it will not be available for
     * investing any more. As far as we know, the next stage is "v pořádku", the blocked amount is cleared and the loan
     * becomes an active investment.
     * <p>
     * Based on that, this method deals with the first case - when the loan is still available for investing, but we've
     * already invested as evidenced by the blocked amount. It also unnecessarily deals with the second case, since
     * that is represented by a blocked amount as well. But that does no harm.
     * <p>
     * In case user has made repeated investments into a particular loan, this will show up as multiple blocked amounts.
     * The method needs to handle this as well.
     * @param api Authenticated Zonky API to read data from.
     * @return Every blocked amount represents a future investment. This method returns such investments.
     */
    static List<Investment> retrieveInvestmentsRepresentedByBlockedAmounts(final Zonky api) {
        // first group all blocked amounts by the loan ID and sum them
        final Map<Integer, Integer> amountsBlockedByLoans =
                api.getBlockedAmounts()
                        .filter(blocked -> blocked.getLoanId() > 0) // 0 == Zonky investors' fee
                        .collect(Collectors.groupingBy(BlockedAmount::getLoanId,
                                                       Collectors.summingInt(BlockedAmount::getAmount)));
        // and then fetch all the loans in parallel, converting them into investments
        return amountsBlockedByLoans.entrySet().parallelStream()
                .map(entry ->
                             Retriever.retrieve(() -> Optional.of(api.getLoan(entry.getKey())))
                                     .map(l -> new Investment(l, entry.getValue()))
                                     .orElseThrow(() -> new RuntimeException("Loan retrieval failed."))
                ).collect(Collectors.toList());
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
        return portfolioOverview.getLatestBlocking();
    }

    /**
     * Get loans that are available to be evaluated by the strategy. These are loans that come from the marketplace,
     * minus loans that are already invested into or discarded due to the {@link ConfirmationProvider} mechanism.
     * @return Loans in the marketplace in which the user could potentially invest. Unmodifiable.
     */
    public synchronized Collection<ParticipationDescriptor> getAvailableParticipations() {
        return Collections.unmodifiableList(new ArrayList<>(this.stillAvailable));
    }

    /**
     * Get investments made during this session.
     * @return Investments made so far during this session. Unmodifiable.
     */
    public synchronized List<Investment> getParticipationsBought() {
        return Collections.unmodifiableList(new ArrayList<>(this.investmentsMadeNow));
    }

    public synchronized boolean invest(final RecommendedParticipation recommendation) {
        this.ensureOpen();
        final ParticipationDescriptor descriptor = recommendation.descriptor();
        final Participation participation = descriptor.item();
        if (balance.intValue() < recommendation.amount().intValue()) {
            // should not be allowed by the calling code
            return false;
        }
        Events.fire(new PurchaseRequestedEvent(recommendation));
        if (!isDryRun) {
            zonky.purchase(recommendation.descriptor().item());
        }
        final Loan l = new Loan(participation.getLoanId(), Integer.MAX_VALUE);
        final Investment i = new Investment(l, recommendation.amount().intValue());
        markSuccessfulInvestment(i);
        Events.fire(new PurchaseMadeEvent(i, balance.intValue(), isDryRun));
        return true;
    }

    private synchronized void markSuccessfulInvestment(final Investment i) {
        this.allInvestments.add(i);
        this.investmentsMadeNow.add(i);
        this.stillAvailable.removeIf(l -> l.item().getLoanId() == i.getLoanId());
        this.balance = balance.subtract(BigDecimal.valueOf(i.getAmount()));
        portfolioOverview.run(); // refresh portfolio overview
    }

    @Override
    public synchronized void close() {
        Session.INSTANCE.set(null); // the session can no longer be used
    }
}
