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

import com.github.robozonky.api.remote.entities.BlockedAmount;
import com.github.robozonky.api.remote.entities.Transaction;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.remote.enums.TransactionCategory;
import com.github.robozonky.api.remote.enums.TransactionOrientation;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TransfersTest {

    @Test
    void syntheticTransactionsAndBlockedAmountsAreAlwaysUnprocessed() {
        final Transaction transaction = new Transaction(Loan.custom().build(), BigDecimal.TEN.add(BigDecimal.ONE),
                                                        TransactionCategory.SMP_SELL, TransactionOrientation.IN);
        final Transfers t = new Transfers(OffsetDateTime.now().minusSeconds(1)); // prevent flakiness
        final boolean syntheticAdded = t.fromInvestment(11, Rating.D, BigDecimal.TEN);
        assertThat(syntheticAdded).isTrue();
        final BlockedAmount ba = new BlockedAmount(12, BigDecimal.ONE);
        final boolean blockedAmountAdded = t.fromZonky(ba, () -> Rating.C);
        assertThat(blockedAmountAdded).isTrue();
        t.fromZonky(transaction, () -> Rating.B);
        // all amounts will first be included as unprocessed
        assertThat(t.getUnprocessed()).hasSize(3);
        final Transfers t2 = t.rebase(t.getCurrentEpoch().plusHours(1)); // should remove the real transaction
        assertThat(t2.getUnprocessed()).hasSize(2);
        // promote the synthetic to real, removing it too
        final Transaction transaction2 = new Transaction(Loan.custom().setId(11).build(),
                                                         BigDecimal.TEN, TransactionCategory.INVESTMENT,
                                                         TransactionOrientation.OUT);
        final boolean transactionAdded = t2.fromZonky(transaction2, () -> Rating.D);
        assertThat(transactionAdded).isFalse(); // just promoted existing, not added new
        assertThat(t2.getUnprocessed()).hasSize(1)
                .extracting(SourceAgnosticTransfer::getAmount)
                .first()
                .isEqualTo(ba.getAmount().negate()); // the blocked amount is the only one remaining
    }
}
