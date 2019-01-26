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

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.robozonky.api.remote.entities.Restrictions;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.InvestmentStrategy;
import com.github.robozonky.api.strategies.LoanDescriptor;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.api.strategies.RecommendedLoan;
import com.github.robozonky.strategy.natural.conditions.MarketplaceFilter;
import com.github.robozonky.strategy.natural.conditions.MarketplaceFilterCondition;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.*;

class NaturalLanguageInvestmentStrategyTest {

    private static Loan mockLoan(final int amount) {
        return Loan.custom()
                .setId(1)
                .setAmount(amount)
                .setDatePublished(OffsetDateTime.now())
                .setNonReservedRemainingInvestment(amount)
                .setRating(Rating.A)
                .build();
    }

    @Test
    void unacceptablePortfolioDueToLowBalance() {
        final ParsedStrategy p = new ParsedStrategy(DefaultPortfolio.EMPTY);
        final InvestmentStrategy s = new NaturalLanguageInvestmentStrategy(p);
        final PortfolioOverview portfolio = mock(PortfolioOverview.class);
        when(portfolio.getCzkAvailable()).thenReturn(BigDecimal.valueOf(p.getMinimumBalance() - 1));
        final Stream<RecommendedLoan> result = s.recommend(Collections.singletonList(new LoanDescriptor(mockLoan(2))),
                                                           portfolio, new Restrictions());
        assertThat(result).isEmpty();
    }

    @Test
    void unacceptablePortfolioDueToOverInvestment() {
        final DefaultValues v = new DefaultValues(DefaultPortfolio.EMPTY);
        v.setTargetPortfolioSize(1000);
        final ParsedStrategy p = new ParsedStrategy(v, Collections.emptyList(), Collections.emptyMap());
        final InvestmentStrategy s = new NaturalLanguageInvestmentStrategy(p);
        final PortfolioOverview portfolio = mock(PortfolioOverview.class);
        when(portfolio.getCzkAvailable()).thenReturn(BigDecimal.valueOf(10_000));
        when(portfolio.getCzkInvested()).thenReturn(BigDecimal.valueOf(p.getMaximumInvestmentSizeInCzk()));
        final Stream<RecommendedLoan> result = s.recommend(Collections.singletonList(new LoanDescriptor(mockLoan(2))),
                                                           portfolio, new Restrictions());
        assertThat(result).isEmpty();
    }

    @Test
    void noLoansApplicable() {
        final MarketplaceFilter filter = MarketplaceFilter.of(MarketplaceFilterCondition.alwaysAccepting());
        final ParsedStrategy p = new ParsedStrategy(DefaultPortfolio.PROGRESSIVE, Collections.singleton(filter));
        final InvestmentStrategy s = new NaturalLanguageInvestmentStrategy(p);
        final PortfolioOverview portfolio = mock(PortfolioOverview.class);
        when(portfolio.getCzkAvailable()).thenReturn(BigDecimal.valueOf(10_000));
        when(portfolio.getCzkInvested()).thenReturn(BigDecimal.valueOf(p.getMaximumInvestmentSizeInCzk() - 1));
        final Stream<RecommendedLoan> result = s.recommend(Collections.singletonList(new LoanDescriptor(mockLoan(2))),
                                                           portfolio, new Restrictions());
        assertThat(result).isEmpty();
    }

    @Test
    void nothingRecommendedDueToRatingOverinvested() {
        final ParsedStrategy p = new ParsedStrategy(DefaultPortfolio.EMPTY, Collections.emptySet());
        final InvestmentStrategy s = new NaturalLanguageInvestmentStrategy(p);
        final PortfolioOverview portfolio = mock(PortfolioOverview.class);
        when(portfolio.getCzkAvailable()).thenReturn(BigDecimal.valueOf(10_000));
        when(portfolio.getCzkInvested()).thenReturn(BigDecimal.valueOf(p.getMaximumInvestmentSizeInCzk() - 1));
        when(portfolio.getShareOnInvestment(any())).thenReturn(BigDecimal.ZERO);
        final Loan l = mockLoan(1000);
        final Stream<RecommendedLoan> result = s.recommend(Collections.singletonList(new LoanDescriptor(l)),
                                                           portfolio, new Restrictions());
        assertThat(result).isEmpty();
    }

    @Test
    void recommendationIsMade() {
        final ParsedStrategy p = new ParsedStrategy(DefaultPortfolio.PROGRESSIVE, Collections.emptySet());
        final InvestmentStrategy s = new NaturalLanguageInvestmentStrategy(p);
        final PortfolioOverview portfolio = mock(PortfolioOverview.class);
        when(portfolio.getCzkAvailable()).thenReturn(BigDecimal.valueOf(200));
        when(portfolio.getCzkInvested()).thenReturn(BigDecimal.valueOf(p.getMaximumInvestmentSizeInCzk() - 1));
        when(portfolio.getShareOnInvestment(any())).thenReturn(BigDecimal.ZERO);
        final Loan l = mockLoan(100_000);
        final Loan l2 = mockLoan(100);
        final LoanDescriptor ld = new LoanDescriptor(l);
        final List<RecommendedLoan> result =
                s.recommend(Arrays.asList(new LoanDescriptor(l2), ld), portfolio, new Restrictions())
                        .collect(Collectors.toList());
        assertThat(result).hasSize(1);
        final RecommendedLoan r = result.get(0);
        assertSoftly(softly -> {
            softly.assertThat(r.descriptor()).isEqualTo(ld);
            softly.assertThat(r.amount()).isEqualTo(BigDecimal.valueOf(200));
            softly.assertThat(r.isConfirmationRequired()).isFalse();
        });
    }
}
