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

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;

import com.github.triceo.robozonky.api.remote.entities.Loan;
import com.github.triceo.robozonky.internal.api.Defaults;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.mockito.Mockito;

public class LoanDescriptorTest {

    private static Loan mockLoan() {
        final Loan mockedLoan = Mockito.mock(Loan.class);
        Mockito.when(mockedLoan.getId()).thenReturn(1);
        Mockito.when(mockedLoan.getAmount()).thenReturn(2000.0);
        Mockito.when(mockedLoan.getRemainingInvestment()).thenReturn(1000.0);
        Mockito.when(mockedLoan.getDatePublished()).thenReturn(OffsetDateTime.now());
        return mockedLoan;
    }

    @Test
    public void constructor() {
        final Loan mockedLoan = LoanDescriptorTest.mockLoan();
        final LoanDescriptor ld = new LoanDescriptor(mockedLoan, Duration.ofSeconds(0));
        final SoftAssertions softly = new SoftAssertions();
        softly.assertThat(ld.getLoan()).isSameAs(mockedLoan);
        softly.assertThat(ld.getLoanCaptchaProtectionEndDateTime())
                .isPresent()
                .contains(mockedLoan.getDatePublished());
        softly.assertAll();
    }

    @Test
    public void equalsSelf() {
        final Loan mockedLoan = LoanDescriptorTest.mockLoan();
        final LoanDescriptor ld = new LoanDescriptor(mockedLoan, Duration.ofSeconds(100));
        Assertions.assertThat(ld)
                .isNotEqualTo(null)
                .isEqualTo(ld);
        final LoanDescriptor ld2 = new LoanDescriptor(mockedLoan, Duration.ofSeconds(100));
        Assertions.assertThat(ld).isEqualTo(ld2);
    }

    @Test
    public void recommendAmount() {
        final Loan mockedLoan = LoanDescriptorTest.mockLoan();
        final LoanDescriptor ld = new LoanDescriptor(mockedLoan, Duration.ofSeconds(100));
        final Optional<Recommendation> r = ld.recommend(Defaults.MINIMUM_INVESTMENT_IN_CZK);
        Assertions.assertThat(r).isPresent();
        final Recommendation recommendation = r.get();
        final SoftAssertions softly = new SoftAssertions();
        softly.assertThat(recommendation.getLoanDescriptor()).isSameAs(ld);
        softly.assertThat(recommendation.getRecommendedInvestmentAmount())
                .isEqualTo(Defaults.MINIMUM_INVESTMENT_IN_CZK);
        softly.assertThat(recommendation.isConfirmationRequired()).isFalse();
        softly.assertAll();
    }

    @Test
    public void recommendWrongAmount() {
        final Loan mockedLoan = LoanDescriptorTest.mockLoan();
        final LoanDescriptor ld = new LoanDescriptor(mockedLoan, Duration.ofSeconds(100));
        final Optional<Recommendation> r = ld.recommend(Defaults.MINIMUM_INVESTMENT_IN_CZK - 1);
        Assertions.assertThat(r).isEmpty();
    }
}
