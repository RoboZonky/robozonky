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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Collections;

import com.github.robozonky.api.notifications.ExecutionCompletedEvent;
import com.github.robozonky.api.notifications.ExecutionStartedEvent;
import com.github.robozonky.api.notifications.InvestmentDelegatedEvent;
import com.github.robozonky.api.notifications.InvestmentMadeEvent;
import com.github.robozonky.api.notifications.InvestmentPurchasedEvent;
import com.github.robozonky.api.notifications.InvestmentRejectedEvent;
import com.github.robozonky.api.notifications.InvestmentRequestedEvent;
import com.github.robozonky.api.notifications.InvestmentSkippedEvent;
import com.github.robozonky.api.notifications.InvestmentSoldEvent;
import com.github.robozonky.api.notifications.LoanDefaultedEvent;
import com.github.robozonky.api.notifications.LoanDelinquentEvent;
import com.github.robozonky.api.notifications.LoanLostEvent;
import com.github.robozonky.api.notifications.LoanNoLongerDelinquentEvent;
import com.github.robozonky.api.notifications.LoanNowDelinquentEvent;
import com.github.robozonky.api.notifications.LoanRecommendedEvent;
import com.github.robozonky.api.notifications.LoanRepaidEvent;
import com.github.robozonky.api.notifications.PurchaseRecommendedEvent;
import com.github.robozonky.api.notifications.PurchaseRequestedEvent;
import com.github.robozonky.api.notifications.PurchasingCompletedEvent;
import com.github.robozonky.api.notifications.PurchasingStartedEvent;
import com.github.robozonky.api.notifications.RoboZonkyDaemonFailedEvent;
import com.github.robozonky.api.notifications.SaleOfferedEvent;
import com.github.robozonky.api.notifications.SaleRecommendedEvent;
import com.github.robozonky.api.notifications.SaleRequestedEvent;
import com.github.robozonky.api.notifications.SellingCompletedEvent;
import com.github.robozonky.api.notifications.SellingStartedEvent;
import com.github.robozonky.api.remote.entities.Participation;
import com.github.robozonky.api.remote.entities.sanitized.Development;
import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.InvestmentDescriptor;
import com.github.robozonky.api.strategies.LoanDescriptor;
import com.github.robozonky.api.strategies.ParticipationDescriptor;
import com.github.robozonky.api.strategies.RecommendedInvestment;
import com.github.robozonky.api.strategies.RecommendedLoan;
import com.github.robozonky.api.strategies.RecommendedParticipation;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.*;

class EventFactoryTest extends AbstractZonkyLeveragingTest {

    private static RecommendedLoan recommendedLoan() {
        final Loan loan = Loan.custom().setNonReservedRemainingInvestment(2000).build();
        return new LoanDescriptor(loan).recommend(200).orElse(null);
    }

    private static RecommendedParticipation recommendedParticipation() {
        final Participation p = mock(Participation.class);
        when(p.getRemainingPrincipal()).thenReturn(BigDecimal.TEN);
        return new ParticipationDescriptor(p, () -> Loan.custom().build()).recommend().orElse(null);
    }

    private static RecommendedInvestment recommendedInvestment() {
        return new InvestmentDescriptor(Investment.custom().setRemainingPrincipal(BigDecimal.TEN).build(),
                                        () -> Loan.custom().build()).recommend().orElse(null);
    }

    private static void assertCorrectThreshold(final LoanDelinquentEvent e, final int threshold) {
        assertThat(e.getThresholdInDays()).isEqualTo(threshold);
    }

    @Test
    void thresholds() {
        final Loan loan = Loan.custom().setRating(Rating.D).setAmount(100_000).build();
        final Investment investment = Investment.fresh(loan, BigDecimal.TEN).build();
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
            softly.assertThat(e.getInvestments()).isEmpty();
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
    void investmentDelegated() {
        final InvestmentDelegatedEvent e = EventFactory.investmentDelegated(recommendedLoan(), "something");
        assertSoftly(softly -> {
            softly.assertThat(e.getLoan()).isNotNull();
            softly.assertThat(e.getRecommendation()).isNotNull();
            softly.assertThat(e.getConfirmationProviderId()).isNotNull();
        });
    }

    @Test
    void investmentMade() {
        final InvestmentMadeEvent e = EventFactory.investmentMade(Investment.custom().build(), Loan.custom().build(),
                                                                  mockPortfolioOverview());
        assertSoftly(softly -> {
            softly.assertThat(e.getLoan()).isNotNull();
            softly.assertThat(e.getInvestment()).isNotNull();
            softly.assertThat(e.getPortfolioOverview()).isNotNull();
        });
    }

    @Test
    void investmentPurchased() {
        final InvestmentPurchasedEvent e = EventFactory.investmentPurchased(Investment.custom().build(),
                                                                            Loan.custom().build(),
                                                                            mockPortfolioOverview());
        assertSoftly(softly -> {
            softly.assertThat(e.getLoan()).isNotNull();
            softly.assertThat(e.getInvestment()).isNotNull();
            softly.assertThat(e.getPortfolioOverview()).isNotNull();
        });
    }

    @Test
    void investmentRejected() {
        final InvestmentRejectedEvent e = EventFactory.investmentRejected(recommendedLoan(), "something");
        assertSoftly(softly -> {
            softly.assertThat(e.getLoan()).isNotNull();
            softly.assertThat(e.getRecommendation()).isNotNull();
            softly.assertThat(e.getConfirmationProviderId()).isNotNull();
        });
    }

    @Test
    void investmentRequested() {
        final InvestmentRequestedEvent e = EventFactory.investmentRequested(recommendedLoan());
        assertSoftly(softly -> {
            softly.assertThat(e.getLoan()).isNotNull();
            softly.assertThat(e.getRecommendation()).isNotNull();
        });
    }

    @Test
    void investmentSkipped() {
        final InvestmentSkippedEvent e = EventFactory.investmentSkipped(recommendedLoan());
        assertSoftly(softly -> {
            softly.assertThat(e.getLoan()).isNotNull();
            softly.assertThat(e.getRecommendation()).isNotNull();
        });
    }

    @Test
    void investmentSold() {
        final InvestmentSoldEvent e = EventFactory.investmentSold(Investment.custom().build(), Loan.custom().build(),
                                                                  mockPortfolioOverview());
        assertSoftly(softly -> {
            softly.assertThat(e.getLoan()).isNotNull();
            softly.assertThat(e.getInvestment()).isNotNull();
            softly.assertThat(e.getPortfolioOverview()).isNotNull();
        });
    }

    @Test
    void loanDefaulted() {
        final LoanDefaultedEvent e = EventFactory.loanDefaulted(Investment.custom().build(), Loan.custom().build(),
                                                                LocalDate.now(), Collections.emptyList());
        assertSoftly(softly -> {
            softly.assertThat(e.getLoan()).isNotNull();
            softly.assertThat(e.getInvestment()).isNotNull();
            softly.assertThat(e.getDelinquentSince()).isNotNull();
            softly.assertThat(e.getCollectionActions()).isEmpty();
        });
    }

    @Test
    void loanNoLongerDelinquent() {
        final LoanNoLongerDelinquentEvent e = EventFactory.loanNoLongerDelinquent(Investment.custom().build(),
                                                                             Loan.custom().build());
        assertSoftly(softly -> {
            softly.assertThat(e.getLoan()).isNotNull();
            softly.assertThat(e.getInvestment()).isNotNull();
        });
    }

    @Test
    void loanNowDelinquent() {
        final LoanNowDelinquentEvent e = EventFactory.loanNowDelinquent(Investment.custom().build(),
                                                                        Loan.custom().build(),
                                                                        LocalDate.now(), Collections.emptyList());
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
        final LoanLostEvent e = EventFactory.loanLost(Investment.custom().build(), Loan.custom().build());
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
    void loanRepaid() {
        final LoanRepaidEvent e = EventFactory.loanRepaid(Investment.custom().build(), Loan.custom().build(),
                                                          mockPortfolioOverview());
        assertSoftly(softly -> {
            softly.assertThat(e.getLoan()).isNotNull();
            softly.assertThat(e.getInvestment()).isNotNull();
            softly.assertThat(e.getPortfolioOverview()).isNotNull();
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
    void purchaseRequested() {
        final PurchaseRequestedEvent e = EventFactory.purchaseRequested(recommendedParticipation());
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
            softly.assertThat(e.getInvestments()).isEmpty();
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
    void robozonkyDaemonFailed() {
        final RoboZonkyDaemonFailedEvent e = EventFactory.roboZonkyDaemonFailed(new IllegalArgumentException());
        assertThat(e.getCause()).isNotNull();
    }

    @Test
    void saleOffered() {
        final SaleOfferedEvent e = EventFactory.saleOffered(Investment.custom().build(), Loan.custom().build());
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
    void saleRequested() {
        final SaleRequestedEvent e = EventFactory.saleRequested(recommendedInvestment());
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
}
