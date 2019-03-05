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
import com.github.robozonky.app.tenant.PowerTenant;
import jdk.jfr.Event;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.github.robozonky.app.events.impl.EventFactory.investmentPurchased;
import static com.github.robozonky.app.events.impl.EventFactory.investmentPurchasedLazy;
import static com.github.robozonky.app.events.impl.EventFactory.purchaseRecommended;
import static com.github.robozonky.app.events.impl.EventFactory.purchaseRequested;
import static com.github.robozonky.app.events.impl.EventFactory.purchasingCompleted;
import static com.github.robozonky.app.events.impl.EventFactory.purchasingCompletedLazy;
import static com.github.robozonky.app.events.impl.EventFactory.purchasingStarted;
import static com.github.robozonky.app.events.impl.EventFactory.purchasingStartedLazy;

/**
 * Represents a single session over secondary marketplace, consisting of several attempts to purchase participations.
 * <p>
 * Instances of this class are supposed to be short-lived, as the marketplace and Zonky account balance can change
 * externally at any time. Essentially, one remote marketplace check should correspond to one instance of this class.
 */
final class PurchasingSession {

    private static final Logger LOGGER = LogManager.getLogger(PurchasingSession.class);

    private final Collection<ParticipationDescriptor> stillAvailable;
    private final List<Investment> investmentsMadeNow = new ArrayList<>(0);
    private final PowerTenant tenant;
    private final SessionState<ParticipationDescriptor> discarded;

    PurchasingSession(final Collection<ParticipationDescriptor> marketplace, final PowerTenant tenant) {
        this.tenant = tenant;
        this.discarded = new SessionState<>(tenant, marketplace, d -> d.item().getId(), "discardedParticipations");
        this.stillAvailable = new ArrayList<>(marketplace);
    }

    public static Collection<Investment> purchase(final PowerTenant auth,
                                                  final Collection<ParticipationDescriptor> items,
                                                  final PurchaseStrategy strategy) {
        final PurchasingSession s = new PurchasingSession(items, auth);
        final Collection<ParticipationDescriptor> c = s.getAvailable();
        if (c.isEmpty()) {
            return Collections.emptyList();
        }
        s.tenant.fire(purchasingStartedLazy(() -> purchasingStarted(c, auth.getPortfolio().getOverview())));
        final Event event = new PurchasingSessionJfrEvent();
        try {
            event.begin();
            s.purchase(strategy);
        } finally {
            event.commit();
        }
        final Collection<Investment> result = s.getResult();
        s.tenant.fire(purchasingCompletedLazy(() -> purchasingCompleted(result, auth.getPortfolio().getOverview())));
        return Collections.unmodifiableCollection(result);
    }

    private void purchase(final PurchaseStrategy strategy) {
        boolean invested;
        do {
            invested = strategy.recommend(getAvailable(), tenant.getPortfolio().getOverview(), tenant.getRestrictions())
                    .filter(r -> tenant.getPortfolio().getBalance().compareTo(r.amount()) >= 0)
                    .peek(r -> tenant.fire(purchaseRecommended(r)))
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
            tenant.run(zonky -> zonky.purchase(participation));
            LOGGER.info("Purchased a participation worth {} CZK.", participation.getRemainingPrincipal());
            return true;
        } catch (final Exception ex) {
            LOGGER.debug("Failed purchasing {}. Likely someone's beaten us to it.", ex);
            return false;
        }
    }

    private boolean purchase(final RecommendedParticipation recommendation) {
        tenant.fire(purchaseRequested(recommendation));
        final Participation participation = recommendation.descriptor().item();
        final Loan l = recommendation.descriptor().related();
        final boolean succeeded = tenant.getSessionInfo().isDryRun() || actualPurchase(participation);
        final Investment i = Investment.fresh(participation, l, recommendation.amount());
        discarded.put(recommendation.descriptor()); // don't purchase this one again in dry run
        if (succeeded) {
            markSuccessfulPurchase(i);
            tenant.fire(investmentPurchasedLazy(() -> investmentPurchased(i, l, tenant.getPortfolio().getOverview())));
        }
        return succeeded;
    }

    private void markSuccessfulPurchase(final Investment i) {
        investmentsMadeNow.add(i);
        tenant.getPortfolio().simulateCharge(i.getLoanId(), i.getRating(), i.getRemainingPrincipal());
    }
}
