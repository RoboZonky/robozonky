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

import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.entities.sanitized.MarketplaceLoan;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.api.strategies.RecommendedReservation;
import com.github.robozonky.api.strategies.ReservationDescriptor;
import com.github.robozonky.api.strategies.ReservationStrategy;
import com.github.robozonky.app.tenant.PowerTenant;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.github.robozonky.app.events.impl.EventFactory.reservationAcceptationRecommended;
import static com.github.robozonky.app.events.impl.EventFactory.reservationAccepted;
import static com.github.robozonky.app.events.impl.EventFactory.reservationAcceptedLazy;
import static com.github.robozonky.app.events.impl.EventFactory.reservationCheckCompleted;
import static com.github.robozonky.app.events.impl.EventFactory.reservationCheckStarted;

/**
 * Represents a single investment session over a certain marketplace, consisting of several attempts to invest into
 * given loan.
 * <p>
 * Instances of this class are supposed to be short-lived, as the marketplace can change externally at any time.
 * Essentially, one remote marketplace check should correspond to one instance of this class.
 */
final class ReservationSession {

    private static final Logger LOGGER = LogManager.getLogger(ReservationSession.class);
    private final Collection<ReservationDescriptor> reservationsStillAvailable;
    private final List<Investment> reservationsAccepted = new ArrayList<>(0);
    private final SessionState<ReservationDescriptor> seen;
    private final PowerTenant tenant;

    ReservationSession(final Collection<ReservationDescriptor> marketplace, final PowerTenant tenant) {
        this.tenant = tenant;
        this.seen = new SessionState<>(tenant, marketplace, d -> d.item().getId(), "seenReservations");
        this.reservationsStillAvailable = new ArrayList<>(marketplace);
    }

    public static Collection<Investment> process(final PowerTenant tenant,
                                                 final Collection<ReservationDescriptor> loans,
                                                 final ReservationStrategy strategy) {
        final ReservationSession s = new ReservationSession(loans, tenant);
        final PortfolioOverview portfolioOverview = tenant.getPortfolio().getOverview();
        final long balance = portfolioOverview.getCzkAvailable().longValue();
        s.tenant.fire(reservationCheckStarted(loans, portfolioOverview));
        if (balance >= tenant.getRestrictions().getMinimumInvestmentAmount() && !s.getAvailable().isEmpty()) {
            s.process(strategy);
        }
        // make sure we get fresh portfolio reference here
        s.tenant.fire(reservationCheckCompleted(s.reservationsAccepted, tenant.getPortfolio().getOverview()));
        return Collections.unmodifiableCollection(s.reservationsAccepted);
    }

    private void process(final ReservationStrategy strategy) {
        boolean invested;
        do {
            invested = strategy.recommend(getAvailable(), tenant.getPortfolio().getOverview(), tenant.getRestrictions())
                    .filter(r -> tenant.getPortfolio().getBalance().compareTo(r.amount()) >= 0)
                    .peek(r -> tenant.fire(reservationAcceptationRecommended(r)))
                    .anyMatch(this::accept); // keep trying until investment opportunities are exhausted
        } while (invested);
    }

    Collection<ReservationDescriptor> getAvailable() {
        reservationsStillAvailable.removeIf(seen::contains);
        return Collections.unmodifiableCollection(reservationsStillAvailable);
    }

    private boolean actuallyAccept(final RecommendedReservation recommendation) {
        final ReservationDescriptor loan = recommendation.descriptor();
        final int loanId = loan.item().getId();
        try {
            tenant.run(z -> z.accept(recommendation.descriptor().item()));
            LOGGER.info("Accepted reservation of loan #{}.", loanId);
            return true;
        } catch (final Exception ex) {
            LOGGER.debug("Failed accepting reservation of loan #{}.", loanId, ex);
            return false;
        }
    }

    boolean accept(final RecommendedReservation recommendation) {
        LOGGER.debug("Will attempt to accept reservation {}.", recommendation);
        final boolean succeeded = tenant.getSessionInfo().isDryRun() || actuallyAccept(recommendation);
        skip(recommendation.descriptor()); // never show again
        if (!succeeded) {
            return false;
        }
        final int confirmedAmount = recommendation.amount().intValue();
        final MarketplaceLoan l = recommendation.descriptor().related();
        final Investment i = Investment.fresh(l, confirmedAmount);
        markSuccessfulInvestment(i);
        tenant.fire(reservationAcceptedLazy(() -> reservationAccepted(i, l, tenant.getPortfolio().getOverview())));
        return true;
    }

    private void markSuccessfulInvestment(final Investment i) {
        reservationsAccepted.add(i);
        tenant.getPortfolio().simulateCharge(i.getLoanId(), i.getRating(), i.getOriginalPrincipal());
    }

    private void skip(final ReservationDescriptor reservations) {
        seen.put(reservations);
    }
}
