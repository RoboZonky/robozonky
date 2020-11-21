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

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.notifications.ExecutionStartedEvent;
import com.github.robozonky.api.notifications.ExtendedPortfolioOverview;
import com.github.robozonky.api.notifications.InvestmentMadeEvent;
import com.github.robozonky.api.notifications.InvestmentPurchasedEvent;
import com.github.robozonky.api.notifications.InvestmentSoldEvent;
import com.github.robozonky.api.notifications.LoanDefaultedEvent;
import com.github.robozonky.api.notifications.LoanDelinquentEvent;
import com.github.robozonky.api.notifications.LoanLostEvent;
import com.github.robozonky.api.notifications.LoanNoLongerDelinquentEvent;
import com.github.robozonky.api.notifications.LoanNowDelinquentEvent;
import com.github.robozonky.api.notifications.PurchasingStartedEvent;
import com.github.robozonky.api.notifications.ReservationAcceptedEvent;
import com.github.robozonky.api.notifications.ReservationCheckStartedEvent;
import com.github.robozonky.api.notifications.RoboZonkyCrashedEvent;
import com.github.robozonky.api.notifications.RoboZonkyDaemonResumedEvent;
import com.github.robozonky.api.notifications.RoboZonkyDaemonSuspendedEvent;
import com.github.robozonky.api.notifications.RoboZonkyTestingEvent;
import com.github.robozonky.api.notifications.SaleOfferedEvent;
import com.github.robozonky.api.notifications.SellingStartedEvent;
import com.github.robozonky.api.notifications.WeeklySummaryEvent;
import com.github.robozonky.api.remote.entities.Investment;
import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.api.remote.entities.Participation;
import com.github.robozonky.api.remote.enums.LoanHealth;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.internal.remote.entities.LoanHealthStatsImpl;
import com.github.robozonky.internal.remote.entities.LoanImpl;
import com.github.robozonky.internal.remote.entities.ParticipationImpl;
import com.github.robozonky.internal.test.DateUtil;
import com.github.robozonky.test.mock.MockInvestmentBuilder;
import com.github.robozonky.test.mock.MockLoanBuilder;

class EventFactoryTest extends AbstractZonkyLeveragingTest {

    @Test
    void thresholds() {
        final Loan loan = new MockLoanBuilder()
            .set(LoanImpl::setRating, Rating.D)
            .set(LoanImpl::setAmount, Money.from(100_000))
            .build();
        final Investment investment = MockInvestmentBuilder.fresh(loan, BigDecimal.TEN)
            .build();
        final LocalDate now = LocalDate.now();
        final LoanDelinquentEvent e = EventFactory.loanDelinquent90plus(investment, loan);
        assertThat(e.getThresholdInDays()).isEqualTo(90);
        final LoanDelinquentEvent e2 = EventFactory.loanDelinquent60plus(investment, loan);
        assertThat(e2.getThresholdInDays()).isEqualTo(60);
        final LoanDelinquentEvent e3 = EventFactory.loanDelinquent30plus(investment, loan);
        assertThat(e3.getThresholdInDays()).isEqualTo(30);
        final LoanDelinquentEvent e4 = EventFactory.loanDelinquent10plus(investment, loan);
        assertThat(e4.getThresholdInDays()).isEqualTo(10);
    }

    @Test
    void executionCompleted() {
        Loan loan = MockLoanBuilder.fresh();
        var e = EventFactory.executionCompleted(mockPortfolioOverview());
        assertSoftly(softly -> {
            softly.assertThat(e.getPortfolioOverview())
                .isNotNull();
            softly.assertThat(e.getCreatedOn())
                .isBeforeOrEqualTo(DateUtil.zonedNow());
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
            .build(), MockLoanBuilder.fresh(), mockPortfolioOverview());
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
            .build(), MockLoanBuilder.fresh(), LocalDate.now());
        assertSoftly(softly -> {
            softly.assertThat(e.getLoan())
                .isNotNull();
            softly.assertThat(e.getInvestment())
                .isNotNull();
        });
    }

    @Test
    void loanNoLongerDelinquent() {
        final LoanNoLongerDelinquentEvent e = EventFactory.loanNoLongerDelinquent(MockInvestmentBuilder.fresh()
            .build(), MockLoanBuilder.fresh());
        assertSoftly(softly -> {
            softly.assertThat(e.getLoan())
                .isNotNull();
            softly.assertThat(e.getInvestment())
                .isNotNull();
        });
    }

    @Test
    void loanNowDelinquent() {
        Loan loan = new MockLoanBuilder()
            .set(LoanImpl::setRating, Rating.AAAAA)
            .build();
        final LoanNowDelinquentEvent e = EventFactory.loanNowDelinquent(MockInvestmentBuilder
            .fresh(loan, new LoanHealthStatsImpl(LoanHealth.CURRENTLY_IN_DUE), 200)
            .build(), loan);
        assertSoftly(softly -> {
            softly.assertThat(e.getLoan())
                .isNotNull();
            softly.assertThat(e.getInvestment())
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
    void purchasingCompleted() {
        var e = EventFactory.purchasingCompleted(mockPortfolioOverview());
        assertThat(e.getPortfolioOverview())
            .isNotNull();
    }

    @Test
    void purchasingStarted() {
        final PurchasingStartedEvent e = EventFactory.purchasingStarted(mockPortfolioOverview());
        assertThat(e.getPortfolioOverview()).isNotNull();
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
        var e = EventFactory.reservationCheckCompleted(mockPortfolioOverview());
        assertThat(e.getPortfolioOverview())
            .isNotNull();
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
        var dateTime = DateUtil.zonedNow();
        final RoboZonkyDaemonResumedEvent e = EventFactory.roboZonkyDaemonResumed(dateTime, dateTime.plusDays(1));
        assertSoftly(softly -> {
            softly.assertThat(e.getUnavailableSince())
                .isEqualTo(dateTime);
            softly.assertThat(e.getUnavailableUntil())
                .isEqualTo(dateTime.plusDays(1));
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
            .build(), MockLoanBuilder.fresh());
        assertSoftly(softly -> {
            softly.assertThat(e.getLoan())
                .isNotNull();
            softly.assertThat(e.getInvestment())
                .isNotNull();
        });
    }

    @Test
    void sellingCompleted() {
        var e = EventFactory.sellingCompleted(mockPortfolioOverview());
        assertThat(e.getPortfolioOverview())
            .isNotNull();
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
                .isBeforeOrEqualTo(DateUtil.zonedNow());
        });
    }
}
