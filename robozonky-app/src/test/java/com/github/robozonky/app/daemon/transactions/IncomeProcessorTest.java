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

package com.github.robozonky.app.daemon.transactions;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.stream.Stream;

import com.github.robozonky.api.remote.entities.Transaction;
import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.remote.enums.PaymentStatus;
import com.github.robozonky.api.remote.enums.TransactionCategory;
import com.github.robozonky.api.remote.enums.TransactionOrientation;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.app.daemon.BlockedAmountProcessor;
import com.github.robozonky.app.daemon.Portfolio;
import com.github.robozonky.app.daemon.TransactionalPortfolio;
import com.github.robozonky.common.Tenant;
import com.github.robozonky.common.remote.Select;
import com.github.robozonky.common.remote.Zonky;
import com.github.robozonky.common.state.InstanceState;
import com.github.robozonky.common.state.TenantState;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IncomeProcessorTest extends AbstractZonkyLeveragingTest {

    private final IncomeProcessor processor = new IncomeProcessor();
    private final Zonky zonky = harmlessZonky(10_000);
    private final Tenant tenant = mockTenant(zonky);
    private final Portfolio portfolio = Portfolio.create(tenant, BlockedAmountProcessor.createLazy(tenant));
    private final TransactionalPortfolio transactional = new TransactionalPortfolio(portfolio, tenant);
    private final InstanceState<IncomeProcessor> state =
            TenantState.of(tenant.getSessionInfo()).in(IncomeProcessor.class);

    @Test
    public void doesNotQueryAtFirstAttempt() {
        processor.accept(transactional);
        transactional.run(); // persist
        final Select s = new
                Select().greaterThanOrEquals("transaction.transactionDate", LocalDate.now().minusWeeks(1));
        verify(zonky, times(1)).getTransactions(eq(s));
        assertThat(state.getValue(IncomeProcessor.STATE_KEY)).hasValue("-1"); // nothing found
    }

    @Test
    public void queriesAndKeepsPreviousMaxWhenNothingFound() {
        state.update(m -> m.put(IncomeProcessor.STATE_KEY, "1"));
        processor.accept(transactional);
        transactional.run(); // persist
        final Select s = new
                Select().greaterThanOrEquals("transaction.transactionDate", LocalDate.now().minusDays(1));
        verify(zonky, times(1)).getTransactions(eq(s));
        assertThat(state.getValue(IncomeProcessor.STATE_KEY)).hasValue("1"); // keep existing maximum
    }

    @Test
    public void queriesAndUpdatesWhenNewTransactionsFound() {
        state.update(m -> m.put(IncomeProcessor.STATE_KEY, "1"));
        final Loan l1 = Loan.custom().build();
        final Transaction t1 = new Transaction(1, l1, BigDecimal.TEN, TransactionCategory.PAYMENT,
                                               TransactionOrientation.IN);
        final Loan l2 = Loan.custom().build();
        final Transaction t2 = new Transaction(2, l2, BigDecimal.ONE, TransactionCategory.SMP_SELL,
                                               TransactionOrientation.IN);
        final Investment i2 = Investment.fresh(l2, BigDecimal.ONE).build();
        final Loan l3 = Loan.custom().build();
        final Investment i3 = Investment.fresh(l3, BigDecimal.TEN)
                .setPaymentStatus(PaymentStatus.PAID)
                .build();
        final Transaction t3 = new Transaction(3, l3, BigDecimal.TEN, TransactionCategory.PAYMENT,
                                               TransactionOrientation.IN);
        when(zonky.getTransactions((Select) any())).thenAnswer(i -> Stream.of(t2, t3, t1));
        when(zonky.getLoan(eq(l2.getId()))).thenReturn(l2);
        when(zonky.getLoan(eq(l3.getId()))).thenReturn(l3);
        when(zonky.getInvestmentByLoanId(eq(l2.getId()))).thenReturn(Optional.of(i2));
        when(zonky.getInvestmentByLoanId(eq(l3.getId()))).thenReturn(Optional.of(i3));
        processor.accept(transactional);
        transactional.run(); // persist
        verify(zonky, times(1)).getTransactions((Select) any());
        assertThat(state.getValue(IncomeProcessor.STATE_KEY)).hasValue("3"); // new maximum
        assertThat(getEventsRequested()).hasSize(2);
    }
}
