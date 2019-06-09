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
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.HashSet;

import com.github.robozonky.api.notifications.Summary;
import com.github.robozonky.api.notifications.WeeklySummaryEvent;
import com.github.robozonky.api.remote.entities.Transaction;
import com.github.robozonky.api.remote.enums.TransactionCategory;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.app.tenant.PowerTenant;
import com.github.robozonky.internal.remote.Select;
import com.github.robozonky.internal.remote.Zonky;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.*;

class SummarizerTest extends AbstractZonkyLeveragingTest {

    private final Zonky zonky = harmlessZonky(10_000);
    private final PowerTenant tenant = mockTenant(zonky);

    @Test
    void basics() {
        final AbstractTransactionProcessor<CashFlow> p = new CashFlowProcessor();
        final Collection<Transaction> transactions = new HashSet<>();
        final Transaction transaction0 = new Transaction(0, BigDecimal.ZERO, TransactionCategory.PAYMENT);
        transactions.add(transaction0);
        for (final TransactionCategory category : TransactionCategory.values()) {
            final int index = category.ordinal() + 1;
            final Transaction t = new Transaction(index, BigDecimal.valueOf(index), category);
            transactions.add(t);
        }
        when(zonky.getTransactions((Select) Mockito.any())).thenReturn(transactions.stream());
        final Summarizer summarizer = new Summarizer();
        summarizer.accept(tenant);
        Assertions.assertThat(this.getEventsRequested())
                .first()
                .isInstanceOf(WeeklySummaryEvent.class);
        final WeeklySummaryEvent evt = (WeeklySummaryEvent)this.getEventsRequested().get(0);
        final Summary summary = evt.getSummary();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(summary.getCashInTotal()).isEqualTo(5 + 7 + 8); // investment fee return counts below...
            softly.assertThat(summary.getCashInFromDeposits()).isEqualTo(7);
            softly.assertThat(summary.getCashOutTotal()).isEqualTo(1 + 2 + 3 + 4 + 6 - 9); //... here
            softly.assertThat(summary.getCashOutFromFees()).isEqualTo(1 + 4 - 9);
            softly.assertThat(summary.getCashOutFromWithdrawals()).isEqualTo(2);
            softly.assertThat(summary.getArrivingInvestments()).isEmpty();
            softly.assertThat(summary.getLeavingInvestments()).isEmpty();
            softly.assertThat(summary.getPortfolioOverview()).isNotNull();
            softly.assertThat(summary.getCreatedOn()).isBeforeOrEqualTo(OffsetDateTime.now());
        });

    }
}
