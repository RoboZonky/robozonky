/*
 * Copyright 2020 The RoboZonky Project
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

package com.github.robozonky.api.strategies;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.remote.entities.Investment;
import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.internal.remote.entities.AmountsImpl;
import com.github.robozonky.internal.remote.entities.LoanImpl;

class InvestmentDescriptorTest {

    private static final Loan LOAN = mock(LoanImpl.class);

    private static Investment mockInvestment(final BigDecimal amount) {
        final Investment i = mock(Investment.class);
        when(i.getPrincipal()).thenReturn(new AmountsImpl(Money.from(amount)));
        return i;
    }

    @Test
    void equals() {
        final Investment i = mockInvestment(BigDecimal.TEN);
        final InvestmentDescriptor id = new InvestmentDescriptor(i, () -> LOAN);
        assertSoftly(softly -> {
            softly.assertThat(id)
                .isNotEqualTo(null);
            softly.assertThat(id)
                .isNotEqualTo(UUID.randomUUID()
                    .toString());
            softly.assertThat(id)
                .isEqualTo(id);
        });
        final InvestmentDescriptor id2 = new InvestmentDescriptor(i, () -> LOAN);
        assertSoftly(softly -> {
            softly.assertThat(id)
                .isEqualTo(id2);
            softly.assertThat(id2)
                .isEqualTo(id);
        });
        final InvestmentDescriptor id3 = new InvestmentDescriptor(mockInvestment(BigDecimal.ONE), () -> LOAN);
        assertThat(id).isNotEqualTo(id3);
    }
}
