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

import com.github.robozonky.api.remote.entities.Participation;
import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.strategies.ParticipationDescriptor;
import com.github.robozonky.api.strategies.PurchaseStrategy;
import com.github.robozonky.api.strategies.RecommendedParticipation;
import com.github.robozonky.app.tenant.PowerTenant;
import com.github.robozonky.internal.remote.PurchaseResult;
import jdk.jfr.Event;
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

    private static final Logger LOGGER = Logging.purchasing();

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
                    .peek(r -> tenant.fire(purchaseRecommended(r)))
                    .filter(this::isBalanceAcceptable) // no need to try if we don't have enough money
                    .anyMatch(this::purchase); // keep trying until investment opportunities are exhausted
        } while (invested);
    }

    /**
     * Get loans that are available to be evaluated by the strategy. These are loans that come from the marketplace,
     * minus loans that are already invested into.
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
        final PurchaseResult result = tenant.call(zonky -> zonky.purchase(participation));
        if (result.isSuccess()) {
            LOGGER.info("Purchased a participation worth {} CZK.", participation.getRemainingPrincipal());
            return true;
        }
        final BigDecimal amount = participation.getRemainingPrincipal();
        switch (result.getFailureType().get()) {
            case INSUFFICIENT_BALANCE:
                LOGGER.debug("Failed purchasing a participation worth {} CZK. We don't have enough account balance.",
                             amount);
                tenant.setKnownBalanceUpperBound(amount.longValue() - 1);
                return false;
            case ALREADY_HAVE_INVESTMENT:
                LOGGER.debug("Failed purchasing a participation worth {} CZK. Someone's beaten us to it.", amount);
                return false;
            default:
                LOGGER.debug("Failed purchasing a participation worth {} CZK for an unknown reason.", amount);
                return false;
        }
    }

    private boolean isBalanceAcceptable(final RecommendedParticipation participation) {
        return participation.amount().intValue() <= tenant.getKnownBalanceUpperBound();
    }

    private boolean purchase(final RecommendedParticipation recommendation) {
        if (!isBalanceAcceptable(recommendation)) {
            LOGGER.debug("Will not purchase {} due to balance ({} CZK) likely too low.", recommendation,
                         tenant.getKnownBalanceUpperBound());
            return false;
        }
        tenant.fire(purchaseRequested(recommendation));
        final Participation participation = recommendation.descriptor().item();
        final Loan l = recommendation.descriptor().related();
        final boolean succeeded = tenant.getSessionInfo().isDryRun() || actualPurchase(participation);
        final Investment i = Investment.fresh(participation, l, recommendation.amount());
        discarded.put(recommendation.descriptor()); // don't purchase this one again in dry run
        if (succeeded) {
            investmentsMadeNow.add(i);
            tenant.getPortfolio().simulateCharge(i.getLoanId(), i.getRating(), i.getRemainingPrincipal());
            tenant.setKnownBalanceUpperBound(tenant.getKnownBalanceUpperBound() - recommendation.amount().longValue());
            tenant.fire(investmentPurchasedLazy(() -> investmentPurchased(i, l, tenant.getPortfolio().getOverview())));
        }
        return succeeded;
    }
}
