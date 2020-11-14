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

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.remote.entities.Investment;
import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.remote.enums.SellStatus;
import com.github.robozonky.api.strategies.InvestmentDescriptor;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.api.strategies.RecommendedInvestment;
import com.github.robozonky.api.strategies.SellStrategy;
import com.github.robozonky.internal.remote.entities.InvestmentImpl;
import com.github.robozonky.internal.remote.entities.LoanImpl;
import com.github.robozonky.internal.remote.entities.SellInfoImpl;
import com.github.robozonky.test.AbstractMinimalRoboZonkyTest;
import com.github.robozonky.test.mock.MockInvestmentBuilder;
import com.github.robozonky.test.mock.MockLoanBuilder;

class NaturalLanguageSellStrategyTest extends AbstractMinimalRoboZonkyTest {

    private static final Loan LOAN = new MockLoanBuilder()
        .set(LoanImpl::setAmount, Money.from(100_000))
        .set(LoanImpl::setRating, Rating.A)
        .build();

    private static Investment mockInvestment() {
        return mockInvestment(BigDecimal.TEN);
    }

    private static Investment mockInvestment(final BigDecimal fee) {
        return MockInvestmentBuilder.fresh(LOAN, 10)
            .set(InvestmentImpl::setSellStatus,
                    fee.equals(BigDecimal.ZERO) ? SellStatus.SELLABLE_WITHOUT_FEE : SellStatus.SELLABLE_WITH_FEE)
            .set(InvestmentImpl::setSmpSellInfo, new SellInfoImpl(Money.from(10), Money.from(fee)))
            .build();
    }

    private static InvestmentDescriptor mockDescriptor() {
        return mockDescriptor(mockInvestment());
    }

    private static InvestmentDescriptor mockDescriptor(final Investment investment) {
        return new InvestmentDescriptor(investment, () -> LOAN);
    }

    @Test
    void noLoansApplicable() {
        final DefaultValues v = new DefaultValues(DefaultPortfolio.PROGRESSIVE);
        v.setSellingMode(SellingMode.SELL_FILTERS);
        final ParsedStrategy p = spy(new ParsedStrategy(v));
        doReturn(Stream.empty()).when(p)
            .getMatchingSellFilters(any(), any());
        final SellStrategy s = new NaturalLanguageSellStrategy(p);
        final PortfolioOverview portfolio = mock(PortfolioOverview.class);
        final Stream<RecommendedInvestment> result = s.recommend(Collections.singletonList(mockDescriptor()),
                portfolio, mockSessionInfo());
        assertThat(result).isEmpty();
    }

    @Test
    void someLoansApplicable() {
        final DefaultValues v = new DefaultValues(DefaultPortfolio.PROGRESSIVE);
        v.setSellingMode(SellingMode.SELL_FILTERS);
        final ParsedStrategy p = spy(new ParsedStrategy(v));
        doAnswer(e -> e.getArgument(0)).when(p)
            .getMatchingSellFilters(any(), any());
        final SellStrategy s = new NaturalLanguageSellStrategy(p);
        final PortfolioOverview portfolio = mock(PortfolioOverview.class);
        final Stream<RecommendedInvestment> result = s.recommend(Collections.singletonList(mockDescriptor()),
                portfolio, mockSessionInfo());
        assertThat(result).hasSize(1);
    }

    @Test
    void feeBasedInvestmentsNotApplicableInSelloffStrategy() {
        final DefaultValues v = new DefaultValues(DefaultPortfolio.PROGRESSIVE);
        v.setSellingMode(SellingMode.FREE_AND_OUTSIDE_STRATEGY);
        final ParsedStrategy p = spy(new ParsedStrategy(v));
        doAnswer(e -> e.getArgument(0)).when(p)
            .getMatchingPrimaryMarketplaceFilters(any(), any());
        final SellStrategy s = new NaturalLanguageSellStrategy(p);
        final PortfolioOverview portfolio = mock(PortfolioOverview.class);
        final Investment i1 = mockInvestment();
        final Investment i2 = mockInvestment(BigDecimal.ZERO);
        final Stream<RecommendedInvestment> result = s.recommend(asList(mockDescriptor(i1), mockDescriptor(i2)),
                portfolio, mockSessionInfo());
        assertThat(result).extracting(d -> d.descriptor()
            .item())
            .containsOnly(i2);
    }

}
