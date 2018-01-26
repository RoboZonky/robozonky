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
import java.util.Optional;

import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.internal.api.Defaults;
import com.github.robozonky.internal.api.Settings;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.*;
import static org.mockito.Mockito.*;

class LoanDescriptorTest {

    private static Loan mockLoan() {
        return mockLoan(Rating.D);
    }

    private static Loan mockLoan(final Rating r) {
        final Loan mockedLoan = mock(Loan.class);
        when(mockedLoan.getId()).thenReturn(1);
        when(mockedLoan.getRating()).thenReturn(r);
        when(mockedLoan.getAmount()).thenReturn(2000.0);
        when(mockedLoan.getRemainingInvestment()).thenReturn(1000.0);
        when(mockedLoan.getDatePublished()).thenReturn(OffsetDateTime.now());
        return mockedLoan;
    }

    @Disabled("Looks like CAPTCHA is disabled for now. Let's wait and see if it comes back.")
    @Test
    void constructorForCaptcha() {
        final Loan mockedLoan = LoanDescriptorTest.mockLoan();
        final LoanDescriptor ld = new LoanDescriptor(mockedLoan);
        assertSoftly(softly -> {
            softly.assertThat(ld.item()).isSameAs(mockedLoan);
            softly.assertThat(ld.getLoanCaptchaProtectionEndDateTime())
                    .isPresent()
                    .contains(mockedLoan.getDatePublished().plus(Settings.INSTANCE.getCaptchaDelay()));
        });
    }

    @Test
    void constructorForCaptchaLess() {
        final Loan mockedLoan = LoanDescriptorTest.mockLoan(Rating.AAAAA);
        final LoanDescriptor ld = new LoanDescriptor(mockedLoan);
        assertSoftly(softly -> {
            softly.assertThat(ld.item()).isSameAs(mockedLoan);
            softly.assertThat(ld.getLoanCaptchaProtectionEndDateTime()).isEmpty();
        });
    }

    @Test
    void equalsSelf() {
        final Loan mockedLoan = LoanDescriptorTest.mockLoan();
        final LoanDescriptor ld = new LoanDescriptor(mockedLoan);
        assertThat(ld)
                .isNotEqualTo(null)
                .isEqualTo(ld);
        final LoanDescriptor ld2 = new LoanDescriptor(mockedLoan);
        assertThat(ld).isEqualTo(ld2);
    }

    @Test
    void recommendAmount() {
        final Loan mockedLoan = LoanDescriptorTest.mockLoan();
        final LoanDescriptor ld = new LoanDescriptor(mockedLoan);
        final Optional<RecommendedLoan> r = ld.recommend(BigDecimal.valueOf(Defaults.MINIMUM_INVESTMENT_IN_CZK));
        assertThat(r).isPresent();
        final RecommendedLoan recommendation = r.get();
        final SoftAssertions softly = new SoftAssertions();
        softly.assertThat(recommendation.descriptor()).isSameAs(ld);
        softly.assertThat(recommendation.amount())
                .isEqualTo(BigDecimal.valueOf(Defaults.MINIMUM_INVESTMENT_IN_CZK));
        softly.assertThat(recommendation.isConfirmationRequired()).isFalse();
        softly.assertAll();
    }

    @Test
    void recommendWrongAmount() {
        final Loan mockedLoan = LoanDescriptorTest.mockLoan();
        final LoanDescriptor ld = new LoanDescriptor(mockedLoan);
        final Optional<RecommendedLoan> r = ld.recommend(BigDecimal.valueOf(Defaults.MINIMUM_INVESTMENT_IN_CZK - 1));
        assertThat(r).isEmpty();
    }
}
