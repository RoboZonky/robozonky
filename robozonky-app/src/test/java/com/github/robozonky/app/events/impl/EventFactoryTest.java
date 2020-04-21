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

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.notifications.ExecutionStartedEvent;
import com.github.robozonky.api.notifications.InvestmentMadeEvent;
import com.github.robozonky.api.notifications.InvestmentPurchasedEvent;
import com.github.robozonky.api.notifications.InvestmentSoldEvent;
import com.github.robozonky.api.notifications.LoanDefaultedEvent;
import com.github.robozonky.api.notifications.LoanDelinquentEvent;
import com.github.robozonky.api.notifications.LoanLostEvent;
import com.github.robozonky.api.notifications.LoanNoLongerDelinquentEvent;
import com.github.robozonky.api.notifications.LoanNowDelinquentEvent;
import com.github.robozonky.api.notifications.LoanRecommendedEvent;
import com.github.robozonky.api.notifications.PurchaseRecommendedEvent;
import com.github.robozonky.api.notifications.PurchasingStartedEvent;
import com.github.robozonky.api.notifications.ReservationAcceptationRecommendedEvent;
import com.github.robozonky.api.notifications.ReservationAcceptedEvent;
import com.github.robozonky.api.notifications.ReservationCheckStartedEvent;
import com.github.robozonky.api.notifications.RoboZonkyCrashedEvent;
import com.github.robozonky.api.notifications.RoboZonkyDaemonResumedEvent;
import com.github.robozonky.api.notifications.RoboZonkyDaemonSuspendedEvent;
import com.github.robozonky.api.notifications.RoboZonkyTestingEvent;
import com.github.robozonky.api.notifications.SaleOfferedEvent;
import com.github.robozonky.api.notifications.SaleRecommendedEvent;
import com.github.robozonky.api.notifications.SellingStartedEvent;
import com.github.robozonky.api.notifications.WeeklySummaryEvent;
import com.github.robozonky.api.remote.entities.Investment;
import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.api.remote.entities.MyReservation;
import com.github.robozonky.api.remote.entities.Participation;
import com.github.robozonky.api.remote.entities.Reservation;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.ExtendedPortfolioOverview;
import com.github.robozonky.api.strategies.InvestmentDescriptor;
import com.github.robozonky.api.strategies.LoanDescriptor;
import com.github.robozonky.api.strategies.ParticipationDescriptor;
import com.github.robozonky.api.strategies.RecommendedInvestment;
import com.github.robozonky.api.strategies.RecommendedLoan;
import com.github.robozonky.api.strategies.RecommendedParticipation;
import com.github.robozonky.api.strategies.RecommendedReservation;
import com.github.robozonky.api.strategies.ReservationDescriptor;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.internal.remote.entities.LoanImpl;
import com.github.robozonky.internal.remote.entities.MyReservationImpl;
import com.github.robozonky.internal.remote.entities.ParticipationImpl;
import com.github.robozonky.internal.remote.entities.ReservationImpl;
import com.github.robozonky.test.mock.MockInvestmentBuilder;
import com.github.robozonky.test.mock.MockLoanBuilder;
import com.github.robozonky.test.mock.MockReservationBuilder;

class EventFactoryTest extends AbstractZonkyLeveragingTest {

    private static RecommendedLoan recommendedLoan() {
        final LoanImpl loan = new MockLoanBuilder().setNonReservedRemainingInvestment(2000)
            .build();
        return new LoanDescriptor(loan).recommend(Money.from(200))
            .orElse(null);
    }

    private static RecommendedParticipation recommendedParticipation() {
        final Participation p = mock(ParticipationImpl.class);
        when(p.getRemainingPrincipal()).thenReturn(Money.from(10));
        return new ParticipationDescriptor(p, MockLoanBuilder::fresh).recommend()
            .orElse(null);
    }

    private static RecommendedInvestment recommendedInvestment() {
        return new InvestmentDescriptor(MockInvestmentBuilder.fresh()
            .setRemainingPrincipal(BigDecimal.TEN)
            .build(),
                MockLoanBuilder::fresh).recommend()
                    .orElse(null);
    }

    private static RecommendedReservation recommendedReservation() {
        final MyReservation mr = mock(MyReservationImpl.class);
        when(mr.getReservedAmount()).thenReturn(Money.from(200));
        final Reservation r = new MockReservationBuilder()
            .setMyReservation(mr)
            .build();
        final Loan l = MockLoanBuilder.fresh();
        return new ReservationDescriptor(r, () -> l)
            .recommend(r.getMyReservation()
                .getReservedAmount())
            .orElse(null);
    }

    private static void assertCorrectThreshold(final LoanDelinquentEvent e, final int threshold) {
        assertThat(e.getThresholdInDays()).isEqualTo(threshold);
    }

    @Test
    void thresholds() {
        final Loan loan = new MockLoanBuilder().setRating(Rating.D)
            .setAmount(100_000)
            .build();
        final Investment investment = MockInvestmentBuilder.fresh(loan, BigDecimal.TEN)
            .build();
        final LocalDate now = LocalDate.now();
        final LoanDelinquentEvent e = EventFactory.loanDelinquent90plus(investment, loan, now);
        assertCorrectThreshold(e, 90);
        final LoanDelinquentEvent e2 = EventFactory.loanDelinquent60plus(investment, loan, now);
        assertCorrectThreshold(e2, 60);
        final LoanDelinquentEvent e3 = EventFactory.loanDelinquent30plus(investment, loan, now);
        assertCorrectThreshold(e3, 30);
        final LoanDelinquentEvent e4 = EventFactory.loanDelinquent10plus(investment, loan, now);
        assertCorrectThreshold(e4, 10);
    }

    @Test
    void executionCompleted() {
        Loan loan = MockLoanBuilder.fresh();
        var e = EventFactory.executionCompleted(singleton(loan), mockPortfolioOverview());
        assertSoftly(softly -> {
            softly.assertThat(e.getLoansInvestedInto())
                .containsExactly(loan);
            softly.assertThat(e.getPortfolioOverview())
                .isNotNull();
            softly.assertThat(e.getCreatedOn())
                .isBeforeOrEqualTo(OffsetDateTime.now());
            softly.assertThat(e.toString())
                .isNotEmpty();
        });
    }

    @Test
    void executionStarted() {
        final ExecutionStartedEvent e = EventFactory.executionStarted(mockPortfolioOverview());
        assertThat(e.getPortfolioOverview()).isNotNull();
    }

    @Test
    void investmentMade() {
        final InvestmentMadeEvent e = EventFactory.investmentMade(MockLoanBuilder.fresh(), Money.from(200),
                mockPortfolioOverview());
        assertSoftly(softly -> {
            softly.assertThat(e.getLoan())
                .isNotNull();
            softly.assertThat(e.getInvestedAmount())
                .isEqualTo(Money.from(200));
            softly.assertThat(e.getPortfolioOverview())
                .isNotNull();
        });
    }

    @Test
    void investmentPurchased() {
        final Loan loan = MockLoanBuilder.fresh();
        final Participation participation = new ParticipationImpl(loan, Money.from(200), loan.getTermInMonths() - 1);
        final InvestmentPurchasedEvent e = EventFactory.investmentPurchased(participation, loan, Money.from(200),
                mockPortfolioOverview());
        assertSoftly(softly -> {
            softly.assertThat(e.getLoan())
                .isNotNull();
            softly.assertThat(e.getPurchasedAmount())
                .isEqualTo(Money.from(200));
            softly.assertThat(e.getPortfolioOverview())
                .isNotNull();
            softly.assertThat(e.getParticipation())
                .isNotNull();
        });
    }

    @Test
    void investmentSold() {
        final InvestmentSoldEvent e = EventFactory.investmentSold(MockInvestmentBuilder.fresh()
            .build(),
                MockLoanBuilder.fresh(), mockPortfolioOverview());
        assertSoftly(softly -> {
            softly.assertThat(e.getLoan())
                .isNotNull();
            softly.assertThat(e.getInvestment())
                .isNotNull();
            softly.assertThat(e.getPortfolioOverview())
                .isNotNull();
        });
    }

    @Test
    void loanDefaulted() {
        final LoanDefaultedEvent e = EventFactory.loanDefaulted(MockInvestmentBuilder.fresh()
            .build(),
                MockLoanBuilder.fresh(), LocalDate.now());
        assertSoftly(softly -> {
            softly.assertThat(e.getLoan())
                .isNotNull();
            softly.assertThat(e.getInvestment())
                .isNotNull();
            softly.assertThat(e.getDelinquentSince())
                .isNotNull();
        });
    }

    @Test
    void loanNoLongerDelinquent() {
        final LoanNoLongerDelinquentEvent e = EventFactory.loanNoLongerDelinquent(MockInvestmentBuilder.fresh()
            .build(),
                MockLoanBuilder.fresh());
        assertSoftly(softly -> {
            softly.assertThat(e.getLoan())
                .isNotNull();
            softly.assertThat(e.getInvestment())
                .isNotNull();
        });
    }

    @Test
    void loanNowDelinquent() {
        final LoanNowDelinquentEvent e = EventFactory.loanNowDelinquent(MockInvestmentBuilder.fresh()
            .build(),
                MockLoanBuilder.fresh(), LocalDate.now());
        assertSoftly(softly -> {
            softly.assertThat(e.getLoan())
                .isNotNull();
            softly.assertThat(e.getInvestment())
                .isNotNull();
            softly.assertThat(e.getDelinquentSince())
                .isNotNull();
            softly.assertThat(e.getThresholdInDays())
                .isEqualTo(0);
        });
    }

    @Test
    void loanLost() {
        final LoanLostEvent e = EventFactory.loanLost(MockInvestmentBuilder.fresh()
            .build(), MockLoanBuilder.fresh());
        assertSoftly(softly -> {
            softly.assertThat(e.getLoan())
                .isNotNull();
            softly.assertThat(e.getInvestment())
                .isNotNull();
        });
    }

    @Test
    void loanRecommended() {
        final LoanRecommendedEvent e = EventFactory.loanRecommended(recommendedLoan());
        assertSoftly(softly -> {
            softly.assertThat(e.getLoan())
                .isNotNull();
            softly.assertThat(e.getRecommendation())
                .isNotNull();
        });
    }

    @Test
    void purchaseRecommended() {
        final PurchaseRecommendedEvent e = EventFactory.purchaseRecommended(recommendedParticipation());
        assertSoftly(softly -> {
            softly.assertThat(e.getLoan())
                .isNotNull();
            softly.assertThat(e.getParticipation())
                .isNotNull();
            softly.assertThat(e.getRecommendation())
                .isNotNull();
        });
    }

    @Test
    void purchasingCompleted() {
        var participation = mock(ParticipationImpl.class);
        var e = EventFactory.purchasingCompleted(singleton(participation), mockPortfolioOverview());
        assertSoftly(softly -> {
            softly.assertThat(e.getParticipationsPurchased())
                .containsExactly(participation);
            softly.assertThat(e.getPortfolioOverview())
                .isNotNull();
        });
    }

    @Test
    void purchasingStarted() {
        final PurchasingStartedEvent e = EventFactory.purchasingStarted(mockPortfolioOverview());
        assertThat(e.getPortfolioOverview()).isNotNull();
    }

    @Test
    void reservationAcceptationRecommended() {
        final ReservationAcceptationRecommendedEvent e = EventFactory
            .reservationAcceptationRecommended(recommendedReservation());
        assertThat(e.getRecommendation()).isNotNull();
    }

    @Test
    void reservationAccepted() {
        final Loan l = MockLoanBuilder.fresh();
        final ReservationAcceptedEvent e = EventFactory.reservationAccepted(l, Money.from(200),
                mockPortfolioOverview());
        assertSoftly(softly -> {
            softly.assertThat(e.getLoan())
                .isNotNull();
            softly.assertThat(e.getInvestedAmount())
                .isEqualTo(Money.from(200));
            softly.assertThat(e.getPortfolioOverview())
                .isNotNull();
        });
    }

    @Test
    void reservationCheckCompleted() {
        var reservation = mock(ReservationImpl.class);
        var e = EventFactory.reservationCheckCompleted(singleton(reservation), mockPortfolioOverview());
        assertSoftly(softly -> {
            softly.assertThat(e.getReservationsAccepted())
                .containsExactly(reservation);
            softly.assertThat(e.getPortfolioOverview())
                .isNotNull();
        });
    }

    @Test
    void reservationCheckStarted() {
        final ReservationCheckStartedEvent e = EventFactory.reservationCheckStarted(mockPortfolioOverview());
        assertThat(e.getPortfolioOverview()).isNotNull();
    }

    @Test
    void robozonkyDaemonSuspended() {
        final RoboZonkyDaemonSuspendedEvent e = EventFactory.roboZonkyDaemonSuspended(new IllegalArgumentException());
        assertThat(e.getCause()).isNotNull();
    }

    @Test
    void robozonkyDaemonResumed() {
        final RoboZonkyDaemonResumedEvent e = EventFactory.roboZonkyDaemonResumed(OffsetDateTime.MIN,
                OffsetDateTime.MAX);
        assertSoftly(softly -> {
            softly.assertThat(e.getUnavailableSince())
                .isEqualTo(OffsetDateTime.MIN);
            softly.assertThat(e.getUnavailableUntil())
                .isEqualTo(OffsetDateTime.MAX);
        });
    }

    @Test
    void robozonkyExperimentalUpdateDetected() {
        var e = EventFactory.roboZonkyExperimentalUpdateDetected("5.0.0-cr-1");
        assertThat(e.getNewVersion()).isEqualTo("5.0.0-cr-1");
    }

    @Test
    void robozonkyUpdateDetected() {
        var e = EventFactory.roboZonkyUpdateDetected("5.0.0");
        assertThat(e.getNewVersion()).isEqualTo("5.0.0");
    }

    @Test
    void robozonkyCrashed() {
        final RoboZonkyCrashedEvent e = EventFactory.roboZonkyCrashed(new OutOfMemoryError());
        assertThat(e.getCause()).isNotEmpty();
    }

    @Test
    void roboZonkyTesting() {
        final RoboZonkyTestingEvent e = EventFactory.roboZonkyTesting();
        assertThat(e).isNotNull();
    }

    @Test
    void saleOffered() {
        final SaleOfferedEvent e = EventFactory.saleOffered(MockInvestmentBuilder.fresh()
            .build(),
                MockLoanBuilder.fresh());
        assertSoftly(softly -> {
            softly.assertThat(e.getLoan())
                .isNotNull();
            softly.assertThat(e.getInvestment())
                .isNotNull();
        });
    }

    @Test
    void saleRecommended() {
        final SaleRecommendedEvent e = EventFactory.saleRecommended(recommendedInvestment());
        assertSoftly(softly -> {
            softly.assertThat(e.getLoan())
                .isNotNull();
            softly.assertThat(e.getInvestment())
                .isNotNull();
            softly.assertThat(e.getRecommendation())
                .isNotNull();
        });
    }

    @Test
    void sellingCompleted() {
        var investment = MockInvestmentBuilder.fresh()
            .build();
        var e = EventFactory.sellingCompleted(singleton(investment), mockPortfolioOverview());
        assertSoftly(softly -> {
            softly.assertThat(e.getInvestments())
                .containsExactly(investment);
            softly.assertThat(e.getPortfolioOverview())
                .isNotNull();
        });
    }

    @Test
    void sellingStarted() {
        final SellingStartedEvent e = EventFactory.sellingStarted(mockPortfolioOverview());
        assertThat(e.getPortfolioOverview()).isNotNull();
    }

    @Test
    void weeklySummary() {
        final WeeklySummaryEvent e = EventFactory.weeklySummary(mock(ExtendedPortfolioOverview.class));
        assertSoftly(softly -> {
            softly.assertThat(e.getPortfolioOverview())
                .isNotNull();
            softly.assertThat(e.getConceivedOn())
                .isBeforeOrEqualTo(OffsetDateTime.now());
        });
    }
}
