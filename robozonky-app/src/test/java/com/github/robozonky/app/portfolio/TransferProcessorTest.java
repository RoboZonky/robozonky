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
import java.util.stream.Stream;

import com.github.robozonky.api.remote.entities.Transaction;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.remote.enums.TransactionCategory;
import com.github.robozonky.api.remote.enums.TransactionOrientation;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.app.authentication.Tenant;
import com.github.robozonky.app.configuration.daemon.Transactional;
import com.github.robozonky.common.remote.Zonky;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class TransferProcessorTest extends AbstractZonkyLeveragingTest {

    private static class TestingTransferProcessor extends TransferProcessor {

        @Override
        boolean filter(final SourceAgnosticTransfer transaction) {
            return true; // accept all
        }

        @Override
        void process(final SourceAgnosticTransfer transaction, final Transactional portfolio) {
            // don't do anything
        }
    }

    @Test
    void deduplicates() {
        final Loan l = Loan.custom().setRating(Rating.D).build();
        final BigDecimal amount = BigDecimal.valueOf(200);
        final SourceAgnosticTransfer t1 = SourceAgnosticTransfer.real(
                new Transaction(l, amount, TransactionCategory.SMP_SELL, TransactionOrientation.IN),
                l::getRating);
        final SourceAgnosticTransfer t2 = SourceAgnosticTransfer.real(
                new Transaction(l, amount.multiply(BigDecimal.TEN), TransactionCategory.SMP_SELL,
                                TransactionOrientation.IN),
                l::getRating); // same loan, different transfer; will be ignored
        final Loan l2 = Loan.custom().setRating(Rating.D).build();
        final SourceAgnosticTransfer t3 = SourceAgnosticTransfer.real(
                new Transaction(l2, amount, TransactionCategory.SMP_SELL, TransactionOrientation.IN),
                l::getRating);
        final Zonky zonky = harmlessZonky(10_000);
        final Tenant tenant = mockTenant(zonky);
        final Portfolio portfolio = Portfolio.create(tenant, TransferMonitor.createLazy(tenant));
        final Transactional transactional = new Transactional(portfolio, tenant);
        // here starts the test
        final TransferProcessor tp = spy(new TestingTransferProcessor());
        tp.accept(Stream.of(t1, t2, t3), transactional);
        verify(tp, times(1)).process(eq(t1), eq(transactional));
        verify(tp, times(1)).process(eq(t3), eq(transactional));
        verify(tp, never()).process(eq(t2), eq(transactional));
    }

    @Test
    void remembersHistory() {
        final Loan l = Loan.custom().setRating(Rating.D).build();
        final BigDecimal amount = BigDecimal.valueOf(200);
        final SourceAgnosticTransfer t1 = SourceAgnosticTransfer.real(
                new Transaction(l, amount, TransactionCategory.SMP_SELL, TransactionOrientation.IN),
                l::getRating);
        final Zonky zonky = harmlessZonky(10_000);
        final Tenant tenant = mockTenant(zonky);
        final Portfolio portfolio = Portfolio.create(tenant, TransferMonitor.createLazy(tenant));
        final Transactional transactional = new Transactional(portfolio, tenant);
        // process the loan, which should then be marked as seen
        final TransferProcessor tp = spy(new TestingTransferProcessor());
        tp.accept(Stream.of(t1), transactional);
        verify(tp, times(1)).process(eq(t1), eq(transactional));
        transactional.run(); // persist state changes
        // now process the same loan and another one, making sure only the second one is processed
        final Loan l2 = Loan.custom().setRating(Rating.D).build();
        final SourceAgnosticTransfer t2 = SourceAgnosticTransfer.real(
                new Transaction(l2, amount, TransactionCategory.SMP_SELL, TransactionOrientation.IN),
                l::getRating);
        tp.accept(Stream.of(t1, t2), transactional);
        verify(tp, times(1)).process(eq(t1), eq(transactional));
        verify(tp, times(1)).process(eq(t2), eq(transactional));
    }
}

