/*
 * Copyright 2020 The RoboZonky Project
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

import static com.github.robozonky.app.events.impl.EventFactory.investmentPurchased;
import static com.github.robozonky.app.events.impl.EventFactory.investmentPurchasedLazy;
import static com.github.robozonky.app.events.impl.EventFactory.purchasingCompleted;
import static com.github.robozonky.app.events.impl.EventFactory.purchasingCompletedLazy;
import static com.github.robozonky.app.events.impl.EventFactory.purchasingStarted;
import static com.github.robozonky.app.events.impl.EventFactory.purchasingStartedLazy;

import java.util.stream.Stream;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;

import com.github.robozonky.api.remote.entities.Participation;
import com.github.robozonky.api.strategies.ParticipationDescriptor;
import com.github.robozonky.api.strategies.PurchaseStrategy;
import com.github.robozonky.app.tenant.PowerTenant;

/**
 * Represents a single session over secondary marketplace, consisting of several attempts to purchase participations.
 * <p>
 * Instances of this class are supposed to be short-lived, as the marketplace and Zonky account balance can change
 * externally at any time. Essentially, one remote marketplace check should correspond to one instance of this class.
 */
final class PurchasingSession extends
        AbstractSession<RecommendedParticipation, ParticipationDescriptor, Participation> {

    PurchasingSession(final Stream<ParticipationDescriptor> marketplace, final PowerTenant tenant) {
        super(marketplace, tenant, d -> d.item()
            .getId(), "discardedParticipations", Audit.purchasing());
    }

    public static Stream<Participation> purchase(final PowerTenant auth, final Stream<ParticipationDescriptor> items,
            final PurchaseStrategy strategy) {
        final PurchasingSession s = new PurchasingSession(items, auth);
        s.tenant.fire(purchasingStartedLazy(() -> purchasingStarted(auth.getPortfolio()
            .getOverview())));
        s.purchase(strategy);
        s.tenant.fire(purchasingCompletedLazy(() -> purchasingCompleted(auth.getPortfolio()
            .getOverview())));
        return s.getResult();
    }

    private void purchase(final PurchaseStrategy strategy) {
        getAvailable()
            .filter(i -> strategy.recommend(i, () -> tenant.getPortfolio()
                .getOverview(), tenant.getSessionInfo()))
            .map(RecommendedParticipation::new)
            .takeWhile(this::isBalanceAcceptable) // no need to try if we don't have enough money
            .forEach(this::accept); // keep trying until investment opportunities are exhausted
    }

    private boolean actualPurchase(final Participation participation) {
        try {
            tenant.run(zonky -> zonky.purchase(participation));
            logger.info("Purchased a participation worth {}.", participation.getRemainingPrincipal());
            return true;
        } catch (BadRequestException ex) {
            var response = getResponseEntity(ex.getResponse());
            if (response.contains("TOO_MANY_REQUESTS")) {
                // HTTP 429 needs to terminate investing and throw failure up to the availability algorithm.
                throw new IllegalStateException("HTTP 429 Too Many Requests caught during purchasing.", ex);
            } else if (response.contains("INSUFFICIENT_BALANCE")) {
                logger.debug("Failed purchasing participation worth {}. We don't have sufficient balance.",
                        participation.getRemainingPrincipal());
                tenant.setKnownBalanceUpperBound(participation.getRemainingPrincipal()
                    .subtract(1));
                return false;
            } else if (response.contains("ALREADY_HAVE_INVESTMENT")) {
                logger.debug("Failed purchasing participation #{}, already have investment.",
                        participation.getId());
                return false;
            }
            throw new IllegalStateException("Unknown problem during purchasing. Reason given: '" + response + "'.", ex);
        } catch (NotFoundException ex) {
            logger.debug("Failed purchasing participation #{}, not found.", participation.getId());
            return false;
        } catch (Exception ex) {
            throw new IllegalStateException("Unknown problem during purchasing.", ex);
        }
    }

    @Override
    protected boolean accept(final RecommendedParticipation recommendation) {
        if (!isBalanceAcceptable(recommendation)) {
            logger.debug("Will not purchase {} due to balance ({}) likely too low.", recommendation,
                    tenant.getKnownBalanceUpperBound());
            return false;
        }
        final Participation participation = recommendation.descriptor()
            .item();
        final boolean succeeded = tenant.getSessionInfo()
            .isDryRun() || actualPurchase(participation);
        discard(recommendation.descriptor());
        if (succeeded) {
            result.add(participation);
            tenant.getPortfolio()
                .simulateCharge(participation.getLoanId(), participation.getInterestRate(), recommendation.amount());
            tenant.setKnownBalanceUpperBound(tenant.getKnownBalanceUpperBound()
                .subtract(recommendation.amount()));
            tenant.fire(investmentPurchasedLazy(() -> investmentPurchased(participation,
                    recommendation.descriptor()
                        .related(),
                    recommendation.amount(),
                    tenant.getPortfolio()
                        .getOverview())));
        }
        return succeeded;
    }
}
