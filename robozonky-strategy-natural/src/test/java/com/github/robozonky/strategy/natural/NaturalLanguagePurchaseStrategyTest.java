/*
 * Copyright 2017 The RoboZonky Project
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.api.remote.entities.Participation;
import com.github.robozonky.api.remote.entities.Restrictions;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.ParticipationDescriptor;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.api.strategies.PurchaseStrategy;
import com.github.robozonky.api.strategies.RecommendedParticipation;
import com.github.robozonky.internal.api.Defaults;
import com.github.robozonky.strategy.natural.conditions.MarketplaceFilter;
import com.github.robozonky.strategy.natural.conditions.MarketplaceFilterCondition;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.*;
import static org.mockito.Mockito.*;

class NaturalLanguagePurchaseStrategyTest {

    private ParticipationDescriptor mockDescriptor() {
        return mockDescriptor(mockParticipation());
    }

    private ParticipationDescriptor mockDescriptor(final Participation participation) {
        final Loan l = new Loan(1, 2);
        return new ParticipationDescriptor(participation, l);
    }

    private final Participation mockParticipation() {
        final Participation p = mock(Participation.class);
        return p;
    }

    @Test
    void unacceptablePortfolioDueToLowBalance() {
        final ParsedStrategy p = new ParsedStrategy(DefaultPortfolio.EMPTY);
        final PurchaseStrategy s = new NaturalLanguagePurchaseStrategy(p);
        final PortfolioOverview portfolio = mock(PortfolioOverview.class);
        when(portfolio.getCzkAvailable()).thenReturn(p.getMinimumBalance() - 1);
        final Stream<RecommendedParticipation> result =
                s.recommend(Collections.singletonList(mockDescriptor()), portfolio, new Restrictions());
        assertThat(result).isEmpty();
    }

    @Test
    void unacceptablePortfolioDueToOverInvestment() {
        final DefaultValues v = new DefaultValues(DefaultPortfolio.EMPTY);
        v.setTargetPortfolioSize(1000);
        final ParsedStrategy p = new ParsedStrategy(v);
        final PurchaseStrategy s = new NaturalLanguagePurchaseStrategy(p);
        final PortfolioOverview portfolio = mock(PortfolioOverview.class);
        when(portfolio.getCzkAvailable()).thenReturn(p.getMinimumBalance());
        when(portfolio.getCzkInvested()).thenReturn(p.getMaximumInvestmentSizeInCzk());
        final Stream<RecommendedParticipation> result =
                s.recommend(Collections.singletonList(mockDescriptor()), portfolio, new Restrictions());
        assertThat(result).isEmpty();
    }

    @Test
    void noLoansApplicable() {
        final MarketplaceFilter filter = MarketplaceFilter.of(MarketplaceFilterCondition.alwaysAccepting());
        final DefaultValues v = new DefaultValues(DefaultPortfolio.PROGRESSIVE);
        final FilterSupplier w = new FilterSupplier(v, Collections.emptySet(), Collections.singleton(filter));
        final ParsedStrategy p = new ParsedStrategy(v, Collections.emptySet(), Collections.emptyMap(), w);
        final PurchaseStrategy s = new NaturalLanguagePurchaseStrategy(p);
        final PortfolioOverview portfolio = mock(PortfolioOverview.class);
        when(portfolio.getCzkAvailable()).thenReturn(p.getMinimumBalance());
        when(portfolio.getCzkInvested()).thenReturn(p.getMaximumInvestmentSizeInCzk() - 1);
        final Stream<RecommendedParticipation> result =
                s.recommend(Collections.singletonList(mockDescriptor()), portfolio, new Restrictions());
        assertThat(result).isEmpty();
    }

    @Test
    void nothingRecommendedDueToRatingOverinvested() {
        final ParsedStrategy p = new ParsedStrategy(DefaultPortfolio.EMPTY);
        final PurchaseStrategy s = new NaturalLanguagePurchaseStrategy(p);
        final PortfolioOverview portfolio = mock(PortfolioOverview.class);
        when(portfolio.getCzkAvailable()).thenReturn(p.getMinimumBalance());
        when(portfolio.getCzkInvested()).thenReturn(p.getMaximumInvestmentSizeInCzk() - 1);
        when(portfolio.getShareOnInvestment(any())).thenReturn(BigDecimal.ZERO);
        final Participation l = mockParticipation();
        doReturn(Rating.A).when(l).getRating();
        final Stream<RecommendedParticipation> result =
                s.recommend(Collections.singletonList(mockDescriptor(l)), portfolio, new Restrictions());
        assertThat(result).isEmpty();
    }

    @Test
    void recommendationIsMade() {
        final DefaultValues v = new DefaultValues(DefaultPortfolio.PROGRESSIVE);
        final ParsedStrategy ps = new ParsedStrategy(v, Collections.emptyList(), Collections.emptyMap(),
                                                     new FilterSupplier(v, null, Collections.emptySet()));
        final PurchaseStrategy s = new NaturalLanguagePurchaseStrategy(ps);
        final PortfolioOverview portfolio = mock(PortfolioOverview.class);
        when(portfolio.getCzkAvailable()).thenReturn(ps.getMinimumBalance());
        when(portfolio.getCzkInvested()).thenReturn(ps.getMaximumInvestmentSizeInCzk() - 1);
        when(portfolio.getShareOnInvestment(any())).thenReturn(BigDecimal.ZERO);
        final Participation p = spy(mockParticipation());
        doReturn(BigDecimal.valueOf(100000)).when(p).getRemainingPrincipal();  // not recommended due to balance
        doReturn(Rating.A).when(p).getRating();
        final Participation p2 = spy(mockParticipation());
        final int amount = Defaults.MINIMUM_INVESTMENT_IN_CZK - 1; // check amounts under Zonky investment minimum
        doReturn(BigDecimal.valueOf(amount)).when(p2).getRemainingPrincipal();
        doReturn(Rating.A).when(p2).getRating();
        final ParticipationDescriptor pd = mockDescriptor(p2);
        final List<RecommendedParticipation> result =
                s.recommend(Arrays.asList(mockDescriptor(p), pd), portfolio, new Restrictions())
                        .collect(Collectors.toList());
        assertThat(result).hasSize(1);
        final RecommendedParticipation r = result.get(0);
        assertSoftly(softly -> {
            softly.assertThat(r.descriptor()).isEqualTo(pd);
            softly.assertThat(r.amount()).isEqualTo(pd.item().getRemainingPrincipal());
        });
    }
}
