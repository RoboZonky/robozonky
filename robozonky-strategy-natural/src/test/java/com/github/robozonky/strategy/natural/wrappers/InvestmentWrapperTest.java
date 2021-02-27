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

import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.Ratio;
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
    private static final InvestmentImpl INVESTMENT = MockInvestmentBuilder
        .fresh(LOAN, new LoanHealthStatsImpl(LoanHealth.HEALTHY), 2_000)
        .set(InvestmentImpl::setSmpSellInfo, new SellInfoImpl(Money.from(BigDecimal.TEN)))
        .build();
    private static final PortfolioOverview FOLIO = mockPortfolioOverview();

    @Test
    void fromInvestment() {
        var invested = new BigDecimal("200.00");
        var investment = MockInvestmentBuilder
            .fresh(LOAN, new LoanHealthStatsImpl(LoanHealth.HEALTHY), invested)
            .set(InvestmentImpl::setSellStatus, SellStatus.SELLABLE_WITHOUT_FEE)
            .set(InvestmentImpl::setInterest, new AmountsImpl(Money.from(10), Money.from(1)))
            .build();
        var investmentLoanData = (InvestmentLoanDataImpl) investment.getLoan();
        investmentLoanData.setPayments(new InstalmentsImpl(20, 10));
        var investmentDescriptor = new InvestmentDescriptor(investment, () -> LOAN);
        var wrapper = Wrapper.wrap(investmentDescriptor, FOLIO);
        assertSoftly(softly -> {
            softly.assertThat(wrapper.getId())
                .isEqualTo(investment.getId());
            softly.assertThat(wrapper.isInsuranceActive())
                .isEqualTo(investment.getLoan()
                    .getDetailLabels()
                    .contains(DetailLabel.CURRENTLY_INSURED));
            softly.assertThat(wrapper.getInterestRate())
                .isEqualTo(Ratio.fromPercentage(19.99));
            softly.assertThat(wrapper.getRegion())
                .isEqualTo(LOAN.getRegion());
            softly.assertThat(wrapper.getMainIncomeType())
                .isEqualTo(LOAN.getMainIncomeType());
            softly.assertThat(wrapper.getPurpose())
                .isEqualTo(LOAN.getPurpose());
            softly.assertThat(wrapper.getOriginalAmount())
                .isEqualTo(LOAN.getAmount()
                    .getValue()
                    .intValue());
            softly.assertThat(wrapper.getRemainingPrincipal())
                .isEqualTo(investment.getPrincipal()
                    .getUnpaid()
                    .getValue());
            softly.assertThat(wrapper.getOriginal())
                .isSameAs(investmentDescriptor);
            softly.assertThat(wrapper.getStory())
                .isEqualTo(LOAN.getStory());
            softly.assertThat(wrapper.getOriginalTermInMonths())
                .isEqualTo(investment.getLoan()
                    .getPayments()
                    .getTotal());
            softly.assertThat(wrapper.getRemainingTermInMonths())
                .isEqualTo(investment.getLoan()
                    .getPayments()
                    .getUnpaid());
            softly.assertThat(wrapper.getHealth())
                .contains(LoanHealth.HEALTHY);
            softly.assertThat(wrapper.getOriginalPurchasePrice())
                .contains(invested);
            softly.assertThat(wrapper.getSellPrice())
                .contains(invested);
            softly.assertThat(wrapper.getSellFee())
                .contains(BigDecimal.ZERO);
            softly.assertThat(wrapper.getReturns())
                .contains(new BigDecimal("9.00"));
            softly.assertThat(wrapper.getRevenueRate())
                .isEqualTo(Ratio.fromRaw("0.1499"));
            softly.assertThat(wrapper.getOriginalAnnuity())
                .isEqualTo(LOAN.getAnnuity()
                    .getValue()
                    .intValue());
            softly.assertThat(wrapper.getCurrentDpd())
                .hasValue(0);
            softly.assertThat(wrapper.getLongestDpd())
                .hasValue(0);
            softly.assertThat(wrapper.getDaysSinceDpd())
                .hasValue(0);
            softly.assertThat(wrapper.toString())
                .isNotNull();
        });
    }

    @Test
    void sellInfoValues() {
        var feeInfo = new SellFeeImpl(Money.from(2));
        var sellInfo = new SellInfoImpl(Money.from(10));
        sellInfo.setFee(feeInfo);
        sellInfo.setDiscount(Ratio.fromPercentage(10));
        var investment = MockInvestmentBuilder
            .fresh(LOAN, new LoanHealthStatsImpl(LoanHealth.CURRENTLY_IN_DUE), BigDecimal.valueOf(200))
            .set(InvestmentImpl::setSellStatus, SellStatus.SELLABLE_WITH_FEE)
            .set(InvestmentImpl::setSmpSellInfo, sellInfo)
            .set(InvestmentImpl::setInterest, new AmountsImpl(Money.from(10), Money.from(1)))
            .build();
        var investmentLoanData = (InvestmentLoanDataImpl) investment.getLoan();
        investmentLoanData.setDpd(1);
        var investmentDescriptor = new InvestmentDescriptor(investment, () -> LOAN);
        var wrapper = Wrapper.wrap(investmentDescriptor, FOLIO);
        assertSoftly(softly -> {
            softly.assertThat(wrapper.getHealth())
                .contains(LoanHealth.CURRENTLY_IN_DUE);
            softly.assertThat(wrapper.getSellPrice())
                .contains(new BigDecimal("10.00"));
            softly.assertThat(wrapper.getSellFee())
                .contains(new BigDecimal("2.00"));
            softly.assertThat(wrapper.getReturns())
                .contains(new BigDecimal("7.00"));
        });
    }

    @Test
    void equality() {
        var investmentDescriptor = new InvestmentDescriptor(INVESTMENT, () -> LOAN);
        var wrapper = Wrapper.wrap(investmentDescriptor, FOLIO);
        assertSoftly(softly -> {
            softly.assertThat(wrapper)
                .isEqualTo(wrapper);
            softly.assertThat(wrapper)
                .isEqualTo(Wrapper.wrap(investmentDescriptor, FOLIO));
            softly.assertThat(wrapper)
                .isEqualTo(Wrapper.wrap(new InvestmentDescriptor(INVESTMENT, () -> LOAN), FOLIO));
            softly.assertThat(wrapper)
                .isNotEqualTo(null);
        });
    }
}
