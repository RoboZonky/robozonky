/*
 * Copyright 2017 Lukáš Petrovický
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

package com.github.triceo.robozonky.api.strategies;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.github.triceo.robozonky.api.remote.entities.Loan;
import com.github.triceo.robozonky.internal.api.Defaults;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.mockito.Mockito;

public class RecommendedLoanTest {

    private static Loan mockLoan() {
        final Loan loan = Mockito.mock(Loan.class);
        Mockito.when(loan.getDatePublished()).thenReturn(OffsetDateTime.now());
        return loan;
    }

    private static LoanDescriptor mockLoanDescriptor() {
        final Loan loan = RecommendedLoanTest.mockLoan();
        return new LoanDescriptor(loan);
    }

    @Test
    public void constructor() {
        final LoanDescriptor ld = RecommendedLoanTest.mockLoanDescriptor();
        final int amount = Defaults.MINIMUM_INVESTMENT_IN_CZK;
        final RecommendedLoan r = new RecommendedLoan(ld, amount, true);
        final SoftAssertions softly = new SoftAssertions();
        softly.assertThat(r.descriptor()).isSameAs(ld);
        softly.assertThat(r.amount()).isEqualTo(BigDecimal.valueOf(amount));
        softly.assertThat(r.isConfirmationRequired()).isTrue();
        softly.assertAll();
    }

    @Test
    public void constructorNoLoanDescriptor() {
        final int amount = Defaults.MINIMUM_INVESTMENT_IN_CZK;
        Assertions.assertThatThrownBy(() -> new RecommendedLoan(null, amount, true))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void constructorSmallAmount() {
        Assertions.assertThatThrownBy(() -> new RecommendedLoan(RecommendedLoanTest.mockLoanDescriptor(),
                                                                Defaults.MINIMUM_INVESTMENT_IN_CZK - 1, true))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void equalsSame() {
        final LoanDescriptor ld = RecommendedLoanTest.mockLoanDescriptor();
        final int amount = Defaults.MINIMUM_INVESTMENT_IN_CZK;
        final boolean confirmationRequired = true;
        final RecommendedLoan r1 = new RecommendedLoan(ld, amount, confirmationRequired);
        Assertions.assertThat(r1).isEqualTo(r1);
        final RecommendedLoan r2 = new RecommendedLoan(ld, amount, confirmationRequired);
        Assertions.assertThat(r1).isEqualTo(r2);
    }

    @Test
    public void notEqualsDifferentLoanDescriptor() {
        final int amount = Defaults.MINIMUM_INVESTMENT_IN_CZK;
        final boolean confirmationRequired = true;
        final RecommendedLoan r1 = new RecommendedLoan(RecommendedLoanTest.mockLoanDescriptor(), amount,
                                                       confirmationRequired);
        final RecommendedLoan r2 = new RecommendedLoan(RecommendedLoanTest.mockLoanDescriptor(), amount,
                                                       confirmationRequired);
        Assertions.assertThat(r1).isNotEqualTo(r2);
    }

    @Test
    public void notEqualsDifferentAmount() {
        final LoanDescriptor ld = RecommendedLoanTest.mockLoanDescriptor();
        final int amount = Defaults.MINIMUM_INVESTMENT_IN_CZK;
        final boolean confirmationRequired = true;
        final RecommendedLoan r1 = new RecommendedLoan(ld, amount, confirmationRequired);
        final RecommendedLoan r2 = new RecommendedLoan(ld, amount + 1, confirmationRequired);
        Assertions.assertThat(r1).isNotEqualTo(r2);
    }

    @Test
    public void notEqualsDifferentConfirmationRequirements() {
        final LoanDescriptor ld = RecommendedLoanTest.mockLoanDescriptor();
        final int amount = Defaults.MINIMUM_INVESTMENT_IN_CZK;
        final RecommendedLoan r1 = new RecommendedLoan(ld, amount, true);
        final RecommendedLoan r2 = new RecommendedLoan(ld, amount, false);
        Assertions.assertThat(r1).isNotEqualTo(r2);
    }

    @Test
    public void notEqualsDifferentJavaType() {
        final RecommendedLoan r1 = new RecommendedLoan(RecommendedLoanTest.mockLoanDescriptor(),
                                                       Defaults.MINIMUM_INVESTMENT_IN_CZK, true);
        Assertions.assertThat(r1).isNotEqualTo(r1.toString());
    }
}
