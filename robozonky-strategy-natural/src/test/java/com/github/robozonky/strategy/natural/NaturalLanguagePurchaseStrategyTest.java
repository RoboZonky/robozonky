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
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.ParticipationDescriptor;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.api.strategies.PurchaseStrategy;
import com.github.robozonky.api.strategies.RecommendedParticipation;
import com.github.robozonky.internal.api.Defaults;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public class NaturalLanguagePurchaseStrategyTest {

    private final ParticipationDescriptor mockDescriptor() {
        return mockDescriptor(mock());
    }

    private final ParticipationDescriptor mockDescriptor(final Participation participation) {
        final Loan l = new Loan(1, 2);
        return new ParticipationDescriptor(participation, l);
    }

    private final Participation mock() {
        final Participation p = Mockito.mock(Participation.class);
        return p;
    }

    @Test
    public void unacceptablePortfolioDueToLowBalance() {
        final ParsedStrategy p = new ParsedStrategy(DefaultPortfolio.EMPTY);
        final PurchaseStrategy s = new NaturalLanguagePurchaseStrategy(p);
        final PortfolioOverview portfolio = Mockito.mock(PortfolioOverview.class);
        Mockito.when(portfolio.getCzkAvailable()).thenReturn(p.getMinimumBalance() - 1);
        final Stream<RecommendedParticipation> result =
                s.recommend(Collections.singletonList(mockDescriptor()), portfolio);
        Assertions.assertThat(result).isEmpty();
    }

    @Test
    public void unacceptablePortfolioDueToOverInvestment() {
        final DefaultValues v = new DefaultValues(DefaultPortfolio.EMPTY);
        v.setTargetPortfolioSize(1000);
        final ParsedStrategy p = new ParsedStrategy(v, Collections.emptyList(), Collections.emptyList(),
                                                    Collections.emptyMap(), Collections.emptyList());
        final PurchaseStrategy s = new NaturalLanguagePurchaseStrategy(p);
        final PortfolioOverview portfolio = Mockito.mock(PortfolioOverview.class);
        Mockito.when(portfolio.getCzkAvailable()).thenReturn(p.getMinimumBalance());
        Mockito.when(portfolio.getCzkInvested()).thenReturn(p.getMaximumInvestmentSizeInCzk());
        final Stream<RecommendedParticipation> result =
                s.recommend(Collections.singletonList(mockDescriptor()), portfolio);
        Assertions.assertThat(result).isEmpty();
    }

    @Test
    public void noLoansApplicable() {
        final MarketplaceFilter filter = new MarketplaceFilter();
        filter.ignoreWhen(Collections.singleton(new MarketplaceFilterCondition() {
            @Override
            public boolean test(final Wrapper loan) {
                return true;
            }
        }));
        final ParsedStrategy p =
                new ParsedStrategy(DefaultPortfolio.PROGRESSIVE,
                                   Collections.singletonMap(Boolean.FALSE, Collections.singleton(filter)));
        final PurchaseStrategy s = new NaturalLanguagePurchaseStrategy(p);
        final PortfolioOverview portfolio = Mockito.mock(PortfolioOverview.class);
        Mockito.when(portfolio.getCzkAvailable()).thenReturn(p.getMinimumBalance());
        Mockito.when(portfolio.getCzkInvested()).thenReturn(p.getMaximumInvestmentSizeInCzk() - 1);
        final Stream<RecommendedParticipation> result =
                s.recommend(Collections.singletonList(mockDescriptor()), portfolio);
        Assertions.assertThat(result).isEmpty();
    }

    @Test
    public void nothingRecommendedDueToRatingOverinvested() {
        final ParsedStrategy p = new ParsedStrategy(DefaultPortfolio.EMPTY);
        final PurchaseStrategy s = new NaturalLanguagePurchaseStrategy(p);
        final PortfolioOverview portfolio = Mockito.mock(PortfolioOverview.class);
        Mockito.when(portfolio.getCzkAvailable()).thenReturn(p.getMinimumBalance());
        Mockito.when(portfolio.getCzkInvested()).thenReturn(p.getMaximumInvestmentSizeInCzk() - 1);
        Mockito.when(portfolio.getShareOnInvestment(ArgumentMatchers.any())).thenReturn(BigDecimal.ZERO);
        final Participation l = mock();
        Mockito.doReturn(Rating.A).when(l).getRating();
        final Stream<RecommendedParticipation> result =
                s.recommend(Collections.singletonList(mockDescriptor(l)), portfolio);
        Assertions.assertThat(result).isEmpty();
    }

    @Test
    public void recommendationIsMade() {
        final ParsedStrategy ps = new ParsedStrategy(DefaultPortfolio.PROGRESSIVE);
        final PurchaseStrategy s = new NaturalLanguagePurchaseStrategy(ps);
        final PortfolioOverview portfolio = Mockito.mock(PortfolioOverview.class);
        Mockito.when(portfolio.getCzkAvailable()).thenReturn(ps.getMinimumBalance());
        Mockito.when(portfolio.getCzkInvested()).thenReturn(ps.getMaximumInvestmentSizeInCzk() - 1);
        Mockito.when(portfolio.getShareOnInvestment(ArgumentMatchers.any())).thenReturn(BigDecimal.ZERO);
        final Participation p = Mockito.spy(mock());
        Mockito.doReturn(BigDecimal.valueOf(100000)).when(p).getRemainingPrincipal();  // not recommended due to balance
        Mockito.doReturn(Rating.A).when(p).getRating();
        final Participation p2 = Mockito.spy(mock());
        final int amount = Defaults.MINIMUM_INVESTMENT_IN_CZK - 1; // check amounts under Zonky investment minimum
        Mockito.doReturn(BigDecimal.valueOf(amount)).when(p2).getRemainingPrincipal();
        Mockito.doReturn(Rating.A).when(p2).getRating();
        final ParticipationDescriptor pd = mockDescriptor(p2);
        final List<RecommendedParticipation> result =
                s.recommend(Arrays.asList(mockDescriptor(p), pd), portfolio).collect(Collectors.toList());
        Assertions.assertThat(result).hasSize(1);
        final RecommendedParticipation r = result.get(0);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(r.descriptor()).isEqualTo(pd);
            softly.assertThat(r.amount()).isEqualTo(pd.item().getRemainingPrincipal());
        });
    }
}
