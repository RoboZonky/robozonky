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

package com.github.robozonky.app.summaries;

import java.math.BigDecimal;
import java.util.Optional;

import com.github.robozonky.api.remote.entities.Transaction;
import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.remote.enums.PaymentStatus;
import com.github.robozonky.api.remote.enums.TransactionCategory;
import com.github.robozonky.api.remote.enums.TransactionOrientation;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.app.tenant.PowerTenant;
import com.github.robozonky.internal.remote.Zonky;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class LeavingInvestmentProcessorTest extends AbstractZonkyLeveragingTest {

    private final Zonky zonky = harmlessZonky();
    private final PowerTenant tenant = mockTenant(zonky);

    @Test
    void ignoresOutTransactions() {
        final Loan l = Loan.custom().build();
        final Investment i = Investment.fresh(l, 200).build();
        final Transaction transaction = new Transaction(i, BigDecimal.valueOf(200), TransactionCategory.SMP_BUY,
                                                        TransactionOrientation.OUT);
        final AbstractTransactionProcessor<Investment> p = new LeavingInvestmentProcessor(tenant);
        p.accept(transaction);
        assertThat(p.get()).isEmpty();
    }

    @Test
    void correctProcessing() {
        final Loan l = Loan.custom().build();
        final Investment i = Investment.fresh(l, 200).build();
        when(zonky.getInvestmentByLoanId(l.getId())).thenReturn(Optional.of(i));
        final Transaction transaction1 = new Transaction(i, BigDecimal.valueOf(200), TransactionCategory.SMP_SELL,
                                                         TransactionOrientation.IN);
        final Transaction ignoreForWrongCategory = new Transaction(i, BigDecimal.valueOf(200),
                                                                   TransactionCategory.INVESTMENT_FEE,
                                                                   TransactionOrientation.IN);
        final Loan l2 = Loan.custom().build();
        final Investment i2 = Investment.fresh(l2, 200).build();
        when(zonky.getInvestmentByLoanId(l2.getId())).thenReturn(Optional.of(i2));
        final Transaction ignoreForNotYetPaid = new Transaction(i2, BigDecimal.valueOf(200),
                                                                TransactionCategory.PAYMENT,
                                                                TransactionOrientation.IN);
        final Loan l3 = Loan.custom().build();
        final Investment i3 = Investment.fresh(l3, 200)
                .setPaymentStatus(PaymentStatus.PAID)
                .build();
        when(zonky.getInvestmentByLoanId(l3.getId())).thenReturn(Optional.of(i3));
        final Transaction transaction4 = new Transaction(i3, BigDecimal.valueOf(200), TransactionCategory.PAYMENT,
                                                         TransactionOrientation.IN);
        final AbstractTransactionProcessor<Investment> p = new LeavingInvestmentProcessor(tenant);
        p.accept(transaction1);
        p.accept(ignoreForWrongCategory);
        p.accept(ignoreForNotYetPaid);
        p.accept(transaction4);
        assertThat(p.get()).containsOnly(i, i3);
    }
}
