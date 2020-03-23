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
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.remote.entities.Investment;
import com.github.robozonky.api.remote.entities.Loan;

class RecommendedInvestmentTest {

    private static final Loan LOAN = LoanDescriptorTest.mockLoan();

    private static Investment mockInvestment(final BigDecimal remainingPrincipal) {
        final Investment i = mock(Investment.class);
        when(i.getRemainingPrincipal()).thenReturn(Optional.of(Money.from(remainingPrincipal)));
        return i;
    }

    @Test
    void equals() {
        final BigDecimal remainingPrincipal = BigDecimal.TEN;
        final Investment i = mockInvestment(remainingPrincipal);
        final InvestmentDescriptor d = new InvestmentDescriptor(i, () -> LOAN);
        final RecommendedInvestment r = new RecommendedInvestment(d, Money.from(remainingPrincipal));
        assertSoftly(softly -> {
            softly.assertThat(r)
                .isNotEqualTo(null);
            softly.assertThat(r)
                .isNotEqualTo(UUID.randomUUID()
                    .toString());
            softly.assertThat(r)
                .isEqualTo(r);
        });
        final RecommendedInvestment r2 = new RecommendedInvestment(d, Money.from(remainingPrincipal));
        assertSoftly(softly -> {
            softly.assertThat(r)
                .isEqualTo(r2);
            softly.assertThat(r2)
                .isEqualTo(r);
        });
        final RecommendedInvestment r3 = new RecommendedInvestment(
                new InvestmentDescriptor(mockInvestment(remainingPrincipal), () -> LOAN),
                Money.from(remainingPrincipal));
        assertThat(r).isNotEqualTo(r3);
    }
}
