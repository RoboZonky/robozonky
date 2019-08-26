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
import java.util.Collection;
import java.util.Collections;

import com.github.robozonky.api.remote.entities.Participation;
import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.strategies.ParticipationDescriptor;
import com.github.robozonky.api.strategies.PurchaseStrategy;
import com.github.robozonky.api.strategies.RecommendedParticipation;
import com.github.robozonky.app.tenant.PowerTenant;
import com.github.robozonky.internal.remote.PurchaseResult;

import static com.github.robozonky.app.events.impl.EventFactory.investmentPurchased;
import static com.github.robozonky.app.events.impl.EventFactory.investmentPurchasedLazy;
import static com.github.robozonky.app.events.impl.EventFactory.purchaseRecommended;
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
final class PurchasingSession extends
                              AbstractSession<RecommendedParticipation, ParticipationDescriptor, Participation> {

    PurchasingSession(final Collection<ParticipationDescriptor> marketplace, final PowerTenant tenant) {
        super(marketplace, tenant,
              new SessionState<>(tenant, marketplace, d -> d.item().getId(), "discardedParticipations"),
              Audit.purchasing());
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
        s.purchase(strategy);
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
                    .anyMatch(this::accept); // keep trying until investment opportunities are exhausted
        } while (invested);
    }

    private boolean actualPurchase(final Participation participation) {
        final PurchaseResult result = tenant.call(zonky -> zonky.purchase(participation));
        if (result.isSuccess()) {
            logger.info("Purchased a participation worth {} CZK.", participation.getRemainingPrincipal());
            return true;
        }
        final BigDecimal amount = participation.getRemainingPrincipal();
        switch (result.getFailureType().get()) {
            case TOO_MANY_REQUESTS:
                // HTTP 429 needs to terminate investing and throw failure up to the availability algorithm.
                throw new IllegalStateException("HTTP 429 Too Many Requests caught during purchasing.");
            case INSUFFICIENT_BALANCE:
                logger.debug("Failed purchasing a participation worth {} CZK. We don't have enough account balance.",
                             amount);
                tenant.setKnownBalanceUpperBound(amount.longValue() - 1);
                break;
            case ALREADY_HAVE_INVESTMENT:
                logger.debug("Failed purchasing a participation worth {} CZK. Someone's beaten us to it.", amount);
                break;
            default:
                logger.debug("Failed purchasing a participation worth {} CZK. Reason unknown.", amount);
        }
        return false;
    }

    @Override
    protected boolean accept(final RecommendedParticipation recommendation) {
        if (!isBalanceAcceptable(recommendation)) {
            logger.debug("Will not purchase {} due to balance ({} CZK) likely too low.", recommendation,
                         tenant.getKnownBalanceUpperBound());
            return false;
        }
        final Participation participation = recommendation.descriptor().item();
        final Loan l = recommendation.descriptor().related();
        final boolean succeeded = tenant.getSessionInfo().isDryRun() || actualPurchase(participation);
        final Investment i = Investment.fresh(participation, l, recommendation.amount());
        discard(recommendation.descriptor());
        if (succeeded) {
            result.add(i);
            tenant.getPortfolio().simulateCharge(i.getLoanId(), i.getRating(), i.getRemainingPrincipal());
            tenant.setKnownBalanceUpperBound(tenant.getKnownBalanceUpperBound() - recommendation.amount().longValue());
            tenant.fire(investmentPurchasedLazy(() -> investmentPurchased(i, l, tenant.getPortfolio().getOverview())));
        }
        return succeeded;
    }
}
