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

package com.github.robozonky.app.purchasing;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;

import com.github.robozonky.api.confirmations.ConfirmationProvider;
import com.github.robozonky.api.notifications.InvestmentPurchasedEvent;
import com.github.robozonky.api.notifications.PurchaseRequestedEvent;
import com.github.robozonky.api.notifications.PurchasingCompletedEvent;
import com.github.robozonky.api.notifications.PurchasingStartedEvent;
import com.github.robozonky.api.remote.entities.BlockedAmount;
import com.github.robozonky.api.remote.entities.Investment;
import com.github.robozonky.api.remote.entities.Participation;
import com.github.robozonky.api.remote.enums.TransactionCategory;
import com.github.robozonky.api.strategies.ParticipationDescriptor;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.api.strategies.RecommendedParticipation;
import com.github.robozonky.app.Events;
import com.github.robozonky.app.portfolio.Portfolio;
import com.github.robozonky.common.remote.Zonky;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a single session over secondary marketplace, consisting of several attempts to purchase participations.
 * <p>
 * Instances of this class are supposed to be short-lived, as the marketplace and Zonky account balance can change
 * externally at any time. Essentially, one remote marketplace check should correspond to one instance of this class.
 */
class Session implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(Session.class);
    private static final AtomicReference<Session> INSTANCE = new AtomicReference<>(null);
    private final List<ParticipationDescriptor> stillAvailable;
    private final Collection<Investment> investmentsMadeNow = new LinkedHashSet<>();
    private final Zonky zonky;
    private final boolean isDryRun;
    private PortfolioOverview portfolioOverview;

    private Session(final Set<ParticipationDescriptor> marketplace, final Zonky zonky, final boolean dryRun) {
        this.zonky = zonky;
        this.isDryRun = dryRun;
        this.stillAvailable = new ArrayList<>(marketplace);
        this.portfolioOverview = Portfolio.INSTANCE.calculateOverview(zonky, dryRun);
    }

    private synchronized static Session create(final Zonky api, final Collection<ParticipationDescriptor> marketplace,
                                               final boolean dryRun) {
        if (Session.INSTANCE.get() != null) {
            throw new IllegalStateException("Purchasing session already exists.");
        }
        final Session s = new Session(new LinkedHashSet<>(marketplace), api, dryRun);
        Session.INSTANCE.set(s);
        return s;
    }

    static Collection<Investment> purchase(final Zonky api, final Collection<ParticipationDescriptor> items,
                                           final InvestmentCommand command, final boolean dryRun) {
        try (final Session session = Session.create(api, items, dryRun)) {
            final Collection<ParticipationDescriptor> c = session.getAvailable();
            if (c.isEmpty()) {
                return Collections.emptyList();
            }
            Events.fire(new PurchasingStartedEvent(c, session.getPortfolioOverview()));
            command.accept(session);
            final Collection<Investment> result = session.getResult();
            Events.fire(new PurchasingCompletedEvent(result, session.getPortfolioOverview()));
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
    public synchronized Collection<ParticipationDescriptor> getAvailable() {
        return Collections.unmodifiableList(new ArrayList<>(stillAvailable));
    }

    /**
     * Get investments made during this session.
     * @return Investments made so far during this session. Unmodifiable.
     */
    public synchronized List<Investment> getResult() {
        return Collections.unmodifiableList(new ArrayList<>(investmentsMadeNow));
    }

    private boolean actualPurchase(final Participation participation) {
        try {
            zonky.purchase(participation);
            return true;
        } catch (final NotFoundException | BadRequestException ex) {
            LOGGER.debug("Zonky 404 during purchasing. Likely someone's beaten us to it.", ex);
            return false;
        }
    }

    public synchronized boolean purchase(final RecommendedParticipation recommendation) {
        ensureOpen();
        if (portfolioOverview.getCzkAvailable() < recommendation.amount().intValue()) {
            // should not be allowed by the calling code
            return false;
        }
        Events.fire(new PurchaseRequestedEvent(recommendation));
        final Participation participation = recommendation.descriptor().item();
        final boolean purchased = isDryRun || actualPurchase(participation);
        if (purchased) {
            final Investment i = new Investment(recommendation.descriptor());
            markSuccessfulPurchase(i);
            Events.fire(new InvestmentPurchasedEvent(i, portfolioOverview.getCzkAvailable(), isDryRun));
        }
        return purchased;
    }

    private synchronized void markSuccessfulPurchase(final Investment i) {
        investmentsMadeNow.add(i);
        final int id = i.getLoanId();
        stillAvailable.removeIf(l -> l.item().getLoanId() == id);
        final BigDecimal amount = i.getAmount();
        Portfolio.INSTANCE.newBlockedAmount(zonky, new BlockedAmount(id, amount, TransactionCategory.SMP_BUY));
        final BigDecimal newBalance = BigDecimal.valueOf(portfolioOverview.getCzkAvailable()).subtract(amount);
        portfolioOverview = Portfolio.INSTANCE.calculateOverview(newBalance);
    }

    @Override
    public synchronized void close() {
        Session.INSTANCE.set(null); // the session can no longer be used
    }
}
