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

import com.github.robozonky.api.remote.entities.Transaction;
import com.github.robozonky.api.remote.enums.TransactionCategory;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

class CashFlowProcessorTest {

    @Test
    void basics() {
        final AbstractTransactionProcessor<CashFlow> p = new CashFlowProcessor();
        final Transaction transaction0 = new Transaction(0, BigDecimal.ZERO, TransactionCategory.PAYMENT);
        p.accept(transaction0);
        for (final TransactionCategory category : TransactionCategory.values()) {
            final int index = category.ordinal() + 1;
            final Transaction t = new Transaction(index, BigDecimal.valueOf(index), category);
            p.accept(t);
        }
        // some amounts are expected negated, as they are outgoing
        Assertions.assertThat(p.get())
                .extracting(CashFlow::getAmount)
                .containsOnly(BigDecimal.valueOf(1).negate(), BigDecimal.valueOf(2).negate(),
                              BigDecimal.valueOf(3).negate(), BigDecimal.valueOf(4).negate(), BigDecimal.valueOf(5),
                              BigDecimal.valueOf(6).negate(), BigDecimal.valueOf(7), BigDecimal.valueOf(8),
                              BigDecimal.valueOf(9), BigDecimal.valueOf(10));
        final CashFlowSummary summary = CashFlowSummary.from(p.get());
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(summary.getInTotal()).isEqualTo(5 + 7 + 8 + 10); // investment fee return counts below...
            softly.assertThat(summary.getInFromDeposits()).isEqualTo(7);
            softly.assertThat(summary.getOutTotal()).isEqualTo(1 + 2 + 3 + 4 + 6 - 9); //... here
            softly.assertThat(summary.getOutFromFees()).isEqualTo(1 + 4 - 9);
            softly.assertThat(summary.getOutFromWithdrawals()).isEqualTo(2);
        });
    }
}
