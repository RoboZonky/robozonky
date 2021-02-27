/*
 * Copyright 2021 The RoboZonky Project
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

package com.github.robozonky.strategy.natural.wrappers;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.Ratio;
import com.github.robozonky.api.remote.enums.MainIncomeType;
import com.github.robozonky.api.remote.enums.Purpose;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.remote.enums.Region;
import com.github.robozonky.api.strategies.LoanDescriptor;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.internal.remote.entities.LoanImpl;
import com.github.robozonky.test.AbstractRoboZonkyTest;
import com.github.robozonky.test.mock.MockLoanBuilder;

class LoanWrapperTest extends AbstractRoboZonkyTest {

    private static final PortfolioOverview FOLIO = mockPortfolioOverview();

    @Test
    void fromLoan() {
        final LoanImpl loan = new MockLoanBuilder()
            .set(LoanImpl::setInsuranceActive, false)
            .set(LoanImpl::setAmount, Money.from(100_000))
            .set(LoanImpl::setInterestRate, Rating.D.getInterestRate())
            .set(LoanImpl::setMainIncomeType, MainIncomeType.EMPLOYMENT)
            .set(LoanImpl::setPurpose, Purpose.AUTO_MOTO)
            .set(LoanImpl::setRegion, Region.JIHOCESKY)
            .set(LoanImpl::setStory, UUID.randomUUID()
                .toString())
            .set(LoanImpl::setTermInMonths, 20)
            .set(LoanImpl::setAnnuity, Money.from(BigDecimal.ONE))
            .build();
        LoanDescriptor ld = new LoanDescriptor(loan);
        final Wrapper<LoanDescriptor> w = Wrapper.wrap(ld, FOLIO);
        assertSoftly(softly -> {
            softly.assertThat(w.getId())
                .isNotEqualTo(0);
            softly.assertThat(w.isInsuranceActive())
                .isEqualTo(loan.isInsuranceActive());
            softly.assertThat(w.getInterestRate())
                .isEqualTo(loan.getInterestRate());
            softly.assertThat(w.getRegion())
                .isEqualTo(loan.getRegion());
            softly.assertThat(w.getMainIncomeType())
                .isEqualTo(loan.getMainIncomeType());
            softly.assertThat(w.getPurpose())
                .isEqualTo(loan.getPurpose());
            softly.assertThat(w.getOriginalAmount())
                .isEqualTo(loan.getAmount()
                    .getValue()
                    .intValue());
            softly.assertThat(w.getRemainingPrincipal())
                .isEqualTo(BigDecimal.valueOf(w.getOriginalAmount()));
            softly.assertThat(w.getOriginal())
                .isSameAs(ld);
            softly.assertThat(w.getStory())
                .isEqualTo(loan.getStory());
            softly.assertThat(w.getOriginalTermInMonths())
                .isEqualTo(loan.getTermInMonths());
            softly.assertThat(w.getRemainingTermInMonths())
                .isEqualTo(loan.getTermInMonths());
            softly.assertThat(w.getHealth())
                .isEmpty();
            softly.assertThat(w.getOriginalPurchasePrice())
                .isEmpty();
            softly.assertThat(w.getSellPrice())
                .isEmpty();
            softly.assertThat(w.getSellFee())
                .isEmpty();
            softly.assertThat(w.getReturns())
                .isEmpty();
            softly.assertThat(w.getRevenueRate())
                .isEqualTo(Ratio.fromRaw("0.0799"));
            softly.assertThat(w.getOriginalAnnuity())
                .isEqualTo(loan.getAnnuity()
                    .getValue()
                    .intValue());
            softly.assertThat(w.getCurrentDpd())
                .isEmpty();
            softly.assertThat(w.getLongestDpd())
                .isEmpty();
            softly.assertThat(w.getDaysSinceDpd())
                .isEmpty();
            softly.assertThat(w.toString())
                .isNotNull();
        });
    }

    @Test
    void fromLoanWithoutRevenueRate() {
        final LoanImpl loan = new MockLoanBuilder()
            .set(LoanImpl::setInterestRate, Rating.D.getInterestRate())
            .set(LoanImpl::setRevenueRate, null)
            .build();
        final Wrapper<LoanDescriptor> w = Wrapper.wrap(new LoanDescriptor(loan), FOLIO);
        when(FOLIO.getInvested()).thenReturn(Money.ZERO);
        assertThat(w.getRevenueRate()).isEqualTo(Ratio.fromPercentage("14.99"));
    }

    @Test
    void values() {
        final LoanImpl l = new MockLoanBuilder()
            .set(LoanImpl::setInsuranceActive, true)
            .set(LoanImpl::setAmount, Money.from(100_000))
            .set(LoanImpl::setInterestRate, Rating.D.getInterestRate())
            .set(LoanImpl::setRevenueRate, null)
            .set(LoanImpl::setMainIncomeType, MainIncomeType.EMPLOYMENT)
            .set(LoanImpl::setPurpose, Purpose.AUTO_MOTO)
            .set(LoanImpl::setRegion, Region.JIHOCESKY)
            .set(LoanImpl::setStory, UUID.randomUUID()
                .toString())
            .set(LoanImpl::setTermInMonths, 20)
            .build();
        final LoanDescriptor original = new LoanDescriptor(l);
        final PortfolioOverview portfolioOverview = mock(PortfolioOverview.class);
        when(portfolioOverview.getInvested()).thenReturn(Money.from(100_000));
        final Wrapper<LoanDescriptor> w = Wrapper.wrap(original, portfolioOverview);
        assertSoftly(softly -> {
            softly.assertThat(w.isInsuranceActive())
                .isEqualTo(l.isInsuranceActive());
            softly.assertThat(w.getInterestRate())
                .isEqualTo(l.getInterestRate());
            softly.assertThat(w.getRegion())
                .isEqualTo(l.getRegion());
            softly.assertThat(w.getMainIncomeType())
                .isEqualTo(l.getMainIncomeType());
            softly.assertThat(w.getPurpose())
                .isEqualTo(l.getPurpose());
            softly.assertThat(w.getOriginalAmount())
                .isEqualTo(l.getAmount()
                    .getValue()
                    .intValue());
            softly.assertThat(w.getRemainingPrincipal())
                .isEqualTo(BigDecimal.valueOf(w.getOriginalAmount()));
            softly.assertThat(w.getOriginal())
                .isSameAs(original);
            softly.assertThat(w.getStory())
                .isEqualTo(l.getStory());
            softly.assertThat(w.getOriginalTermInMonths())
                .isEqualTo(l.getTermInMonths());
            softly.assertThat(w.getRemainingTermInMonths())
                .isEqualTo(l.getTermInMonths());
            softly.assertThat(w.getHealth())
                .isEmpty();
            softly.assertThat(w.getRevenueRate())
                .isGreaterThan(Ratio.ZERO);
            softly.assertThat(w.getOriginalPurchasePrice())
                .isEmpty();
            softly.assertThat(w.getSellPrice())
                .isEmpty();
            softly.assertThat(w.getSellFee())
                .isEmpty();
            softly.assertThat(w.getReturns())
                .isEmpty();
            softly.assertThat(w.toString())
                .isNotNull();
        });
    }

    @Test
    void equality() {
        final LoanImpl l = new MockLoanBuilder()
            .set(LoanImpl::setInsuranceActive, true)
            .set(LoanImpl::setAmount, Money.from(100_000))
            .set(LoanImpl::setInterestRate, Rating.D.getInterestRate())
            .set(LoanImpl::setMainIncomeType, MainIncomeType.EMPLOYMENT)
            .set(LoanImpl::setPurpose, Purpose.AUTO_MOTO)
            .set(LoanImpl::setRegion, Region.JIHOCESKY)
            .set(LoanImpl::setStory, UUID.randomUUID()
                .toString())
            .set(LoanImpl::setTermInMonths, 20)
            .build();
        final LoanDescriptor original = new LoanDescriptor(l);
        final Wrapper<LoanDescriptor> w = Wrapper.wrap(original, FOLIO);
        assertSoftly(softly -> {
            softly.assertThat(w)
                .isEqualTo(w);
            softly.assertThat(w)
                .isEqualTo(Wrapper.wrap(original, FOLIO));
            softly.assertThat(w)
                .isEqualTo(Wrapper.wrap(new LoanDescriptor(l), FOLIO));
            softly.assertThat(w)
                .isNotEqualTo(Wrapper.wrap(new LoanDescriptor(MockLoanBuilder.fresh()), FOLIO));
            softly.assertThat(w)
                .isNotEqualTo(null);
        });
    }
}
