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

import com.github.robozonky.api.remote.entities.BlockedAmount;
import com.github.robozonky.api.remote.entities.Transaction;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class SyntheticTest {

    @Test
    void equals() {
        final Synthetic s = new Synthetic(1, BigDecimal.ZERO);
        assertThat(s).isEqualTo(s);
        assertThat(s).isNotEqualTo(null);
        assertThat(s).isNotEqualTo("");
        final Synthetic s2 = new Synthetic(1, BigDecimal.ZERO);
        assertThat(s).isEqualTo(s2);
        final Synthetic s3 = new Synthetic(2, BigDecimal.ZERO);
        assertThat(s).isNotEqualTo(s3);
        final Synthetic s4 = new Synthetic(1, BigDecimal.ONE);
        assertThat(s).isNotEqualTo(s4);
    }

    @Test
    void equalsBlockedAmount() {
        final Synthetic s = new Synthetic(1, BigDecimal.ZERO);
        final BlockedAmount ba = new BlockedAmount(s.getLoanId(), s.getAmount());
        assertThat(Synthetic.equals(s, ba)).isTrue();
        final BlockedAmount ba2 = new BlockedAmount(s.getLoanId() + 1, s.getAmount());
        assertThat(Synthetic.equals(s, ba2)).isFalse();
        final BlockedAmount ba3 = new BlockedAmount(s.getLoanId(), s.getAmount().add(BigDecimal.ONE));
        assertThat(Synthetic.equals(s, ba3)).isFalse();
    }

    private Transaction newTransaction(final int loanId, final BigDecimal amount) {
        final Transaction t = mock(Transaction.class);
        when(t.getLoanId()).thenReturn(loanId);
        when(t.getAmount()).thenReturn(amount);
        return t;
    }

    @Test
    void equalsTransaction() {
        final Synthetic s = new Synthetic(1, BigDecimal.ZERO);
        final Transaction t = newTransaction(s.getLoanId(), s.getAmount());
        assertThat(Synthetic.equals(s, t)).isTrue();
        final Transaction t2 = newTransaction(s.getLoanId() + 1, s.getAmount());
        assertThat(Synthetic.equals(s, t2)).isFalse();
        final Transaction t3 = newTransaction(s.getLoanId(), s.getAmount().add(BigDecimal.ONE));
        assertThat(Synthetic.equals(s, t3)).isFalse();
    }
}
