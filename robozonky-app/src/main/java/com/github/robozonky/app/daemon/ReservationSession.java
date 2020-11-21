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

import static com.github.robozonky.app.events.impl.EventFactory.reservationAccepted;
import static com.github.robozonky.app.events.impl.EventFactory.reservationAcceptedLazy;
import static com.github.robozonky.app.events.impl.EventFactory.reservationCheckCompleted;
import static com.github.robozonky.app.events.impl.EventFactory.reservationCheckStarted;

import java.util.Collection;
import java.util.stream.Stream;

import com.github.robozonky.api.remote.entities.Reservation;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.api.strategies.RecommendedReservation;
import com.github.robozonky.api.strategies.ReservationDescriptor;
import com.github.robozonky.api.strategies.ReservationStrategy;
import com.github.robozonky.app.tenant.PowerTenant;

/**
 * Represents a single investment session over a certain marketplace, consisting of several attempts to invest into
 * given loan.
 * <p>
 * Instances of this class are supposed to be short-lived, as the marketplace can change externally at any time.
 * Essentially, one remote marketplace check should correspond to one instance of this class.
 */
final class ReservationSession extends AbstractSession<RecommendedReservation, ReservationDescriptor, Reservation> {

    ReservationSession(final Stream<ReservationDescriptor> marketplace, final PowerTenant tenant) {
        super(marketplace, tenant, d -> d.item()
            .getId(), "seenReservations", Audit.reservations());
    }

    public static Stream<Reservation> process(final PowerTenant tenant, final Stream<ReservationDescriptor> loans,
            final ReservationStrategy strategy) {
        final ReservationSession s = new ReservationSession(loans, tenant);
        final Collection<ReservationDescriptor> c = s.getAvailable();
        if (c.isEmpty()) {
            s.logger.debug("Skipping reservations as none are available.");
            return Stream.empty();
        }
        s.tenant.fire(reservationCheckStarted(tenant.getPortfolio()
            .getOverview()));
        s.process(strategy);
        // make sure we get fresh portfolio reference here
        s.tenant.fire(reservationCheckCompleted(tenant.getPortfolio()
            .getOverview()));
        return s.getResult();
    }

    private void process(final ReservationStrategy strategy) {
        boolean invested;
        do {
            PortfolioOverview portfolioOverview = tenant.getPortfolio()
                .getOverview();
            invested = strategy.recommend(getAvailable().stream(), portfolioOverview, tenant.getSessionInfo())
                .filter(this::isBalanceAcceptable) // no need to try if we don't have enough money
                .anyMatch(this::accept); // keep trying until investment opportunities are exhausted
        } while (invested);
    }

    private boolean actuallyAccept(final RecommendedReservation recommendation) {
        final ReservationDescriptor loan = recommendation.descriptor();
        final int loanId = loan.item()
            .getId();
        try {
            tenant.run(z -> z.accept(recommendation.descriptor()
                .item()));
            logger.info("Accepted reservation of loan #{}.", loanId);
            return true;
        } catch (final Exception ex) { // TODO distinguish between low balance and other rare causes of failure
            logger.debug("Failed accepting reservation of loan #{}.", loanId, ex);
            return false;
        }
    }

    @Override
    protected boolean accept(final RecommendedReservation recommendation) {
        if (!isBalanceAcceptable(recommendation)) {
            logger.debug("Will not accept reservation {} due to balance ({}) likely too low.", recommendation,
                    tenant.getKnownBalanceUpperBound());
            return false;
        }
        logger.debug("Will attempt to accept reservation {}.", recommendation);
        final boolean succeeded = tenant.getSessionInfo()
            .isDryRun() || actuallyAccept(recommendation);
        discard(recommendation.descriptor()); // never show again
        if (!succeeded) {
            tenant.setKnownBalanceUpperBound(recommendation.amount()
                .subtract(1));
            return false;
        }
        final Reservation reservation = recommendation.descriptor()
            .item();
        result.add(recommendation.descriptor()
            .item());
        tenant.getPortfolio()
            .simulateCharge(reservation.getId(), reservation.getRating(), recommendation.amount());
        tenant.setKnownBalanceUpperBound(tenant.getKnownBalanceUpperBound()
            .subtract(recommendation.amount()));
        tenant.fire(reservationAcceptedLazy(() -> reservationAccepted(recommendation.descriptor()
            .related(),
                recommendation.amount(),
                tenant.getPortfolio()
                    .getOverview())));
        return true;
    }
}
