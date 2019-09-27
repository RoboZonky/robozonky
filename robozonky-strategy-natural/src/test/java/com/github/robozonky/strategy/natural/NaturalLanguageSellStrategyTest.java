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

package com.github.robozonky.strategy.natural;

import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.strategies.InvestmentDescriptor;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.api.strategies.RecommendedInvestment;
import com.github.robozonky.api.strategies.SellStrategy;
import com.github.robozonky.test.mock.MockLoanBuilder;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class NaturalLanguageSellStrategyTest {

    private InvestmentDescriptor mockDescriptor() {
        return mockDescriptor(mockInvestment());
    }

    private InvestmentDescriptor mockDescriptor(final Investment investment) {
        final Loan l = new MockLoanBuilder()
                .setAmount(100_000)
                .build();
        return new InvestmentDescriptor(investment, () -> l);
    }

    private static Investment mockInvestment() {
        return mockInvestment(BigDecimal.TEN);
    }

    private static Investment mockInvestment(final BigDecimal fee) {
        return Investment.custom()
                .setRemainingPrincipal(BigDecimal.TEN)
                .setSmpFee(fee)
                .build();
    }

    @Test
    void noLoansApplicable() {
        final DefaultValues v = new DefaultValues(DefaultPortfolio.PROGRESSIVE);
        v.setSellingMode(SellingMode.SELL_FILTERS);
        final ParsedStrategy p = spy(new ParsedStrategy(v));
        doReturn(Stream.empty()).when(p).getMatchingSellFilters(any(), any());
        final SellStrategy s = new NaturalLanguageSellStrategy(p);
        final PortfolioOverview portfolio = mock(PortfolioOverview.class);
        final Stream<RecommendedInvestment> result =
                s.recommend(Collections.singletonList(mockDescriptor()), portfolio);
        assertThat(result).isEmpty();
    }

    @Test
    void someLoansApplicable() {
        final DefaultValues v = new DefaultValues(DefaultPortfolio.PROGRESSIVE);
        v.setSellingMode(SellingMode.SELL_FILTERS);
        final ParsedStrategy p = spy(new ParsedStrategy(v));
        doAnswer(e -> {
            final Collection<InvestmentDescriptor> i = e.getArgument(0);
            return i.stream();
        }).when(p).getMatchingSellFilters(any(), any());
        final SellStrategy s = new NaturalLanguageSellStrategy(p);
        final PortfolioOverview portfolio = mock(PortfolioOverview.class);
        final Stream<RecommendedInvestment> result =
                s.recommend(Collections.singletonList(mockDescriptor()), portfolio);
        assertThat(result).hasSize(1);
    }

    @Test
    void feeBasedInvestmentsNotApplicableInSelloffStrategy() {
        final DefaultValues v = new DefaultValues(DefaultPortfolio.PROGRESSIVE);
        v.setSellingMode(SellingMode.FREE_AND_OUTSIDE_STRATEGY);
        final ParsedStrategy p = spy(new ParsedStrategy(v));
        doAnswer(e -> {
            final Collection<InvestmentDescriptor> i = e.getArgument(0);
            return i.stream();
        }).when(p).getMatchingPrimaryMarketplaceFilters(any(), any());
        final SellStrategy s = new NaturalLanguageSellStrategy(p);
        final PortfolioOverview portfolio = mock(PortfolioOverview.class);
        final Investment i1 = mockInvestment();
        final Investment i2 = mockInvestment(BigDecimal.ZERO);
        final Stream<RecommendedInvestment> result =
                s.recommend(Arrays.asList(mockDescriptor(i1), mockDescriptor(i2)), portfolio);
        assertThat(result).extracting(d -> d.descriptor().item()).containsOnly(i2);
    }
}
