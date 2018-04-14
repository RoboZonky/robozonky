/*
 * Copyright 2017 The RoboZonky Project
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

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.github.robozonky.api.remote.entities.sanitized.Loan;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class RecommendedLoanTest {

    private static Loan mockLoan() {
        return Loan.custom().setDatePublished(OffsetDateTime.now()).build();
    }

    private static LoanDescriptor mockLoanDescriptor() {
        final Loan loan = RecommendedLoanTest.mockLoan();
        return new LoanDescriptor(loan);
    }

    @Test
    void constructor() {
        final LoanDescriptor ld = RecommendedLoanTest.mockLoanDescriptor();
        final int amount = 200;
        final RecommendedLoan r = new RecommendedLoan(ld, amount, true);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(r.descriptor()).isSameAs(ld);
            softly.assertThat(r.amount()).isEqualTo(BigDecimal.valueOf(amount));
            softly.assertThat(r.isConfirmationRequired()).isTrue();
        });
    }

    @Test
    void constructorNoLoanDescriptor() {
        final int amount = 200;
        assertThatThrownBy(() -> new RecommendedLoan(null, amount, true))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void equalsSame() {
        final LoanDescriptor ld = RecommendedLoanTest.mockLoanDescriptor();
        final int amount = 200;
        final boolean confirmationRequired = true;
        final RecommendedLoan r1 = new RecommendedLoan(ld, amount, confirmationRequired);
        assertThat(r1).isEqualTo(r1);
        final RecommendedLoan r2 = new RecommendedLoan(ld, amount, confirmationRequired);
        assertThat(r1).isEqualTo(r2);
    }

    @Test
    void notEqualsDifferentLoanDescriptor() {
        final int amount = 200;
        final boolean confirmationRequired = true;
        final RecommendedLoan r1 = new RecommendedLoan(RecommendedLoanTest.mockLoanDescriptor(), amount,
                                                       confirmationRequired);
        final RecommendedLoan r2 = new RecommendedLoan(RecommendedLoanTest.mockLoanDescriptor(), amount,
                                                       confirmationRequired);
        assertThat(r1).isNotEqualTo(r2);
    }

    @Test
    void notEqualsDifferentAmount() {
        final LoanDescriptor ld = RecommendedLoanTest.mockLoanDescriptor();
        final int amount = 200;
        final boolean confirmationRequired = true;
        final RecommendedLoan r1 = new RecommendedLoan(ld, amount, confirmationRequired);
        final RecommendedLoan r2 = new RecommendedLoan(ld, amount + 1, confirmationRequired);
        assertThat(r1).isNotEqualTo(r2);
    }

    @Test
    void notEqualsDifferentConfirmationRequirements() {
        final LoanDescriptor ld = RecommendedLoanTest.mockLoanDescriptor();
        final int amount = 200;
        final RecommendedLoan r1 = new RecommendedLoan(ld, amount, true);
        final RecommendedLoan r2 = new RecommendedLoan(ld, amount, false);
        assertThat(r1).isNotEqualTo(r2);
    }

    @Test
    void notEqualsDifferentJavaType() {
        final RecommendedLoan r1 = new RecommendedLoan(RecommendedLoanTest.mockLoanDescriptor(), 200, true);
        assertThat(r1).isNotEqualTo(r1.toString());
    }
}
