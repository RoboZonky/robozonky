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

package com.github.robozonky.strategy.natural.wrappers;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.Ratio;
import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.api.remote.entities.Participation;
import com.github.robozonky.api.remote.enums.LoanHealth;
import com.github.robozonky.api.remote.enums.MainIncomeType;
import com.github.robozonky.api.remote.enums.Purpose;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.remote.enums.Region;
import com.github.robozonky.api.strategies.ParticipationDescriptor;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.internal.remote.entities.LoanImpl;
import com.github.robozonky.internal.remote.entities.ParticipationImpl;
import com.github.robozonky.test.AbstractRoboZonkyTest;
import com.github.robozonky.test.mock.MockLoanBuilder;

class ParticipationWrapperTest extends AbstractRoboZonkyTest {

    private static final PortfolioOverview FOLIO = mockPortfolioOverview();

    private static final Loan LOAN = new MockLoanBuilder()
        .set(LoanImpl::setInsuranceActive, true)
        .set(LoanImpl::setAmount, Money.from(100_000))
        .set(LoanImpl::setRating, Rating.D)
        .set(LoanImpl::setInterestRate, Ratio.ONE)
        .set(LoanImpl::setMainIncomeType, MainIncomeType.EMPLOYMENT)
        .set(LoanImpl::setPurpose, Purpose.AUTO_MOTO)
        .set(LoanImpl::setRegion, Region.JIHOCESKY)
        .set(LoanImpl::setStory, UUID.randomUUID()
            .toString())
        .set(LoanImpl::setTermInMonths, 20)
        .build();
    private static final Participation PARTICIPATION = mockParticipation(LOAN);

    private static Participation mockParticipation(final Loan loan) {
        final Participation p = mock(ParticipationImpl.class);
        when(p.getInterestRate()).thenReturn(Ratio.ONE);
        doReturn(Money.ZERO).when(p)
            .getRemainingPrincipal();
        doReturn(Money.ZERO).when(p)
            .getPrice();
        doReturn(loan.getPurpose()).when(p)
            .getPurpose();
        doReturn(loan.getRating()).when(p)
            .getRating();
        doReturn(loan.getMainIncomeType()).when(p)
            .getIncomeType();
        doReturn(loan.isInsuranceActive()).when(p)
            .isInsuranceActive();
        return p;
    }

    @Test
    void fromParticipation() {
        final Loan loan = new MockLoanBuilder()
            .set(LoanImpl::setInsuranceActive, true)
            .set(LoanImpl::setAmount, Money.from(100_000))
            .set(LoanImpl::setRating, Rating.D)
            .set(LoanImpl::setInterestRate, Ratio.ONE)
            .set(LoanImpl::setMainIncomeType, MainIncomeType.EMPLOYMENT)
            .set(LoanImpl::setPurpose, Purpose.AUTO_MOTO)
            .set(LoanImpl::setRegion, Region.JIHOCESKY)
            .set(LoanImpl::setStory, UUID.randomUUID()
                .toString())
            .set(LoanImpl::setTermInMonths, 20)
            .set(LoanImpl::setAnnuity, Money.from(BigDecimal.ONE))
            .build();
        final int invested = 200;
        final Participation participation = mock(ParticipationImpl.class);
        when(participation.getId()).thenReturn((long) (Math.random() * 1000));
        when(participation.getInterestRate()).thenReturn(Ratio.ONE);
        doReturn(Money.ZERO).when(participation)
            .getRemainingPrincipal();
        doReturn(Money.ZERO).when(participation)
            .getPrice();
        doReturn(20).when(participation)
            .getOriginalInstalmentCount();
        doReturn(loan.getPurpose()).when(participation)
            .getPurpose();
        doReturn(loan.getRating()).when(participation)
            .getRating();
        doReturn(loan.getMainIncomeType()).when(participation)
            .getIncomeType();
        doReturn(true).when(participation)
            .isInsuranceActive();
        final ParticipationDescriptor pd = new ParticipationDescriptor(participation, () -> loan);
        final Wrapper<ParticipationDescriptor> w = Wrapper.wrap(pd, FOLIO);
        assertSoftly(softly -> {
            softly.assertThat(w.getId())
                .isNotEqualTo(0);
            softly.assertThat(w.isInsuranceActive())
                .isEqualTo(participation.isInsuranceActive());
            softly.assertThat(w.getInterestRate())
                .isEqualTo(Ratio.ONE);
            softly.assertThat(w.getRegion())
                .isEqualTo(loan.getRegion());
            softly.assertThat(w.getRating())
                .isEqualTo(participation.getRating());
            softly.assertThat(w.getMainIncomeType())
                .isEqualTo(loan.getMainIncomeType());
            softly.assertThat(w.getPurpose())
                .isEqualTo(loan.getPurpose());
            softly.assertThat(w.getOriginalAmount())
                .isEqualTo(loan.getAmount()
                    .getValue()
                    .intValue());
            softly.assertThat(w.getRemainingPrincipal())
                .isEqualTo(participation.getRemainingPrincipal()
                    .getValue());
            softly.assertThat(w.getOriginal())
                .isSameAs(pd);
            softly.assertThat(w.getStory())
                .isEqualTo(loan.getStory());
            softly.assertThat(w.getOriginalTermInMonths())
                .isEqualTo(participation.getOriginalInstalmentCount());
            softly.assertThat(w.getRemainingTermInMonths())
                .isEqualTo(participation.getRemainingInstalmentCount());
            softly.assertThat(w.getHealth())
                .contains(LoanHealth.HEALTHY);
            softly.assertThat(w.getOriginalPurchasePrice())
                .isEmpty();
            softly.assertThat(w.getPrice())
                .contains(BigDecimal.ZERO);
            softly.assertThat(w.getSellFee())
                .isEmpty();
            softly.assertThat(w.getReturns())
                .isEmpty();
            softly.assertThat(w.toString())
                .isNotNull();
            softly.assertThat(w.getRevenueRate())
                .isEqualTo(Ratio.fromRaw("0.1499"));
            softly.assertThat(w.getOriginalAnnuity())
                .isEqualTo(loan.getAnnuity()
                    .getValue()
                    .intValue());
            softly.assertThat(w.getCurrentDpd())
                .hasValue(0);
            softly.assertThat(w.getLongestDpd())
                .hasValue(0);
            softly.assertThat(w.getDaysSinceDpd())
                .hasValue(0);
        });
    }

    @Test
    void fromParticipationWithoutRevenueRate() {
        final Loan loan = new MockLoanBuilder()
            .set(LoanImpl::setRating, Rating.C)
            .set(LoanImpl::setInsuranceActive, false)
            .build();
        final int invested = 200;
        final Participation p = mock(ParticipationImpl.class);
        doReturn(loan.getRating()).when(p)
            .getRating();
        when(p.getRemainingPrincipal()).thenReturn(Money.from(invested));
        final Wrapper<ParticipationDescriptor> w = Wrapper.wrap(new ParticipationDescriptor(p, () -> loan), FOLIO);
        when(FOLIO.getInvested()).thenReturn(Money.ZERO);
        assertThat(w.getRevenueRate()).isEqualTo(Ratio.fromPercentage("11.49"));
        assertThat(w.isInsuranceActive()).isFalse();
    }

    @Test
    void values() {
        final ParticipationDescriptor original = new ParticipationDescriptor(PARTICIPATION, () -> LOAN);
        final Wrapper<ParticipationDescriptor> w = Wrapper.wrap(original, FOLIO);
        assertSoftly(softly -> {
            softly.assertThat(w.isInsuranceActive())
                .isEqualTo(PARTICIPATION.isInsuranceActive());
            softly.assertThat(w.getInterestRate())
                .isEqualTo(Ratio.ONE);
            softly.assertThat(w.getRegion())
                .isEqualTo(LOAN.getRegion());
            softly.assertThat(w.getRating())
                .isEqualTo(PARTICIPATION.getRating());
            softly.assertThat(w.getMainIncomeType())
                .isEqualTo(LOAN.getMainIncomeType());
            softly.assertThat(w.getPurpose())
                .isEqualTo(LOAN.getPurpose());
            softly.assertThat(w.getOriginalAmount())
                .isEqualTo(LOAN.getAmount()
                    .getValue()
                    .intValue());
            softly.assertThat(w.getRemainingPrincipal())
                .isEqualTo(PARTICIPATION.getRemainingPrincipal()
                    .getValue());
            softly.assertThat(w.getOriginal())
                .isSameAs(original);
            softly.assertThat(w.getStory())
                .isEqualTo(LOAN.getStory());
            softly.assertThat(w.getOriginalTermInMonths())
                .isEqualTo(PARTICIPATION.getOriginalInstalmentCount());
            softly.assertThat(w.getRemainingTermInMonths())
                .isEqualTo(PARTICIPATION.getRemainingInstalmentCount());
            softly.assertThat(w.getHealth())
                .contains(LoanHealth.HEALTHY);
            softly.assertThat(w.getOriginalPurchasePrice())
                .isEmpty();
            softly.assertThat(w.getPrice())
                .contains(BigDecimal.ZERO);
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
        final ParticipationDescriptor original = new ParticipationDescriptor(PARTICIPATION, () -> LOAN);
        final Wrapper<ParticipationDescriptor> w = Wrapper.wrap(original, FOLIO);
        assertSoftly(softly -> {
            softly.assertThat(w)
                .isEqualTo(w);
            softly.assertThat(w)
                .isEqualTo(Wrapper.wrap(original, FOLIO));
            softly.assertThat(w)
                .isEqualTo(Wrapper.wrap(new ParticipationDescriptor(PARTICIPATION, () -> LOAN), FOLIO));
            softly.assertThat(w)
                .isNotEqualTo(null);
        });
    }

}
