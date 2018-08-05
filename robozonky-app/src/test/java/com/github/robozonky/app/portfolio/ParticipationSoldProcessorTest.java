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
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

import com.github.robozonky.api.notifications.InvestmentSoldEvent;
import com.github.robozonky.api.remote.entities.BlockedAmount;
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

    private static SourceAgnosticTransfer filteredTransfer(final TransferSource source,
                                                           final TransactionCategory category) {
        final Loan l = Loan.custom().setRating(Rating.D).build();
        final BigDecimal amount = BigDecimal.valueOf(200);
        switch (source) {
            case REAL:
                return SourceAgnosticTransfer.real(new Transaction(l, amount, category, TransactionOrientation.IN),
                                                   l::getRating);
            case BLOCKED_AMOUNT:
                return SourceAgnosticTransfer.blockation(new BlockedAmount(l.getId(), amount), l::getRating);
            case SYNTHETIC:
                return SourceAgnosticTransfer.synthetic(OffsetDateTime.now(), l.getId(), TransactionOrientation.IN,
                                                        category, amount, l.getRating());
            default:
                throw new IllegalStateException("Can not happen.");
        }
    }

    @TestFactory
    Collection<DynamicTest> filteringTests() {
        final Collection<DynamicTest> tests = new ArrayList<>(0);
        for (final TransferSource source : TransferSource.values()) {
            final boolean shouldSucceed = source == TransferSource.REAL;
            for (final TransactionCategory category : TransactionCategory.values()) {
                final boolean successExpected = shouldSucceed && category == TransactionCategory.SMP_SELL;
                final SourceAgnosticTransfer transfer = filteredTransfer(source, category);
                final String name = category + " from " + source + " " +
                        (successExpected ? "is" : "is not") +
                        " a participation sale.";
                final DynamicTest test = DynamicTest.dynamicTest(name, () -> {
                    assertThat(ParticipationSoldProcessor.INSTANCE.filter(transfer)).isEqualTo(successExpected);
                });
                tests.add(test);
            }
        }
        return tests;
    }

    @Test
    void nonexistingInvestment() {
        final Zonky zonky = harmlessZonky(10_000);
        final Tenant tenant = mockTenant(zonky);
        final Portfolio portfolio = Portfolio.create(tenant, TransferMonitor.createLazy(tenant));
        final Transactional transactional = new Transactional(portfolio, tenant);
        final SourceAgnosticTransfer transfer = filteredTransfer(TransferSource.REAL, TransactionCategory.PAYMENT);
        assertThatThrownBy(() -> ParticipationSoldProcessor.INSTANCE.process(transfer, transactional))
                .isInstanceOf(Exception.class);
        transactional.run(); // make sure the transaction is processed so that events could be fired
        assertThat(getNewEvents()).isEmpty();
        assertThat(SoldParticipationCache.forTenant(tenant).wasOnceSold(transfer.getLoanId())).isFalse();
    }

    @Test
    void investmentSold() {
        final SourceAgnosticTransfer transfer = filteredTransfer(TransferSource.REAL, TransactionCategory.PAYMENT);
        final int loanId = transfer.getLoanId();
        final Rating rating = transfer.getRating();
        final Loan loan = Loan.custom().setId(loanId).setRating(rating).build();
        final Zonky zonky = harmlessZonky(10_000);
        when(zonky.getLoan(eq(transfer.getLoanId()))).thenReturn(loan);
        final Investment investment = Investment.fresh(loan, transfer.getAmount())
                .setPaymentStatus(PaymentStatus.PAID)
                .build();
        when(zonky.getInvestment(eq(loan))).thenReturn(Optional.of(investment));
        final Tenant tenant = mockTenant(zonky);
        final Portfolio portfolio = Portfolio.create(tenant, TransferMonitor.createLazy(tenant));
        final Transactional transactional = new Transactional(portfolio, tenant);
        ParticipationSoldProcessor.INSTANCE.process(transfer, transactional);
        transactional.run(); // make sure the transaction is processed so that events could be fired
        assertThat(getNewEvents()).first().isInstanceOf(InvestmentSoldEvent.class);
        assertThat(SoldParticipationCache.forTenant(tenant).wasOnceSold(loanId)).isTrue();
    }
}
