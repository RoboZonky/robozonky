/*
 * Copyright 2019 The RoboZonky Project
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

import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.internal.api.Settings;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

class LoanDescriptorTest {

    private static Loan mockLoan() {
        return mockLoan(Rating.D);
    }

    private static Loan mockLoan(final Rating r) {
        return Loan.custom()
                .setId(1)
                .setRating(r)
                .setAmount(2000)
                .setNonReservedRemainingInvestment(1000)
                .setDatePublished(OffsetDateTime.now())
                .build();
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
        final Optional<RecommendedLoan> r = ld.recommend(200);
        assertThat(r).isPresent();
        final RecommendedLoan recommendation = r.get();
        assertSoftly(softly -> {
            softly.assertThat(recommendation.descriptor()).isSameAs(ld);
            softly.assertThat(recommendation.amount()).isEqualTo(BigDecimal.valueOf(200));
            softly.assertThat(recommendation.isConfirmationRequired()).isFalse();
        });
    }

    @Test
    void recommendWrongAmount() {
        final Loan mockedLoan = LoanDescriptorTest.mockLoan();
        final LoanDescriptor ld = new LoanDescriptor(mockedLoan);
        final Optional<RecommendedLoan> r =
                ld.recommend(BigDecimal.valueOf(mockedLoan.getNonReservedRemainingInvestment() + 1));
        assertThat(r).isEmpty();
    }
}
