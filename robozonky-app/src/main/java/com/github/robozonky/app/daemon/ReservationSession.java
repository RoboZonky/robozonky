/*
 * Copyright 2021 The RoboZonky Project
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

import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.robozonky.api.remote.entities.Reservation;
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
        var session = new ReservationSession(loans, tenant);
        session.tenant.fire(reservationCheckStarted(tenant.getPortfolio()
            .getOverview()));
        session.process(strategy);
        // make sure we get fresh portfolio reference here
        session.tenant.fire(reservationCheckCompleted(tenant.getPortfolio()
            .getOverview()));
        return session.getResult();
    }

    private void process(final ReservationStrategy strategy) {
        var reservations = getAvailable()
            .collect(Collectors.groupingBy(i -> strategy.recommend(i, () -> tenant.getPortfolio()
                .getOverview(), tenant.getSessionInfo())));
        // All reservations recommended by the strategy will be accepted as long as we have sufficient account balance.
        reservations.getOrDefault(true, Collections.emptyList())
            .stream()
            .map(RecommendedReservation::new)
            .takeWhile(this::isBalanceAcceptable) // no need to try if we don't have enough money
            .forEach(this::accept); // keep trying until investment opportunities are exhausted
        // All reservations rejected by the strategy will be rejected remotely.
        reservations.getOrDefault(false, Collections.emptyList())
            .forEach(this::reject);
    }

    private boolean actuallyAccept(final ReservationDescriptor descriptor) {
        var loanId = descriptor.item()
            .getId();
        try {
            tenant.run(z -> z.accept(descriptor.item()));
            logger.info("Accepted reservation of loan #{}.", loanId);
            return true;
        } catch (final Exception ex) { // TODO distinguish between low balance and other rare causes of failure
            logger.debug("Failed accepting reservation of loan #{}.", loanId, ex);
            return false;
        }
    }

    private boolean reject(final ReservationDescriptor descriptor) {
        var loanId = descriptor.item()
            .getId();
        if (tenant.getSessionInfo()
            .isDryRun()) {
            logger.info("Not rejecting reservation of loan #{} due to dry run.", loanId);
            return true;
        }
        try {
            tenant.run(z -> z.reject(descriptor.item()));
            logger.info("Rejected reservation of loan #{}.", loanId);
            return true;
        } catch (final Exception ex) { // TODO distinguish between low balance and other rare causes of failure
            logger.debug("Failed rejecting reservation of loan #{}.", loanId, ex);
            return false;
        }
    }

    @Override
    protected boolean accept(final RecommendedReservation recommendation) {
        var descriptor = recommendation.descriptor();
        var reservation = descriptor.item();
        var loanId = reservation.getId();
        if (!isBalanceAcceptable(recommendation)) {
            logger.debug("Will not accept reservation of loan #{} due to balance ({}) likely too low.", loanId,
                    tenant.getKnownBalanceUpperBound());
            return false;
        }
        logger.debug("Will attempt to accept reservation of loan #{}.", loanId);
        var amount = recommendation.amount();
        var isSuccess = tenant.getSessionInfo()
            .isDryRun() || actuallyAccept(descriptor);
        if (!isSuccess) {
            tenant.setKnownBalanceUpperBound(amount.subtract(1));
            return false;
        }
        result.add(reservation);
        tenant.getPortfolio()
            .simulateCharge(loanId, reservation.getInterestRate(), amount);
        tenant.setKnownBalanceUpperBound(tenant.getKnownBalanceUpperBound()
            .subtract(amount));
        tenant.fire(reservationAcceptedLazy(() -> reservationAccepted(recommendation.descriptor()
            .related(), amount,
                tenant.getPortfolio()
                    .getOverview())));
        return true;
    }
}
