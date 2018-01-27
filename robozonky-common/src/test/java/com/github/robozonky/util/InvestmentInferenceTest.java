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

package com.github.robozonky.util;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import com.github.robozonky.api.remote.entities.Investment;
import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.api.remote.entities.MyInvestment;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.SoftAssertions.*;
import static org.mockito.Mockito.*;

class InvestmentInferenceTest {

    @Test
    void fromSecondaryMarketplace() {
        final Loan loan = new Loan(1, 1000);
        final Investment investment = spy(new Investment(loan, 200));
        when(investment.getInvestmentDate()).thenReturn(OffsetDateTime.now().minusMonths(1));
        when(investment.getPurchasePrice()).thenReturn(BigDecimal.valueOf(1));
        final InvestmentInference i = InvestmentInference.with(investment, loan);
        assertSoftly(softly -> {
            softly.assertThat(i.getOriginalAmount()).isEqualTo(investment.getPurchasePrice());
            softly.assertThat(i.getElapsed(LocalDate.now()).toTotalMonths()).isEqualTo(1);
            softly.assertThat(i.getTotalAmountPaid()).isEqualTo(BigDecimal.ZERO);
        });
    }

    @Test
    void fromPrimaryMarketplace() {
        final MyInvestment mi = mock(MyInvestment.class);
        when(mi.getTimeCreated()).thenReturn(OffsetDateTime.now().minusMonths(1));
        final Loan loan = spy(new Loan(1, 1000));
        when(loan.getMyInvestment()).thenReturn(mi);
        final Investment investment = spy(new Investment(loan, 200));
        when(investment.getInvestmentDate()).thenReturn(null);
        when(investment.getPaidPenalty()).thenReturn(BigDecimal.TEN);
        final InvestmentInference i = InvestmentInference.with(investment, loan);
        assertSoftly(softly -> {
            softly.assertThat(i.getOriginalAmount()).isEqualTo(investment.getAmount());
            softly.assertThat(i.getElapsed(LocalDate.now()).toTotalMonths()).isEqualTo(1);
            softly.assertThat(i.getTotalAmountPaid()).isEqualTo(BigDecimal.TEN);
        });
    }
}
