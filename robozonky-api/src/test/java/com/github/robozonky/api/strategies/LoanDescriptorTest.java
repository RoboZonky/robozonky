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

import java.time.OffsetDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.internal.remote.entities.LoanImpl;

class LoanDescriptorTest {

    static LoanImpl mockLoan() {
        return mockLoan(Rating.D);
    }

    static LoanImpl mockLoan(final Rating r) {
        final LoanImpl loan = mock(LoanImpl.class);
        when(loan.getId()).thenReturn(1);
        when(loan.getRating()).thenReturn(r);
        when(loan.getAmount()).thenReturn(Money.from(2_000));
        when(loan.getNonReservedRemainingInvestment()).thenReturn(Money.from(1_000));
        when(loan.getDatePublished()).thenReturn(OffsetDateTime.now());
        return loan;
    }

    @Test
    void constructor() {
        final LoanImpl mockedLoan = LoanDescriptorTest.mockLoan(Rating.AAAAA);
        final LoanDescriptor ld = new LoanDescriptor(mockedLoan);
        assertSoftly(softly -> {
            softly.assertThat(ld.item())
                .isSameAs(mockedLoan);
            softly.assertThat(ld.item())
                .isSameAs(ld.related());
        });
    }

    @Test
    void equalsSelf() {
        final LoanImpl mockedLoan = LoanDescriptorTest.mockLoan();
        final LoanDescriptor ld = new LoanDescriptor(mockedLoan);
        assertThat(ld)
            .isNotEqualTo(null)
            .isEqualTo(ld);
        final LoanDescriptor ld2 = new LoanDescriptor(mockedLoan);
        assertThat(ld).isEqualTo(ld2);
    }

    @Test
    void recommendAmount() {
        final LoanImpl mockedLoan = LoanDescriptorTest.mockLoan();
        final LoanDescriptor ld = new LoanDescriptor(mockedLoan);
        final Optional<RecommendedLoan> r = ld.recommend(Money.from(200));
        assertThat(r).isPresent();
        final RecommendedLoan recommendation = r.get();
        assertSoftly(softly -> {
            softly.assertThat(recommendation.descriptor())
                .isSameAs(ld);
            softly.assertThat(recommendation.amount())
                .isEqualTo(Money.from(200));
        });
    }

    @Test
    void recommendWrongAmount() {
        final LoanImpl mockedLoan = LoanDescriptorTest.mockLoan();
        final LoanDescriptor ld = new LoanDescriptor(mockedLoan);
        final Optional<RecommendedLoan> r = ld.recommend(mockedLoan.getNonReservedRemainingInvestment()
            .add(1));
        assertThat(r).isEmpty();
    }
}
