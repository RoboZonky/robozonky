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
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.remote.enums.TransactionCategory;
import com.github.robozonky.api.remote.enums.TransactionOrientation;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SourceAgnosticTransferTest {

    @Test
    void promotionFromSyntheticToReal() {
        final OffsetDateTime time = OffsetDateTime.now();
        final int loanId = 1;
        final Rating rating = Rating.A;
        final BigDecimal amount = BigDecimal.TEN;
        final TransactionCategory category = TransactionCategory.SMP_SELL;
        final TransactionOrientation orientation = TransactionOrientation.IN;
        final SourceAgnosticTransfer synthetic = SourceAgnosticTransfer.synthetic(time, loanId, orientation, category,
                                                                                  amount, rating);
        synthetic.promote(TransferSource.REAL);
    }

    @Test
    void promotionFromBlockedAmountToReal() {
        final int loanId = 1;
        final Rating rating = Rating.A;
        final BigDecimal amount = BigDecimal.TEN;
        final TransactionCategory category = TransactionCategory.SMP_SELL;
        final BlockedAmount blockedAmount = new BlockedAmount(loanId, amount, category);
        final SourceAgnosticTransfer synthetic = SourceAgnosticTransfer.blockation(blockedAmount, () -> rating);
        synthetic.promote(TransferSource.REAL);
    }

    @Test
    void demotion() {
        final int loanId = 1;
        final Rating rating = Rating.A;
        final BigDecimal amount = BigDecimal.TEN;
        final TransactionCategory category = TransactionCategory.SMP_SELL;
        final BlockedAmount blockedAmount = new BlockedAmount(loanId, amount, category);
        final SourceAgnosticTransfer synthetic = SourceAgnosticTransfer.blockation(blockedAmount, () -> rating);
        assertThatThrownBy(() -> synthetic.promote(TransferSource.SYNTHETIC))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void equals() {
        final OffsetDateTime time = OffsetDateTime.now();
        final int loanId = 1;
        final Rating rating = Rating.A;
        final BigDecimal amount = BigDecimal.TEN;
        final TransactionCategory category = TransactionCategory.SMP_SELL;
        final TransactionOrientation orientation = TransactionOrientation.IN;
        final SourceAgnosticTransfer synthetic = SourceAgnosticTransfer.synthetic(time, loanId, orientation, category,
                                                                                  amount, rating);
        assertThat(synthetic).isEqualTo(synthetic);
        assertThat(synthetic).isNotEqualTo(null);
        final SourceAgnosticTransfer identical = SourceAgnosticTransfer.synthetic(time, loanId, orientation, category,
                                                                                  amount, rating);
        assertThat(identical).isEqualTo(synthetic);
        identical.promote(TransferSource.BLOCKED_AMOUNT); // promoted variant is still equal
        assertThat(synthetic).isEqualTo(identical);
        final SourceAgnosticTransfer changedTime = SourceAgnosticTransfer.synthetic(time.plusDays(1), loanId,
                                                                                    orientation, category, amount,
                                                                                    rating);
        assertThat(changedTime).isEqualTo(synthetic);
        // changes follow which are not supposed to be equal
        final SourceAgnosticTransfer changedOrientation = SourceAgnosticTransfer.synthetic(time, loanId,
                                                                                           TransactionOrientation.OUT,
                                                                                           category, amount, rating);
        assertThat(changedOrientation).isNotEqualTo(synthetic);
        final SourceAgnosticTransfer changedLoanId = SourceAgnosticTransfer.synthetic(time, loanId + 1, orientation,
                                                                                      category, amount, rating);
        assertThat(changedLoanId).isNotEqualTo(synthetic);
        final SourceAgnosticTransfer changedAmount = SourceAgnosticTransfer.synthetic(time, loanId, orientation,
                                                                                      category,
                                                                                      amount.add(BigDecimal.TEN),
                                                                                      rating);
        assertThat(changedAmount).isNotEqualTo(synthetic);
        final SourceAgnosticTransfer changedCategory = SourceAgnosticTransfer.synthetic(time, loanId, orientation,
                                                                                        TransactionCategory.PAYMENT,
                                                                                        amount, rating);
        assertThat(changedCategory).isNotEqualTo(synthetic);
    }
}

