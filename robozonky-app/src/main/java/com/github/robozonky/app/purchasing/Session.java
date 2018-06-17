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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;

import com.github.robozonky.api.confirmations.ConfirmationProvider;
import com.github.robozonky.api.notifications.InvestmentPurchasedEvent;
import com.github.robozonky.api.notifications.PurchaseRecommendedEvent;
import com.github.robozonky.api.notifications.PurchaseRequestedEvent;
import com.github.robozonky.api.notifications.PurchasingCompletedEvent;
import com.github.robozonky.api.notifications.PurchasingStartedEvent;
import com.github.robozonky.api.remote.entities.Participation;
import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.strategies.ParticipationDescriptor;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.api.strategies.RecommendedParticipation;
import com.github.robozonky.app.Events;
import com.github.robozonky.app.authentication.Tenant;
import com.github.robozonky.app.portfolio.Portfolio;
import com.github.robozonky.app.util.SessionState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a single session over secondary marketplace, consisting of several attempts to purchase participations.
 * <p>
 * Instances of this class are supposed to be short-lived, as the marketplace and Zonky account balance can change
 * externally at any time. Essentially, one remote marketplace check should correspond to one instance of this class.
 */
final class Session {

    private static final Logger LOGGER = LoggerFactory.getLogger(Session.class);
    private final Collection<ParticipationDescriptor> stillAvailable;
    private final List<Investment> investmentsMadeNow = new ArrayList<>(0);
    private final Tenant authenticated;
    private final Portfolio portfolio;
    private final SessionState<ParticipationDescriptor> discarded;
    private PortfolioOverview portfolioOverview;

    Session(final Portfolio portfolio, final Collection<ParticipationDescriptor> marketplace, final Tenant tenant) {
        this.authenticated = tenant;
        this.discarded = new SessionState<>(tenant, marketplace, d -> d.item().getId(), "discardedParticipations");
        this.stillAvailable = new ArrayList<>(marketplace);
        this.portfolio = portfolio;
        this.portfolioOverview = portfolio.calculateOverview();
    }

    public static Collection<Investment> purchase(final Portfolio portfolio,
                                                  final Tenant auth,
                                                  final Collection<ParticipationDescriptor> items,
                                                  final RestrictedPurchaseStrategy strategy) {
        final Session session = new Session(portfolio, items, auth);
        final Collection<ParticipationDescriptor> c = session.getAvailable();
        if (c.isEmpty()) {
            return Collections.emptyList();
        }
        Events.fire(new PurchasingStartedEvent(c, session.portfolioOverview));
        session.purchase(strategy);
        final Collection<Investment> result = session.getResult();
        Events.fire(new PurchasingCompletedEvent(result, session.portfolioOverview));
        return Collections.unmodifiableCollection(result);
    }

    private void purchase(final RestrictedPurchaseStrategy strategy) {
        boolean invested;
        do {
            invested = strategy.apply(getAvailable(), portfolioOverview)
                    .filter(r -> portfolioOverview.getCzkAvailable() >= r.amount().intValue())
                    .peek(r -> Events.fire(new PurchaseRecommendedEvent(r)))
                    .anyMatch(this::purchase); // keep trying until investment opportunities are exhausted
        } while (invested);
    }

    /**
     * Get loans that are available to be evaluated by the strategy. These are loans that come from the marketplace,
     * minus loans that are already invested into or discarded due to the {@link ConfirmationProvider} mechanism.
     * @return Loans in the marketplace in which the user could potentially invest. Unmodifiable.
     */
    Collection<ParticipationDescriptor> getAvailable() {
        stillAvailable.removeIf(discarded::contains);
        return Collections.unmodifiableCollection(stillAvailable);
    }

    /**
     * Get investments made during this session.
     * @return Investments made so far during this session. Unmodifiable.
     */
    List<Investment> getResult() {
        return Collections.unmodifiableList(investmentsMadeNow);
    }

    private boolean actualPurchase(final Participation participation) {
        try {
            authenticated.run(zonky -> zonky.purchase(participation));
            return true;
        } catch (final NotFoundException | BadRequestException ex) {
            LOGGER.debug("Zonky 404 during purchasing. Likely someone's beaten us to it.", ex);
            return false;
        }
    }

    boolean purchase(final RecommendedParticipation recommendation) {
        Events.fire(new PurchaseRequestedEvent(recommendation));
        final Participation participation = recommendation.descriptor().item();
        final Loan loan = recommendation.descriptor().related();
        final boolean purchased = authenticated.getSessionInfo().isDryRun() || actualPurchase(participation);
        if (purchased) {
            final Investment i = Investment.fresh(participation, loan, recommendation.amount());
            markSuccessfulPurchase(i);
            discarded.put(recommendation.descriptor()); // don't purchase this one again in dry run
            Events.fire(new InvestmentPurchasedEvent(i, loan, portfolioOverview));
        }
        return purchased;
    }

    private void markSuccessfulPurchase(final Investment i) {
        investmentsMadeNow.add(i);
        portfolio.simulatePurchase(authenticated, i.getLoanId(), i.getRemainingPrincipal());
        portfolioOverview = portfolio.calculateOverview();
    }
}
