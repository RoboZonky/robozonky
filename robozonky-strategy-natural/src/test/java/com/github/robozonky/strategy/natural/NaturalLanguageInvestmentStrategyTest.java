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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.Ratio;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.InvestmentStrategy;
import com.github.robozonky.api.strategies.LoanDescriptor;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.internal.remote.entities.LoanImpl;
import com.github.robozonky.strategy.natural.conditions.MarketplaceFilter;
import com.github.robozonky.strategy.natural.conditions.MarketplaceFilterCondition;
import com.github.robozonky.test.AbstractMinimalRoboZonkyTest;
import com.github.robozonky.test.mock.MockLoanBuilder;

class NaturalLanguageInvestmentStrategyTest extends AbstractMinimalRoboZonkyTest {

    private static LoanImpl mockLoan(final int amount) {
        return new MockLoanBuilder()
            .set(LoanImpl::setAmount, Money.from(amount))
            .set(LoanImpl::setRemainingInvestment, Money.from(amount))
            .set(LoanImpl::setReservedAmount, Money.from(0))
            .set(LoanImpl::setDatePublished, OffsetDateTime.now())
            .set(LoanImpl::setInterestRate, Rating.A.getInterestRate())
            .build();
    }

    @Test
    void unacceptablePortfolioDueToOverInvestment() {
        final DefaultValues v = new DefaultValues(DefaultPortfolio.EMPTY);
        v.setTargetPortfolioSize(1000);
        final ParsedStrategy p = new ParsedStrategy(v, Collections.emptyList(), Collections.emptyMap(),
                Collections.emptyMap());
        final InvestmentStrategy s = new NaturalLanguageInvestmentStrategy(p);
        final PortfolioOverview portfolio = mock(PortfolioOverview.class);
        when(portfolio.getInvested()).thenReturn(p.getMaximumInvestmentSize());
        final Optional<Money> result = s.recommend(new LoanDescriptor(mockLoan(2)), () -> portfolio, mockSessionInfo());
        assertThat(result).isEmpty();
    }

    @Test
    void noLoansApplicable() {
        final MarketplaceFilter filter = MarketplaceFilter.of(MarketplaceFilterCondition.alwaysAccepting());
        final ParsedStrategy p = new ParsedStrategy(DefaultPortfolio.PROGRESSIVE, Collections.singleton(filter));
        final InvestmentStrategy s = new NaturalLanguageInvestmentStrategy(p);
        final PortfolioOverview portfolio = mock(PortfolioOverview.class);
        when(portfolio.getShareOnInvestment(any())).thenReturn(Ratio.ZERO);
        when(portfolio.getInvested()).thenReturn(p.getMaximumInvestmentSize()
            .subtract(1));
        final Optional<Money> result = s.recommend(new LoanDescriptor(mockLoan(2)), () -> portfolio, mockSessionInfo());
        assertThat(result).isEmpty();
    }

    @Test
    void nothingRecommendedDueToRatingOverinvested() {
        final ParsedStrategy p = new ParsedStrategy(DefaultPortfolio.PROGRESSIVE, Collections.emptySet());
        final InvestmentStrategy s = new NaturalLanguageInvestmentStrategy(p);
        final PortfolioOverview portfolio = mock(PortfolioOverview.class);
        when(portfolio.getShareOnInvestment(any())).thenReturn(Ratio.ZERO);
        when(portfolio.getInvested()).thenReturn(p.getMaximumInvestmentSize()
            .subtract(1));
        final LoanImpl l = mockLoan(1000);
        var rate = l.getInterestRate();
        when(portfolio.getShareOnInvestment(eq(rate))).thenReturn(Ratio.fromPercentage("100"));
        final Optional<Money> result = s.recommend(new LoanDescriptor(l), () -> portfolio, mockSessionInfo());
        assertThat(result).isEmpty();
    }

    @Test
    void recommendationIsMade() {
        final ParsedStrategy p = new ParsedStrategy(DefaultPortfolio.PROGRESSIVE, Collections.emptySet());
        final InvestmentStrategy s = new NaturalLanguageInvestmentStrategy(p);
        final PortfolioOverview portfolio = mock(PortfolioOverview.class);
        when(portfolio.getInvested()).thenReturn(p.getMaximumInvestmentSize()
            .subtract(1));
        when(portfolio.getShareOnInvestment(any())).thenReturn(Ratio.ZERO);
        final LoanImpl l = mockLoan(100_000);
        final LoanImpl l2 = mockLoan(100);
        final LoanDescriptor ld = new LoanDescriptor(l);
        final Optional<Money> result = s.recommend(new LoanDescriptor(l2), () -> portfolio, mockSessionInfo());
        assertThat(result).isEmpty();
        final Optional<Money> result2 = s.recommend(ld, () -> portfolio, mockSessionInfo());
        assertThat(result2).contains(Money.from(20_000));
    }
}
