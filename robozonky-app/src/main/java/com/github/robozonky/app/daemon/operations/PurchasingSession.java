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
import com.github.robozonky.api.remote.entities.Participation;
import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.strategies.ParticipationDescriptor;
import com.github.robozonky.api.strategies.PurchaseStrategy;
import com.github.robozonky.api.strategies.RecommendedParticipation;
import com.github.robozonky.app.daemon.Portfolio;
import com.github.robozonky.app.events.Events;
import com.github.robozonky.common.Tenant;

import static com.github.robozonky.app.events.EventFactory.investmentPurchased;
import static com.github.robozonky.app.events.EventFactory.investmentPurchasedLazy;
import static com.github.robozonky.app.events.EventFactory.purchaseRecommended;
import static com.github.robozonky.app.events.EventFactory.purchaseRequested;
import static com.github.robozonky.app.events.EventFactory.purchasingCompleted;
import static com.github.robozonky.app.events.EventFactory.purchasingStarted;

/**
 * Represents a single session over secondary marketplace, consisting of several attempts to purchase participations.
 * <p>
 * Instances of this class are supposed to be short-lived, as the marketplace and Zonky account balance can change
 * externally at any time. Essentially, one remote marketplace check should correspond to one instance of this class.
 */
final class PurchasingSession {

    private final Collection<ParticipationDescriptor> stillAvailable;
    private final List<Investment> investmentsMadeNow = new ArrayList<>(0);
    private final Tenant authenticated;
    private final Portfolio portfolio;
    private final SessionState<ParticipationDescriptor> discarded;
    private final Events events;

    PurchasingSession(final Portfolio portfolio, final Collection<ParticipationDescriptor> marketplace,
                      final Tenant tenant) {
        this.events = Events.forSession(tenant.getSessionInfo());
        this.authenticated = tenant;
        this.discarded = new SessionState<>(tenant, marketplace, d -> d.item().getId(), "discardedParticipations");
        this.stillAvailable = new ArrayList<>(marketplace);
        this.portfolio = portfolio;
    }

    public static Collection<Investment> purchase(final Portfolio portfolio, final Tenant auth,
                                                  final Collection<ParticipationDescriptor> items,
                                                  final PurchaseStrategy strategy) {
        final PurchasingSession session = new PurchasingSession(portfolio, items, auth);
        final Collection<ParticipationDescriptor> c = session.getAvailable();
        if (c.isEmpty()) {
            return Collections.emptyList();
        }
        session.events.fire(purchasingStarted(c, portfolio.getOverview()));
        session.purchase(strategy);
        final Collection<Investment> result = session.getResult();
        session.events.fire(purchasingCompleted(result, portfolio.getOverview()));
        return Collections.unmodifiableCollection(result);
    }

    private void purchase(final PurchaseStrategy strategy) {
        boolean invested;
        do {
            invested = strategy.recommend(getAvailable(), portfolio.getOverview(), authenticated.getRestrictions())
                    .filter(r -> portfolio.getOverview().getCzkAvailable().compareTo(r.amount()) >= 0)
                    .peek(r -> events.fire(purchaseRecommended(r)))
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

    private void actualPurchase(final Participation participation) {
        try {
            authenticated.run(zonky -> zonky.purchase(participation));
        } catch (final Exception ex) {
            throw new IllegalStateException("Failed purchasing " + participation);
        }
    }

    boolean purchase(final RecommendedParticipation recommendation) {
        events.fire(purchaseRequested(recommendation));
        final Participation participation = recommendation.descriptor().item();
        final Loan loan = recommendation.descriptor().related();
        if (!authenticated.getSessionInfo().isDryRun()) {
            actualPurchase(participation);
        }
        final Investment i = Investment.fresh(participation, loan, recommendation.amount());
        markSuccessfulPurchase(i);
        discarded.put(recommendation.descriptor()); // don't purchase this one again in dry run
        events.fire(investmentPurchasedLazy(() -> investmentPurchased(i, loan, portfolio.getOverview())));
        return true;
    }

    private void markSuccessfulPurchase(final Investment i) {
        investmentsMadeNow.add(i);
        portfolio.simulateCharge(i.getLoanId(), i.getRating(), i.getRemainingPrincipal());
    }
}
