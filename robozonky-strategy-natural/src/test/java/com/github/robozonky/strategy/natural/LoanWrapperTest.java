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

package com.github.robozonky.strategy.natural;

import java.math.BigDecimal;
import java.util.UUID;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.Ratio;
import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.api.remote.enums.MainIncomeType;
import com.github.robozonky.api.remote.enums.Purpose;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.remote.enums.Region;
import com.github.robozonky.api.strategies.LoanDescriptor;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.test.mock.MockLoanBuilder;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.*;

class LoanWrapperTest {

    private static final PortfolioOverview FOLIO = mock(PortfolioOverview.class);

    @Test
    void fromLoan() {
        final Loan loan = new MockLoanBuilder()
                .setInsuranceActive(true)
                .setAmount(100_000)
                .setRating(Rating.D)
                .setInterestRate(Ratio.ONE)
                .setMainIncomeType(MainIncomeType.EMPLOYMENT)
                .setPurpose(Purpose.AUTO_MOTO)
                .setRegion(Region.JIHOCESKY)
                .setStory(UUID.randomUUID().toString())
                .setTermInMonths(20)
                .setInterestRate(Ratio.ONE)
                .setAnnuity(BigDecimal.ONE)
                .build();
        LoanDescriptor ld = new LoanDescriptor(loan);
        final Wrapper<LoanDescriptor> w = Wrapper.wrap(ld, FOLIO);
        assertSoftly(softly -> {
            softly.assertThat(w.getId()).isNotEqualTo(0);
            softly.assertThat(w.isInsuranceActive()).isEqualTo(loan.isInsuranceActive());
            softly.assertThat(w.getInterestRate()).isEqualTo(Ratio.ONE);
            softly.assertThat(w.getRegion()).isEqualTo(loan.getRegion());
            softly.assertThat(w.getRating()).isEqualTo(loan.getRating());
            softly.assertThat(w.getMainIncomeType()).isEqualTo(loan.getMainIncomeType());
            softly.assertThat(w.getPurpose()).isEqualTo(loan.getPurpose());
            softly.assertThat(w.getOriginalAmount()).isEqualTo(loan.getAmount().getValue().intValue());
            softly.assertThat(w.getRemainingPrincipal()).isEqualTo(BigDecimal.valueOf(w.getOriginalAmount()));
            softly.assertThat(w.getOriginal()).isSameAs(ld);
            softly.assertThat(w.getStory()).isEqualTo(loan.getStory());
            softly.assertThat(w.getOriginalTermInMonths()).isEqualTo(loan.getTermInMonths());
            softly.assertThat(w.getRemainingTermInMonths()).isEqualTo(loan.getTermInMonths());
            softly.assertThat(w.getHealth()).isEmpty();
            softly.assertThat(w.getOriginalPurchasePrice()).isEmpty();
            softly.assertThat(w.getDiscount()).isEmpty();
            softly.assertThat(w.getPrice()).isEmpty();
            softly.assertThat(w.getSellFee()).isEmpty();
            softly.assertThat(w.getReturns()).isEmpty();
            softly.assertThat(w.getRevenueRate()).isEqualTo(Ratio.fromRaw("0.1499"));
            softly.assertThat(w.getOriginalAnnuity()).isEqualTo(loan.getAnnuity().getValue().intValue());
            softly.assertThat(w.toString()).isNotNull();
        });
    }

    @Test
    void fromLoanWithoutRevenueRate() {
        final Loan loan = new MockLoanBuilder()
                .setRating(Rating.D)
                .build();
        final Wrapper<LoanDescriptor> w = Wrapper.wrap(new LoanDescriptor(loan), FOLIO);
        when(FOLIO.getInvested()).thenReturn(Money.ZERO);
        assertThat(w.getRevenueRate()).isEqualTo(Ratio.fromPercentage("14.99"));
    }

    @Test
    void values() {
        final Loan l = new MockLoanBuilder()
                .setInsuranceActive(true)
                .setAmount(100_000)
                .setRating(Rating.D)
                .setInterestRate(Ratio.ONE)
                .setRevenueRate(null)
                .setMainIncomeType(MainIncomeType.EMPLOYMENT)
                .setPurpose(Purpose.AUTO_MOTO)
                .setRegion(Region.JIHOCESKY)
                .setStory(UUID.randomUUID().toString())
                .setTermInMonths(20)
                .build();
        final LoanDescriptor original = new LoanDescriptor(l);
        final PortfolioOverview portfolioOverview = mock(PortfolioOverview.class);
        when(portfolioOverview.getInvested()).thenReturn(Money.from(100_000));
        final Wrapper<LoanDescriptor> w = Wrapper.wrap(original, portfolioOverview);
        assertSoftly(softly -> {
            softly.assertThat(w.isInsuranceActive()).isEqualTo(l.isInsuranceActive());
            softly.assertThat(w.getInterestRate()).isEqualTo(Ratio.ONE);
            softly.assertThat(w.getRegion()).isEqualTo(l.getRegion());
            softly.assertThat(w.getRating()).isEqualTo(l.getRating());
            softly.assertThat(w.getMainIncomeType()).isEqualTo(l.getMainIncomeType());
            softly.assertThat(w.getPurpose()).isEqualTo(l.getPurpose());
            softly.assertThat(w.getOriginalAmount()).isEqualTo(l.getAmount().getValue().intValue());
            softly.assertThat(w.getRemainingPrincipal()).isEqualTo(BigDecimal.valueOf(w.getOriginalAmount()));
            softly.assertThat(w.getOriginal()).isSameAs(original);
            softly.assertThat(w.getStory()).isEqualTo(l.getStory());
            softly.assertThat(w.getOriginalTermInMonths()).isEqualTo(l.getTermInMonths());
            softly.assertThat(w.getRemainingTermInMonths()).isEqualTo(l.getTermInMonths());
            softly.assertThat(w.getHealth()).isEmpty();
            softly.assertThat(w.getRevenueRate()).isGreaterThan(Ratio.ZERO);
            softly.assertThat(w.getOriginalPurchasePrice()).isEmpty();
            softly.assertThat(w.getDiscount()).isEmpty();
            softly.assertThat(w.getPrice()).isEmpty();
            softly.assertThat(w.getSellFee()).isEmpty();
            softly.assertThat(w.getReturns()).isEmpty();
            softly.assertThat(w.toString()).isNotNull();
        });
    }

    @Test
    void equality() {
        final Loan l = new MockLoanBuilder()
                .setInsuranceActive(true)
                .setAmount(100_000)
                .setRating(Rating.D)
                .setInterestRate(Ratio.ONE)
                .setMainIncomeType(MainIncomeType.EMPLOYMENT)
                .setPurpose(Purpose.AUTO_MOTO)
                .setRegion(Region.JIHOCESKY)
                .setStory(UUID.randomUUID().toString())
                .setTermInMonths(20)
                .build();
        final LoanDescriptor original = new LoanDescriptor(l);
        final Wrapper<LoanDescriptor> w = Wrapper.wrap(original, FOLIO);
        assertSoftly(softly -> {
            softly.assertThat(w).isEqualTo(w);
            softly.assertThat(w).isEqualTo(Wrapper.wrap(original, FOLIO));
            softly.assertThat(w).isEqualTo(Wrapper.wrap(new LoanDescriptor(l), FOLIO));
            softly.assertThat(w).isNotEqualTo(Wrapper.wrap(new LoanDescriptor(MockLoanBuilder.fresh()), FOLIO));
            softly.assertThat(w).isNotEqualTo(null);
        });
    }
}
