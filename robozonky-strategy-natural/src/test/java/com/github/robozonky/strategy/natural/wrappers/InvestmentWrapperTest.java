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

import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.Ratio;
import com.github.robozonky.api.remote.entities.Investment;
import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.api.remote.enums.DetailLabel;
import com.github.robozonky.api.remote.enums.LoanHealth;
import com.github.robozonky.api.remote.enums.MainIncomeType;
import com.github.robozonky.api.remote.enums.Purpose;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.remote.enums.Region;
import com.github.robozonky.api.remote.enums.SellStatus;
import com.github.robozonky.api.strategies.InvestmentDescriptor;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.internal.remote.entities.AmountsImpl;
import com.github.robozonky.internal.remote.entities.InstalmentsImpl;
import com.github.robozonky.internal.remote.entities.InvestmentImpl;
import com.github.robozonky.internal.remote.entities.InvestmentLoanDataImpl;
import com.github.robozonky.internal.remote.entities.LoanHealthStatsImpl;
import com.github.robozonky.internal.remote.entities.LoanImpl;
import com.github.robozonky.internal.remote.entities.SellFeeImpl;
import com.github.robozonky.internal.remote.entities.SellInfoImpl;
import com.github.robozonky.test.AbstractRoboZonkyTest;
import com.github.robozonky.test.mock.MockInvestmentBuilder;
import com.github.robozonky.test.mock.MockLoanBuilder;

class InvestmentWrapperTest extends AbstractRoboZonkyTest {

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
    private static final InvestmentImpl INVESTMENT = MockInvestmentBuilder
        .fresh(LOAN, new LoanHealthStatsImpl(LoanHealth.HEALTHY), 2_000)
        .set(InvestmentImpl::setSmpSellInfo, new SellInfoImpl(Money.from(BigDecimal.TEN)))
        .build();
    private static final PortfolioOverview FOLIO = mockPortfolioOverview();

    @Test
    void fromInvestment() {
        final Loan loan = new MockLoanBuilder()
            .set(LoanImpl::setInsuranceActive, false)
            .set(LoanImpl::setAmount, Money.from(100_000))
            .set(LoanImpl::setRating, Rating.D)
            .set(LoanImpl::setInterestRate, Ratio.fromPercentage(19.99))
            .set(LoanImpl::setMainIncomeType, MainIncomeType.EMPLOYMENT)
            .set(LoanImpl::setPurpose, Purpose.AUTO_MOTO)
            .set(LoanImpl::setRegion, Region.JIHOCESKY)
            .set(LoanImpl::setStory, UUID.randomUUID()
                .toString())
            .set(LoanImpl::setTermInMonths, 20)
            .set(LoanImpl::setAnnuity, Money.from(BigDecimal.ONE))
            .build();
        final int invested = 200;
        final InvestmentImpl investment = MockInvestmentBuilder
            .fresh(loan, new LoanHealthStatsImpl(LoanHealth.HEALTHY), invested)
            .set(InvestmentImpl::setSellStatus, SellStatus.SELLABLE_WITH_FEE)
            .set(InvestmentImpl::setSmpSellInfo, new SellInfoImpl(Money.from(1), Money.from(1)))
            .set(InvestmentImpl::setInterest, new AmountsImpl(Money.from(1)))
            .build();
        InvestmentLoanDataImpl ild = (InvestmentLoanDataImpl) investment.getLoan();
        ild.setPayments(new InstalmentsImpl(20, 10));
        final InvestmentDescriptor id = new InvestmentDescriptor(investment, () -> loan);
        final Wrapper<InvestmentDescriptor> w = Wrapper.wrap(id, FOLIO);
        assertSoftly(softly -> {
            softly.assertThat(w.getId())
                .isEqualTo(investment.getId());
            softly.assertThat(w.isInsuranceActive())
                .isEqualTo(investment.getLoan()
                    .getDetailLabels()
                    .contains(DetailLabel.CURRENTLY_INSURED));
            softly.assertThat(w.getInterestRate())
                .isEqualTo(Ratio.fromPercentage(19.99));
            softly.assertThat(w.getRegion())
                .isEqualTo(loan.getRegion());
            softly.assertThat(w.getRating())
                .isEqualTo(investment.getLoan()
                    .getRating());
            softly.assertThat(w.getMainIncomeType())
                .isEqualTo(loan.getMainIncomeType());
            softly.assertThat(w.getPurpose())
                .isEqualTo(loan.getPurpose());
            softly.assertThat(w.getOriginalAmount())
                .isEqualTo(loan.getAmount()
                    .getValue()
                    .intValue());
            softly.assertThat(w.getRemainingPrincipal())
                .isEqualTo(investment.getPrincipal()
                    .getUnpaid()
                    .getValue());
            softly.assertThat(w.getOriginal())
                .isSameAs(id);
            softly.assertThat(w.getStory())
                .isEqualTo(loan.getStory());
            softly.assertThat(w.getOriginalTermInMonths())
                .isEqualTo(investment.getLoan()
                    .getPayments()
                    .getTotal());
            softly.assertThat(w.getRemainingTermInMonths())
                .isEqualTo(investment.getLoan()
                    .getPayments()
                    .getUnpaid());
            softly.assertThat(w.getHealth())
                .contains(LoanHealth.HEALTHY);
            softly.assertThat(w.getOriginalPurchasePrice())
                .contains(new BigDecimal("1.00"));
            softly.assertThat(w.getPrice())
                .contains(new BigDecimal("1.00"));
            softly.assertThat(w.getSellFee())
                .contains(new BigDecimal("1.00"));
            softly.assertThat(w.getReturns())
                .contains(BigDecimal.ZERO);
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
            softly.assertThat(w.toString())
                .isNotNull();
        });
    }

    @Test
    void sellInfoValues() {
        final SellFeeImpl feeInfo = new SellFeeImpl(Money.from(2));
        final SellInfoImpl sellInfo = new SellInfoImpl(Money.from(10));
        sellInfo.setFee(feeInfo);
        sellInfo.setDiscount(Ratio.fromPercentage(10));
        final Investment investment = MockInvestmentBuilder.fresh(LOAN, BigDecimal.valueOf(200))
            .set(InvestmentImpl::setSellStatus, SellStatus.SELLABLE_WITH_FEE)
            .set(InvestmentImpl::setSmpSellInfo, sellInfo)
            .build();
        final InvestmentDescriptor original = new InvestmentDescriptor(investment, () -> LOAN);
        final Wrapper<InvestmentDescriptor> w = Wrapper.wrap(original, FOLIO);
        assertSoftly(softly -> {
            softly.assertThat(w.getHealth())
                .contains(LoanHealth.HEALTHY);
            softly.assertThat(w.getPrice())
                .contains(new BigDecimal("10.00"));
            softly.assertThat(w.getSellFee())
                .contains(new BigDecimal("2.00"));
        });
    }

    @Test
    void equality() {
        final InvestmentDescriptor original = new InvestmentDescriptor(INVESTMENT, () -> LOAN);
        final Wrapper<InvestmentDescriptor> w = Wrapper.wrap(original, FOLIO);
        assertSoftly(softly -> {
            softly.assertThat(w)
                .isEqualTo(w);
            softly.assertThat(w)
                .isEqualTo(Wrapper.wrap(original, FOLIO));
            softly.assertThat(w)
                .isEqualTo(Wrapper.wrap(new InvestmentDescriptor(INVESTMENT, () -> LOAN), FOLIO));
            softly.assertThat(w)
                .isNotEqualTo(null);
        });
    }
}
