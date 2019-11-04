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

package com.github.robozonky.app.events.impl;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.notifications.*;
import com.github.robozonky.api.remote.entities.*;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.*;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.internal.remote.entities.MutableParticipation;
import com.github.robozonky.test.mock.MockInvestmentBuilder;
import com.github.robozonky.test.mock.MockLoanBuilder;
import com.github.robozonky.test.mock.MockReservationBuilder;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EventFactoryTest extends AbstractZonkyLeveragingTest {

    private static RecommendedLoan recommendedLoan() {
        final Loan loan = new MockLoanBuilder().setNonReservedRemainingInvestment(2000).build();
        return new LoanDescriptor(loan).recommend(Money.from(200)).orElse(null);
    }

    private static RecommendedParticipation recommendedParticipation() {
        final Participation p = mock(Participation.class);
        when(p.getRemainingPrincipal()).thenReturn(Money.from(10));
        return new ParticipationDescriptor(p, MockLoanBuilder::fresh).recommend().orElse(null);
    }

    private static RecommendedInvestment recommendedInvestment() {
        return new InvestmentDescriptor(MockInvestmentBuilder.fresh().setRemainingPrincipal(BigDecimal.TEN).build(),
                MockLoanBuilder::fresh).recommend().orElse(null);
    }

    private static RecommendedReservation recommendedReservation() {
        final MyReservation mr = mock(MyReservation.class);
        when(mr.getReservedAmount()).thenReturn(Money.from(200));
        final Reservation r = new MockReservationBuilder()
                .setMyReservation(mr)
                .build();
        final Loan l = MockLoanBuilder.fresh();
        return new ReservationDescriptor(r, () -> l)
                .recommend(r.getMyReservation().getReservedAmount())
                .orElse(null);
    }

    private static void assertCorrectThreshold(final LoanDelinquentEvent e, final int threshold) {
        assertThat(e.getThresholdInDays()).isEqualTo(threshold);
    }

    @Test
    void thresholds() {
        final Loan loan = new MockLoanBuilder().setRating(Rating.D).setAmount(100_000).build();
        final Investment investment = MockInvestmentBuilder.fresh(loan, BigDecimal.TEN).build();
        final LocalDate now = LocalDate.now();
        final Collection<Development> developments = Collections.emptyList();
        final LoanDelinquentEvent e = EventFactory.loanDelinquent90plus(investment, loan, now, developments);
        assertCorrectThreshold(e, 90);
        final LoanDelinquentEvent e2 = EventFactory.loanDelinquent60plus(investment, loan, now, developments);
        assertCorrectThreshold(e2, 60);
        final LoanDelinquentEvent e3 = EventFactory.loanDelinquent30plus(investment, loan, now, developments);
        assertCorrectThreshold(e3, 30);
        final LoanDelinquentEvent e4 = EventFactory.loanDelinquent10plus(investment, loan, now, developments);
        assertCorrectThreshold(e4, 10);
    }

    @Test
    void executionCompleted() {
        final ExecutionCompletedEvent e = EventFactory.executionCompleted(Collections.emptyList(),
                                                                          mockPortfolioOverview());
        assertSoftly(softly -> {
            softly.assertThat(e.getLoansInvestedInto()).isEmpty();
            softly.assertThat(e.getPortfolioOverview()).isNotNull();
            softly.assertThat(e.getCreatedOn()).isBeforeOrEqualTo(OffsetDateTime.now());
            softly.assertThat(e.toString()).isNotEmpty();
        });
    }

    @Test
    void executionStarted() {
        final ExecutionStartedEvent e = EventFactory.executionStarted(Collections.emptyList(), mockPortfolioOverview());
        assertSoftly(softly -> {
            softly.assertThat(e.getLoanDescriptors()).isEmpty();
            softly.assertThat(e.getPortfolioOverview()).isNotNull();
        });
    }

    @Test
    void investmentMade() {
        final InvestmentMadeEvent e = EventFactory.investmentMade(MockLoanBuilder.fresh(), Money.from(200),
                mockPortfolioOverview());
        assertSoftly(softly -> {
            softly.assertThat(e.getLoan()).isNotNull();
            softly.assertThat(e.getInvestedAmount()).isEqualTo(Money.from(200));
            softly.assertThat(e.getPortfolioOverview()).isNotNull();
        });
    }

    @Test
    void investmentPurchased() {
        final Loan loan = MockLoanBuilder.fresh();
        final Participation participation = new MutableParticipation(loan, Money.from(200),
                loan.getTermInMonths() - 1);
        final InvestmentPurchasedEvent e = EventFactory.investmentPurchased(participation, loan, Money.from(200),
                mockPortfolioOverview());
        assertSoftly(softly -> {
            softly.assertThat(e.getLoan()).isNotNull();
            softly.assertThat(e.getPurchasedAmount()).isEqualTo(Money.from(200));
            softly.assertThat(e.getPortfolioOverview()).isNotNull();
        });
    }

    @Test
    void investmentSold() {
        final InvestmentSoldEvent e = EventFactory.investmentSold(MockInvestmentBuilder.fresh().build(),
                MockLoanBuilder.fresh(), mockPortfolioOverview());
        assertSoftly(softly -> {
            softly.assertThat(e.getLoan()).isNotNull();
            softly.assertThat(e.getInvestment()).isNotNull();
            softly.assertThat(e.getPortfolioOverview()).isNotNull();
        });
    }

    @Test
    void loanDefaulted() {
        final LoanDefaultedEvent e = EventFactory.loanDefaulted(MockInvestmentBuilder.fresh().build(),
                MockLoanBuilder.fresh(), LocalDate.now(), Collections.emptyList());
        assertSoftly(softly -> {
            softly.assertThat(e.getLoan()).isNotNull();
            softly.assertThat(e.getInvestment()).isNotNull();
            softly.assertThat(e.getDelinquentSince()).isNotNull();
            softly.assertThat(e.getCollectionActions()).isEmpty();
        });
    }

    @Test
    void loanNoLongerDelinquent() {
        final LoanNoLongerDelinquentEvent e = EventFactory.loanNoLongerDelinquent(MockInvestmentBuilder.fresh().build(),
                MockLoanBuilder.fresh());
        assertSoftly(softly -> {
            softly.assertThat(e.getLoan()).isNotNull();
            softly.assertThat(e.getInvestment()).isNotNull();
        });
    }

    @Test
    void loanNowDelinquent() {
        final LoanNowDelinquentEvent e = EventFactory.loanNowDelinquent(MockInvestmentBuilder.fresh().build(),
                MockLoanBuilder.fresh(), LocalDate.now(), Collections.emptyList());
        assertSoftly(softly -> {
            softly.assertThat(e.getLoan()).isNotNull();
            softly.assertThat(e.getInvestment()).isNotNull();
            softly.assertThat(e.getDelinquentSince()).isNotNull();
            softly.assertThat(e.getCollectionActions()).isEmpty();
            softly.assertThat(e.getThresholdInDays()).isEqualTo(0);
        });
    }

    @Test
    void loanLost() {
        final LoanLostEvent e = EventFactory.loanLost(MockInvestmentBuilder.fresh().build(), MockLoanBuilder.fresh());
        assertSoftly(softly -> {
            softly.assertThat(e.getLoan()).isNotNull();
            softly.assertThat(e.getInvestment()).isNotNull();
        });
    }

    @Test
    void loanRecommended() {
        final LoanRecommendedEvent e = EventFactory.loanRecommended(recommendedLoan());
        assertSoftly(softly -> {
            softly.assertThat(e.getLoan()).isNotNull();
            softly.assertThat(e.getRecommendation()).isNotNull();
        });
    }

    @Test
    void purchaseRecommended() {
        final PurchaseRecommendedEvent e = EventFactory.purchaseRecommended(recommendedParticipation());
        assertSoftly(softly -> {
            softly.assertThat(e.getLoan()).isNotNull();
            softly.assertThat(e.getParticipation()).isNotNull();
            softly.assertThat(e.getRecommendation()).isNotNull();
        });
    }

    @Test
    void purchasingCompleted() {
        final PurchasingCompletedEvent e = EventFactory.purchasingCompleted(Collections.emptyList(),
                                                                            mockPortfolioOverview());
        assertSoftly(softly -> {
            softly.assertThat(e.getParticipationsPurchased()).isEmpty();
            softly.assertThat(e.getPortfolioOverview()).isNotNull();
        });
    }

    @Test
    void purchasingStarted() {
        final PurchasingStartedEvent e = EventFactory.purchasingStarted(Collections.emptyList(),
                                                                        mockPortfolioOverview());
        assertSoftly(softly -> {
            softly.assertThat(e.getDescriptors()).isEmpty();
            softly.assertThat(e.getPortfolioOverview()).isNotNull();
        });
    }

    @Test
    void reservationAcceptationRecommended() {
        final ReservationAcceptationRecommendedEvent e =
                EventFactory.reservationAcceptationRecommended(recommendedReservation());
        assertThat(e.getRecommendation()).isNotNull();
    }

    @Test
    void reservationAccepted() {
        final Loan l = MockLoanBuilder.fresh();
        final ReservationAcceptedEvent e = EventFactory.reservationAccepted(l, Money.from(200), mockPortfolioOverview());
        assertSoftly(softly -> {
            softly.assertThat(e.getLoan()).isNotNull();
            softly.assertThat(e.getInvestedAmount()).isEqualTo(Money.from(200));
            softly.assertThat(e.getPortfolioOverview()).isNotNull();
        });
    }

    @Test
    void reservationCheckCompleted() {
        final ReservationCheckCompletedEvent e = EventFactory.reservationCheckCompleted(Collections.emptyList(),
                                                                                        mockPortfolioOverview());
        assertSoftly(softly -> {
            softly.assertThat(e.getReservationsAccepted()).isEmpty();
            softly.assertThat(e.getPortfolioOverview()).isNotNull();
        });
    }

    @Test
    void reservationCheckStarted() {
        final ReservationCheckStartedEvent e = EventFactory.reservationCheckStarted(Collections.emptyList(),
                                                                                    mockPortfolioOverview());
        assertSoftly(softly -> {
            softly.assertThat(e.getReservationDescriptors()).isEmpty();
            softly.assertThat(e.getPortfolioOverview()).isNotNull();
        });
    }

    @Test
    void robozonkyDaemonSuspended() {
        final RoboZonkyDaemonSuspendedEvent e = EventFactory.roboZonkyDaemonSuspended(new IllegalArgumentException());
        assertThat(e.getCause()).isNotNull();
    }

    @Test
    void robozonkyDaemonResumed() {
        final RoboZonkyDaemonResumedEvent e = EventFactory.roboZonkyDaemonResumed(OffsetDateTime.MIN, OffsetDateTime.MAX);
        assertSoftly(softly -> {
            softly.assertThat(e.getUnavailableSince()).isEqualTo(OffsetDateTime.MIN);
            softly.assertThat(e.getUnavailableUntil()).isEqualTo(OffsetDateTime.MAX);
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
        assertThat(e.getCause()).isNotNull();
    }

    @Test
    void roboZonkyTesting() {
        final RoboZonkyTestingEvent e = EventFactory.roboZonkyTesting();
        assertThat(e).isNotNull();
    }

    @Test
    void saleOffered() {
        final SaleOfferedEvent e = EventFactory.saleOffered(MockInvestmentBuilder.fresh().build(), MockLoanBuilder.fresh());
        assertSoftly(softly -> {
            softly.assertThat(e.getLoan()).isNotNull();
            softly.assertThat(e.getInvestment()).isNotNull();
        });
    }

    @Test
    void saleRecommended() {
        final SaleRecommendedEvent e = EventFactory.saleRecommended(recommendedInvestment());
        assertSoftly(softly -> {
            softly.assertThat(e.getLoan()).isNotNull();
            softly.assertThat(e.getInvestment()).isNotNull();
            softly.assertThat(e.getRecommendation()).isNotNull();
        });
    }

    @Test
    void sellingCompleted() {
        final SellingCompletedEvent e = EventFactory.sellingCompleted(Collections.emptyList(), mockPortfolioOverview());
        assertSoftly(softly -> {
            softly.assertThat(e.getInvestments()).isEmpty();
            softly.assertThat(e.getPortfolioOverview()).isNotNull();
        });
    }

    @Test
    void sellingStarted() {
        final SellingStartedEvent e = EventFactory.sellingStarted(Collections.emptyList(), mockPortfolioOverview());
        assertSoftly(softly -> {
            softly.assertThat(e.getDescriptors()).isEmpty();
            softly.assertThat(e.getPortfolioOverview()).isNotNull();
        });
    }

    @Test
    void weeklySummary() {
        final WeeklySummaryEvent e = EventFactory.weeklySummary(mock(ExtendedPortfolioOverview.class));
        assertSoftly(softly -> {
            softly.assertThat(e.getPortfolioOverview()).isNotNull();
            softly.assertThat(e.getConceivedOn()).isBeforeOrEqualTo(OffsetDateTime.now());
        });
    }
}
