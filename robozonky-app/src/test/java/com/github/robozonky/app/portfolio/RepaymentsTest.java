/*
 * Copyright 2018 The RoboZonky Project
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

package com.github.robozonky.app.portfolio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import com.github.robozonky.api.notifications.Event;
import com.github.robozonky.api.notifications.LoanRepaidEvent;
import com.github.robozonky.api.remote.entities.Transaction;
import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.remote.enums.PaymentStatus;
import com.github.robozonky.api.remote.enums.TransactionCategory;
import com.github.robozonky.api.remote.enums.TransactionOrientation;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.app.authentication.Tenant;
import com.github.robozonky.common.remote.Select;
import com.github.robozonky.common.remote.Zonky;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.*;

class RepaymentsTest extends AbstractZonkyLeveragingTest {

    private final Zonky zonky = harmlessZonky(10_000);
    private final RemoteBalance balance = mockBalance(zonky);
    private final Tenant tenant = mockTenant(zonky);
    private final Portfolio portfolio = Portfolio.create(tenant, balance);

    @Test
    void onlyChecksAfterInitialized() {
        final Repayments r = new Repayments();
        r.accept(portfolio, tenant);
        verify(zonky, never()).getTransactions(any());
        r.accept(portfolio, tenant);
        verify(zonky).getTransactions(any());
    }

    @Nested
    @DisplayName("After initialization")
    class Initialized {

        private final Repayments r = new Repayments();
        private final LocalDate lastUpdate = LocalDate.now();

        @BeforeEach
        void initialize() {
            r.accept(portfolio, tenant);
        }

        @Test
        @DisplayName("uses proper select to filter transactions.")
        void properSelect() {
            final Select s = new Select()
                    .lessThan("transaction.transactionDate", portfolio.getStatistics().getTimestamp().toLocalDate())
                    .greaterThanOrEquals("transaction.transactionDate", lastUpdate);
            r.accept(portfolio, tenant);
            verify(zonky).getTransactions(eq(s));
        }

        @Test
        @DisplayName("ignores transactions which are not payments.")
        void ignoresNonPayments() {
            final Loan l1 = Loan.custom().setId(3).build();
            final Transaction t1 = new Transaction(l1, BigDecimal.TEN, TransactionCategory.INVESTMENT,
                                                   TransactionOrientation.OUT);
            final Loan l2 = Loan.custom().setId(2).build();
            final Transaction t2 = new Transaction(l2, BigDecimal.TEN, TransactionCategory.SMP_SELL,
                                                   TransactionOrientation.IN);
            final Loan l3 = Loan.custom().setId(1).setMyInvestment(mockMyInvestment()).build();
            final Transaction t3 = new Transaction(l3, BigDecimal.TEN, TransactionCategory.PAYMENT,
                                                   TransactionOrientation.IN);
            when(zonky.getLoan(eq(l3.getId()))).thenReturn(l3);
            when(zonky.getTransactions(any())).thenReturn(Stream.of(t1, t2, t3));
            when(zonky.getInvestment(eq(l3.getMyInvestment().get().getId())))
                    .thenReturn(Optional.of(Investment.custom()
                                                    .setLoanId(l3.getId())
                                                    .build()));
            r.accept(portfolio, tenant);
            verify(zonky, never()).getLoan(eq(l1.getId()));
            verify(zonky, never()).getLoan(eq(l2.getId()));
            verify(zonky).getLoan(eq(l3.getId()));
            verify(zonky, times(1)).getInvestment(anyInt());
        }

        @Test
        @DisplayName("only fires events on paid investments.")
        void firesEvents() {
            final Loan l3 = Loan.custom().setId(1).setMyInvestment(mockMyInvestment()).build();
            final Transaction t3 = new Transaction(l3, BigDecimal.TEN, TransactionCategory.PAYMENT,
                                                   TransactionOrientation.IN);
            final Loan l4 = Loan.custom().setId(2).setMyInvestment(mockMyInvestment()).build();
            final Transaction t4 = new Transaction(l4, BigDecimal.TEN, TransactionCategory.PAYMENT,
                                                   TransactionOrientation.IN);
            when(zonky.getLoan(eq(l3.getId()))).thenReturn(l3);
            when(zonky.getLoan(eq(l4.getId()))).thenReturn(l4);
            when(zonky.getTransactions(any())).thenReturn(Stream.of(t3, t4));
            when(zonky.getInvestment(eq(l3.getMyInvestment().get().getId())))
                    .thenReturn(Optional.of(Investment.custom()
                                                    .setLoanId(l3.getId())
                                                    .setPaymentStatus(PaymentStatus.OK)
                                                    .build()));
            when(zonky.getInvestment(eq(l4.getMyInvestment().get().getId())))
                    .thenReturn(Optional.of(Investment.custom()
                                                    .setLoanId(l4.getId())
                                                    .setPaymentStatus(PaymentStatus.PAID)
                                                    .build()));
            // this is the test
            r.accept(portfolio, tenant);
            final List<Event> events = getNewEvents();
            assertThat(events).hasSize(1)
                    .first().isInstanceOf(LoanRepaidEvent.class);
            final LoanRepaidEvent e = (LoanRepaidEvent) events.get(0);
            assertThat(e.getInvestment().getLoanId()).isEqualTo(l4.getId());
        }
    }
}
