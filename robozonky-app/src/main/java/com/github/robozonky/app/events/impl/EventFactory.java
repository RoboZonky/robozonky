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

package com.github.robozonky.app.events.impl;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.function.Supplier;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.notifications.Event;
import com.github.robozonky.api.notifications.ExecutionCompletedEvent;
import com.github.robozonky.api.notifications.ExecutionStartedEvent;
import com.github.robozonky.api.notifications.InvestmentMadeEvent;
import com.github.robozonky.api.notifications.InvestmentPurchasedEvent;
import com.github.robozonky.api.notifications.InvestmentSoldEvent;
import com.github.robozonky.api.notifications.LoanDefaultedEvent;
import com.github.robozonky.api.notifications.LoanDelinquent10DaysOrMoreEvent;
import com.github.robozonky.api.notifications.LoanDelinquent30DaysOrMoreEvent;
import com.github.robozonky.api.notifications.LoanDelinquent60DaysOrMoreEvent;
import com.github.robozonky.api.notifications.LoanDelinquent90DaysOrMoreEvent;
import com.github.robozonky.api.notifications.LoanLostEvent;
import com.github.robozonky.api.notifications.LoanNoLongerDelinquentEvent;
import com.github.robozonky.api.notifications.LoanNowDelinquentEvent;
import com.github.robozonky.api.notifications.LoanRecommendedEvent;
import com.github.robozonky.api.notifications.PurchaseRecommendedEvent;
import com.github.robozonky.api.notifications.PurchasingCompletedEvent;
import com.github.robozonky.api.notifications.PurchasingStartedEvent;
import com.github.robozonky.api.notifications.ReservationAcceptationRecommendedEvent;
import com.github.robozonky.api.notifications.ReservationAcceptedEvent;
import com.github.robozonky.api.notifications.ReservationCheckCompletedEvent;
import com.github.robozonky.api.notifications.ReservationCheckStartedEvent;
import com.github.robozonky.api.notifications.RoboZonkyCrashedEvent;
import com.github.robozonky.api.notifications.RoboZonkyDaemonResumedEvent;
import com.github.robozonky.api.notifications.RoboZonkyDaemonSuspendedEvent;
import com.github.robozonky.api.notifications.RoboZonkyEndingEvent;
import com.github.robozonky.api.notifications.RoboZonkyExperimentalUpdateDetectedEvent;
import com.github.robozonky.api.notifications.RoboZonkyInitializedEvent;
import com.github.robozonky.api.notifications.RoboZonkyStartingEvent;
import com.github.robozonky.api.notifications.RoboZonkyTestingEvent;
import com.github.robozonky.api.notifications.RoboZonkyUpdateDetectedEvent;
import com.github.robozonky.api.notifications.SaleOfferedEvent;
import com.github.robozonky.api.notifications.SaleRecommendedEvent;
import com.github.robozonky.api.notifications.SellingCompletedEvent;
import com.github.robozonky.api.notifications.SellingStartedEvent;
import com.github.robozonky.api.notifications.WeeklySummaryEvent;
import com.github.robozonky.api.remote.entities.Investment;
import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.api.remote.entities.Participation;
import com.github.robozonky.api.remote.entities.Reservation;
import com.github.robozonky.api.strategies.ExtendedPortfolioOverview;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.api.strategies.RecommendedInvestment;
import com.github.robozonky.api.strategies.RecommendedLoan;
import com.github.robozonky.api.strategies.RecommendedParticipation;
import com.github.robozonky.api.strategies.RecommendedReservation;
import com.github.robozonky.internal.remote.entities.LoanImpl;
import com.github.robozonky.internal.tenant.LazyEvent;

/**
 * Events which require an instance of {@link PortfolioOverview} or {@link LoanImpl} are prime candidates for lazy
 * instantiation, as retrieving it may incur some fairly heavy logic, incl. network requests.
 */
public final class EventFactory {

    private EventFactory() {
        // no instances
    }

    public static ExecutionCompletedEvent executionCompleted(final Collection<Loan> investments,
            final PortfolioOverview portfolioOverview) {
        return new ExecutionCompletedEventImpl(investments, portfolioOverview);
    }

    public static ExecutionStartedEvent executionStarted(final PortfolioOverview portfolioOverview) {
        return new ExecutionStartedEventImpl(portfolioOverview);
    }

    public static InvestmentMadeEvent investmentMade(final Loan loan, final Money investedAmount,
            final PortfolioOverview portfolioOverview) {
        return new InvestmentMadeEventImpl(loan, investedAmount, portfolioOverview);
    }

    public static InvestmentPurchasedEvent investmentPurchased(final Participation participation, final Loan loan,
            final Money purchasedAmount,
            final PortfolioOverview portfolioOverview) {
        return new InvestmentPurchasedEventImpl(participation, loan, purchasedAmount, portfolioOverview);
    }

    public static InvestmentSoldEvent investmentSold(final Investment investment, final Loan loan,
            final PortfolioOverview portfolioOverview) {
        return new InvestmentSoldEventImpl(investment, loan, portfolioOverview);
    }

    public static LoanDefaultedEvent loanDefaulted(final Investment investment, final Loan loan,
            final LocalDate since) {
        return new LoanDefaultedEventImpl(investment, loan, since);
    }

    public static LoanNowDelinquentEvent loanNowDelinquent(final Investment investment, final Loan loan) {
        return new LoanNowDelinquentEventImpl(investment, loan);
    }

    public static LoanDelinquent10DaysOrMoreEvent loanDelinquent10plus(final Investment investment, final Loan loan) {
        return new LoanDelinquent10DaysOrMoreEventImpl(investment, loan);
    }

    public static LoanDelinquent30DaysOrMoreEvent loanDelinquent30plus(final Investment investment, final Loan loan) {
        return new LoanDelinquent30DaysOrMoreEventImpl(investment, loan);
    }

    public static LoanDelinquent60DaysOrMoreEvent loanDelinquent60plus(final Investment investment, final Loan loan) {
        return new LoanDelinquent60DaysOrMoreEventImpl(investment, loan);
    }

    public static LoanDelinquent90DaysOrMoreEvent loanDelinquent90plus(final Investment investment, final Loan loan) {
        return new LoanDelinquent90DaysOrMoreEventImpl(investment, loan);
    }

    public static LoanLostEvent loanLost(final Investment investment, final Loan loan) {
        return new LoanLostEventImpl(investment, loan);
    }

    public static LoanNoLongerDelinquentEvent loanNoLongerDelinquent(final Investment investment, final Loan loan) {
        return new LoanNoLongerDelinquentEventImpl(investment, loan);
    }

    public static LoanRecommendedEvent loanRecommended(final RecommendedLoan recommendation) {
        return new LoanRecommendedEventImpl(recommendation);
    }

    public static PurchaseRecommendedEvent purchaseRecommended(final RecommendedParticipation recommendation) {
        return new PurchaseRecommendedEventImpl(recommendation);
    }

    public static PurchasingCompletedEvent purchasingCompleted(final Collection<Participation> investment,
            final PortfolioOverview portfolio) {
        return new PurchasingCompletedEventImpl(investment, portfolio);
    }

    public static PurchasingStartedEvent purchasingStarted(final PortfolioOverview portfolio) {
        return new PurchasingStartedEventImpl(portfolio);
    }

    public static RoboZonkyCrashedEvent roboZonkyCrashed(final Throwable cause) {
        return new RoboZonkyCrashedEventImpl(cause);
    }

    public static RoboZonkyDaemonSuspendedEvent roboZonkyDaemonSuspended(final Exception cause) {
        return new RoboZonkyDaemonSuspendedEventImpl(cause);
    }

    public static RoboZonkyDaemonResumedEvent roboZonkyDaemonResumed(final ZonedDateTime since,
            final ZonedDateTime until) {
        return new RoboZonkyDaemonResumedEventImpl(since, until);
    }

    public static RoboZonkyEndingEvent roboZonkyEnding() {
        return new RoboZonkyEndingEventImpl();
    }

    public static RoboZonkyExperimentalUpdateDetectedEvent roboZonkyExperimentalUpdateDetected(final String version) {
        return new RoboZonkyExperimentalUpdateDetectedEventImpl(version);
    }

    public static RoboZonkyInitializedEvent roboZonkyInitialized() {
        return new RoboZonkyInitializedEventImpl();
    }

    public static RoboZonkyStartingEvent roboZonkyStarting() {
        return new RoboZonkyStartingEventImpl();
    }

    public static RoboZonkyTestingEvent roboZonkyTesting() {
        return new RoboZonkyTestingEventImpl();
    }

    public static WeeklySummaryEvent weeklySummary(final ExtendedPortfolioOverview portfolioOverview) {
        return new WeeklySummaryEventImpl(portfolioOverview);
    }

    public static RoboZonkyUpdateDetectedEvent roboZonkyUpdateDetected(final String version) {
        return new RoboZonkyUpdateDetectedEventImpl(version);
    }

    public static SaleOfferedEvent saleOffered(final Investment investment, final Loan loan) {
        return new SaleOfferedEventImpl(investment, loan);
    }

    public static SaleRecommendedEvent saleRecommended(final RecommendedInvestment recommendation) {
        return new SaleRecommendedEventImpl(recommendation);
    }

    public static SellingCompletedEvent sellingCompleted(final Collection<Investment> investments,
            final PortfolioOverview portfolio) {
        return new SellingCompletedEventImpl(investments, portfolio);
    }

    public static SellingStartedEvent sellingStarted(final PortfolioOverview portfolio) {
        return new SellingStartedEventImpl(portfolio);
    }

    public static ReservationCheckStartedEvent reservationCheckStarted(final PortfolioOverview portfolioOverview) {
        return new ReservationCheckStartedEventImpl(portfolioOverview);
    }

    public static ReservationCheckCompletedEvent reservationCheckCompleted(final Collection<Reservation> investments,
            final PortfolioOverview portfolioOverview) {
        return new ReservationCheckCompletedEventImpl(investments, portfolioOverview);
    }

    public static ReservationAcceptationRecommendedEvent reservationAcceptationRecommended(
            final RecommendedReservation recommendation) {
        return new ReservationAcceptationRecommendedEventImpl(recommendation);
    }

    public static ReservationAcceptedEvent reservationAccepted(final Loan loan, final Money investedAmount,
            final PortfolioOverview portfolioOverview) {
        return new ReservationAcceptedEventImpl(loan, investedAmount, portfolioOverview);
    }

    public static LazyEvent<ReservationAcceptedEvent> reservationAcceptedLazy(
            final Supplier<ReservationAcceptedEvent> supplier) {
        return async(ReservationAcceptedEvent.class, supplier);
    }

    public static LazyEvent<InvestmentMadeEvent> investmentMadeLazy(final Supplier<InvestmentMadeEvent> supplier) {
        return async(InvestmentMadeEvent.class, supplier);
    }

    public static LazyEvent<InvestmentPurchasedEvent> investmentPurchasedLazy(
            final Supplier<InvestmentPurchasedEvent> supplier) {
        return async(InvestmentPurchasedEvent.class, supplier);
    }

    public static LazyEvent<InvestmentSoldEvent> investmentSoldLazy(final Supplier<InvestmentSoldEvent> supplier) {
        return async(InvestmentSoldEvent.class, supplier);
    }

    public static LazyEvent<LoanDefaultedEvent> loanDefaultedLazy(final Supplier<LoanDefaultedEvent> supplier) {
        return async(LoanDefaultedEvent.class, supplier);
    }

    public static LazyEvent<LoanNowDelinquentEvent> loanNowDelinquentLazy(
            final Supplier<LoanNowDelinquentEvent> supplier) {
        return async(LoanNowDelinquentEvent.class, supplier);
    }

    public static LazyEvent<LoanDelinquent10DaysOrMoreEvent> loanDelinquent10plusLazy(
            final Supplier<LoanDelinquent10DaysOrMoreEvent> supplier) {
        return async(LoanDelinquent10DaysOrMoreEvent.class, supplier);
    }

    public static LazyEvent<LoanDelinquent30DaysOrMoreEvent> loanDelinquent30plusLazy(
            final Supplier<LoanDelinquent30DaysOrMoreEvent> supplier) {
        return async(LoanDelinquent30DaysOrMoreEvent.class, supplier);
    }

    public static LazyEvent<LoanDelinquent60DaysOrMoreEvent> loanDelinquent60plusLazy(
            final Supplier<LoanDelinquent60DaysOrMoreEvent> supplier) {
        return async(LoanDelinquent60DaysOrMoreEvent.class, supplier);
    }

    public static LazyEvent<LoanDelinquent90DaysOrMoreEvent> loanDelinquent90plusLazy(
            final Supplier<LoanDelinquent90DaysOrMoreEvent> supplier) {
        return async(LoanDelinquent90DaysOrMoreEvent.class, supplier);
    }

    public static LazyEvent<LoanLostEvent> loanLostLazy(final Supplier<LoanLostEvent> supplier) {
        return async(LoanLostEvent.class, supplier);
    }

    public static LazyEvent<LoanNoLongerDelinquentEvent> loanNoLongerDelinquentLazy(
            final Supplier<LoanNoLongerDelinquentEvent> supplier) {
        return async(LoanNoLongerDelinquentEvent.class, supplier);
    }

    public static LazyEvent<ExecutionCompletedEvent> executionCompletedLazy(
            final Supplier<ExecutionCompletedEvent> supplier) {
        return async(ExecutionCompletedEvent.class, supplier);
    }

    public static LazyEvent<ExecutionStartedEvent> executionStartedLazy(
            final Supplier<ExecutionStartedEvent> supplier) {
        return async(ExecutionStartedEvent.class, supplier);
    }

    public static LazyEvent<PurchasingCompletedEvent> purchasingCompletedLazy(
            final Supplier<PurchasingCompletedEvent> supplier) {
        return async(PurchasingCompletedEvent.class, supplier);
    }

    public static LazyEvent<PurchasingStartedEvent> purchasingStartedLazy(
            final Supplier<PurchasingStartedEvent> supplier) {
        return async(PurchasingStartedEvent.class, supplier);
    }

    public static LazyEvent<SellingCompletedEvent> sellingCompletedLazy(
            final Supplier<SellingCompletedEvent> supplier) {
        return async(SellingCompletedEvent.class, supplier);
    }

    public static <T extends Event> LazyEvent<T> async(final Class<T> clz, final Supplier<T> eventSupplier) {
        return new LazyEventImpl<>(clz, eventSupplier);
    }
}
