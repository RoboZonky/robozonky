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
import com.github.robozonky.api.remote.ControlApi;
import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.entities.sanitized.MarketplaceLoan;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.api.strategies.RecommendedReservation;
import com.github.robozonky.api.strategies.ReservationDescriptor;
import com.github.robozonky.api.strategies.ReservationStrategy;
import com.github.robozonky.app.tenant.PowerTenant;
import com.github.robozonky.common.tenant.Tenant;
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
 * Instances of this class are supposed to be short-lived, as the marketplace and Zonky account balance can change
 * externally at any time. Essentially, one remote marketplace check should correspond to one instance of this class.
 */
final class ReservationSession {

    private static final Logger LOGGER = LogManager.getLogger(ReservationSession.class);
    private final Collection<ReservationDescriptor> reservationsStillAvailable;
    private final List<Investment> investmentsMadeNow = new ArrayList<>(0);
    private final SessionState<ReservationDescriptor> discarded, seen;
    private final PowerTenant tenant;

    ReservationSession(final Collection<ReservationDescriptor> marketplace,
                       final PowerTenant tenant) {
        this.tenant = tenant;
        this.discarded = newSessionState(tenant, marketplace, "discardedReservations");
        this.seen = newSessionState(tenant, marketplace, "seenReservations");
        this.reservationsStillAvailable = new ArrayList<>(marketplace);
    }

    private static SessionState<ReservationDescriptor> newSessionState(final Tenant tenant,
                                                                       final Collection<ReservationDescriptor> marketplace,
                                                                       final String key) {
        return new SessionState<>(tenant, marketplace, d -> d.item().getId(), key);
    }

    public static Collection<Investment> invest(final PowerTenant tenant, final Collection<ReservationDescriptor> loans,
                                                final ReservationStrategy strategy) {
        final ReservationSession s = new ReservationSession(loans, tenant);
        final PortfolioOverview portfolioOverview = tenant.getPortfolio().getOverview();
        final long balance = portfolioOverview.getCzkAvailable().longValue();
        s.tenant.fire(reservationCheckStarted(loans, portfolioOverview));
        if (balance >= tenant.getRestrictions().getMinimumInvestmentAmount() && !s.getAvailable().isEmpty()) {
            s.invest(strategy);
        }
        final Collection<Investment> result = s.getResult();
        // make sure we get fresh portfolio reference here
        s.tenant.fire(reservationCheckCompleted(result, tenant.getPortfolio().getOverview()));
        return Collections.unmodifiableCollection(result);
    }

    private void invest(final ReservationStrategy strategy) {
        boolean invested;
        do {
            invested = strategy.recommend(getAvailable(), tenant.getPortfolio().getOverview(), tenant.getRestrictions())
                    .peek(r -> tenant.fire(reservationAcceptationRecommended(r)))
                    .anyMatch(this::invest); // keep trying until investment opportunities are exhausted
        } while (invested);
    }

    /**
     * Get loans that are available to be evaluated by the strategy. These are loans that come from the marketplace,
     * minus loans that are already invested into or discarded due to the {@link ConfirmationProvider} mechanism.
     * @return Loans in the marketplace in which the user could potentially invest. Unmodifiable.
     */
    Collection<ReservationDescriptor> getAvailable() {
        reservationsStillAvailable.removeIf(d -> seen.contains(d) || discarded.contains(d));
        return Collections.unmodifiableCollection(reservationsStillAvailable);
    }

    /**
     * Get investments made during this session.
     * @return Investments made so far during this session. Unmodifiable.
     */
    List<Investment> getResult() {
        return Collections.unmodifiableList(investmentsMadeNow);
    }

    private boolean actualPurchase(final RecommendedReservation recommendation) {
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

    /**
     * Request {@link ControlApi} to accept reservation in a given loan.
     * @param recommendation Loan to invest into.
     * @return True if investment successful. The investment is reflected in {@link #getResult()}.
     */
    boolean invest(final RecommendedReservation recommendation) {
        LOGGER.debug("Will attempt to accept reservation {}.", recommendation);
        if (tenant.getPortfolio().getBalance().compareTo(recommendation.amount()) < 0) {
            // should not be allowed by the calling code
            LOGGER.debug("Balance was less than recommendation.");
            return false;
        }
        final boolean succeeded = tenant.getSessionInfo().isDryRun() || actualPurchase(recommendation);
        if (succeeded) {
            final int confirmedAmount = recommendation.amount().intValue();
            final MarketplaceLoan l = recommendation.descriptor().related();
            final Investment i = Investment.fresh(l, confirmedAmount);
            markSuccessfulInvestment(i);
            discard(recommendation.descriptor()); // never show again
            tenant.fire(reservationAcceptedLazy(() -> reservationAccepted(i, l, tenant.getPortfolio().getOverview())));
            return true;
        } else {
            skip(recommendation.descriptor());
            return false;
        }
    }

    private void markSuccessfulInvestment(final Investment i) {
        investmentsMadeNow.add(i);
        tenant.getPortfolio().simulateCharge(i.getLoanId(), i.getRating(), i.getOriginalPrincipal());
    }

    private void discard(final ReservationDescriptor loan) {
        skip(loan);
        discarded.put(loan);
    }

    private void skip(final ReservationDescriptor loan) {
        seen.put(loan);
    }
}
