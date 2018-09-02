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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

import com.github.robozonky.api.notifications.InvestmentSoldEvent;
import com.github.robozonky.api.remote.entities.Transaction;
import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.remote.enums.PaymentStatus;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.remote.enums.TransactionCategory;
import com.github.robozonky.api.remote.enums.TransactionOrientation;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.app.authentication.Tenant;
import com.github.robozonky.app.configuration.daemon.Transactional;
import com.github.robozonky.app.util.SoldParticipationCache;
import com.github.robozonky.common.remote.Zonky;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class ParticipationSoldProcessorTest extends AbstractZonkyLeveragingTest {

    private static Transaction filteredTransfer(final TransactionCategory category) {
        final Loan l = Loan.custom().setRating(Rating.D).build();
        final BigDecimal amount = BigDecimal.valueOf(200);
        return new Transaction(l, amount, category, TransactionOrientation.IN);
    }

    @TestFactory
    Collection<DynamicTest> filteringTests() {
        final Collection<DynamicTest> tests = new ArrayList<>(0);
        for (final TransactionCategory category : TransactionCategory.values()) {
            final boolean successExpected = category == TransactionCategory.SMP_SELL;
            final Transaction transfer = filteredTransfer(category);
            final String name = category + " " +
                    (successExpected ? "is" : "is not") +
                    " a participation sale.";
            final DynamicTest test = DynamicTest.dynamicTest(name, () -> {
                assertThat(ParticipationSoldProcessor.INSTANCE.isApplicable(transfer)).isEqualTo(successExpected);
            });
            tests.add(test);
        }
        return tests;
    }

    @Test
    void nonexistingInvestment() {
        final Zonky zonky = harmlessZonky(10_000);
        final Tenant tenant = mockTenant(zonky);
        final Portfolio portfolio = Portfolio.create(tenant, BlockedAmountProcessor.createLazy(tenant));
        final Transactional transactional = new Transactional(portfolio, tenant);
        final Transaction transfer = filteredTransfer(TransactionCategory.PAYMENT);
        assertThatThrownBy(() -> ParticipationSoldProcessor.INSTANCE.processApplicable(transfer, transactional))
                .isInstanceOf(Exception.class);
        transactional.run(); // make sure the transaction is processed so that events could be fired
        assertThat(getNewEvents()).isEmpty();
        final int loanId = transfer.getLoanId();
        assertThat(SoldParticipationCache.forTenant(tenant).wasOnceSold(loanId)).isFalse();
    }

    @Test
    void investmentSold() {
        final Loan loan = Loan.custom().build();
        final Transaction transfer = new Transaction(loan, BigDecimal.TEN, TransactionCategory.SMP_SELL,
                                                     TransactionOrientation.IN);
        final int loanId = loan.getId();
        final Zonky zonky = harmlessZonky(10_000);
        when(zonky.getLoan(eq(loanId))).thenReturn(loan);
        final Investment investment = Investment.fresh(loan, transfer.getAmount())
                .setPaymentStatus(PaymentStatus.PAID)
                .build();
        when(zonky.getInvestment(eq(loan))).thenReturn(Optional.of(investment));
        final Tenant tenant = mockTenant(zonky);
        final Portfolio portfolio = Portfolio.create(tenant, BlockedAmountProcessor.createLazy(tenant));
        final Transactional transactional = new Transactional(portfolio, tenant);
        ParticipationSoldProcessor.INSTANCE.processApplicable(transfer, transactional);
        transactional.run(); // make sure the transaction is processed so that events could be fired
        assertThat(getNewEvents()).first().isInstanceOf(InvestmentSoldEvent.class);
        assertThat(SoldParticipationCache.forTenant(tenant).wasOnceSold(loanId)).isTrue();
    }
}
