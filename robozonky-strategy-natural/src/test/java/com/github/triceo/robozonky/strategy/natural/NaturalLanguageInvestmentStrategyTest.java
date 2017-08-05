/*
 * Copyright 2017 Lukáš Petrovický
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

package com.github.triceo.robozonky.strategy.natural;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.triceo.robozonky.api.remote.entities.Loan;
import com.github.triceo.robozonky.api.remote.enums.Rating;
import com.github.triceo.robozonky.api.strategies.InvestmentStrategy;
import com.github.triceo.robozonky.api.strategies.LoanDescriptor;
import com.github.triceo.robozonky.api.strategies.PortfolioOverview;
import com.github.triceo.robozonky.api.strategies.RecommendedLoan;
import com.github.triceo.robozonky.internal.api.Defaults;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public class NaturalLanguageInvestmentStrategyTest {

    @Test
    public void unacceptablePortfolioDueToLowBalance() {
        final ParsedStrategy p = new ParsedStrategy(DefaultPortfolio.EMPTY);
        final InvestmentStrategy s = new NaturalLanguageInvestmentStrategy(p);
        final PortfolioOverview portfolio = Mockito.mock(PortfolioOverview.class);
        Mockito.when(portfolio.getCzkAvailable()).thenReturn(p.getMinimumBalance() - 1);
        final Stream<RecommendedLoan> result =
                s.recommend(Collections.singletonList(new LoanDescriptor(new Loan(1, 2))), portfolio);
        Assertions.assertThat(result).isEmpty();
    }

    @Test
    public void unacceptablePortfolioDueToOverInvestment() {
        final DefaultValues v = new DefaultValues(DefaultPortfolio.EMPTY);
        v.setTargetPortfolioSize(1000);
        final ParsedStrategy p = new ParsedStrategy(v, Collections.emptyList(), Collections.emptyList(),
                                                    Collections.emptyList());
        final InvestmentStrategy s = new NaturalLanguageInvestmentStrategy(p);
        final PortfolioOverview portfolio = Mockito.mock(PortfolioOverview.class);
        Mockito.when(portfolio.getCzkAvailable()).thenReturn(p.getMinimumBalance());
        Mockito.when(portfolio.getCzkInvested()).thenReturn(p.getMaximumInvestmentSizeInCzk());
        final Stream<RecommendedLoan> result =
                s.recommend(Collections.singletonList(new LoanDescriptor(new Loan(1, 2))), portfolio);
        Assertions.assertThat(result).isEmpty();
    }

    @Test
    public void noLoansApplicable() {
        final MarketplaceFilter filter = new MarketplaceFilter();
        filter.ignoreWhen(Collections.singleton(new PrimaryMarketplaceFilterCondition() {
            @Override
            public boolean test(final Loan loan) {
                return true;
            }
        }));
        final ParsedStrategy p = new ParsedStrategy(DefaultPortfolio.PROGRESSIVE, Collections.singleton(filter));
        final InvestmentStrategy s = new NaturalLanguageInvestmentStrategy(p);
        final PortfolioOverview portfolio = Mockito.mock(PortfolioOverview.class);
        Mockito.when(portfolio.getCzkAvailable()).thenReturn(p.getMinimumBalance());
        Mockito.when(portfolio.getCzkInvested()).thenReturn(p.getMaximumInvestmentSizeInCzk() - 1);
        final Stream<RecommendedLoan> result =
                s.recommend(Collections.singletonList(new LoanDescriptor(new Loan(1, 2))), portfolio);
        Assertions.assertThat(result).isEmpty();
    }

    @Test
    public void nothingRecommendedDueToRatingOverinvested() {
        final ParsedStrategy p = new ParsedStrategy(DefaultPortfolio.EMPTY);
        final InvestmentStrategy s = new NaturalLanguageInvestmentStrategy(p);
        final PortfolioOverview portfolio = Mockito.mock(PortfolioOverview.class);
        Mockito.when(portfolio.getCzkAvailable()).thenReturn(p.getMinimumBalance());
        Mockito.when(portfolio.getCzkInvested()).thenReturn(p.getMaximumInvestmentSizeInCzk() - 1);
        Mockito.when(portfolio.getShareOnInvestment(ArgumentMatchers.any())).thenReturn(BigDecimal.ZERO);
        final Loan l = Mockito.spy(new Loan(1, 2));
        Mockito.doReturn(Rating.A).when(l).getRating();
        final Stream<RecommendedLoan> result =
                s.recommend(Collections.singletonList(new LoanDescriptor(l)), portfolio);
        Assertions.assertThat(result).isEmpty();
    }

    @Test
    public void recommendationIsMade() {
        final ParsedStrategy p = new ParsedStrategy(DefaultPortfolio.PROGRESSIVE);
        final InvestmentStrategy s = new NaturalLanguageInvestmentStrategy(p);
        final PortfolioOverview portfolio = Mockito.mock(PortfolioOverview.class);
        Mockito.when(portfolio.getCzkAvailable()).thenReturn(p.getMinimumBalance());
        Mockito.when(portfolio.getCzkInvested()).thenReturn(p.getMaximumInvestmentSizeInCzk() - 1);
        Mockito.when(portfolio.getShareOnInvestment(ArgumentMatchers.any())).thenReturn(BigDecimal.ZERO);
        final Loan l = Mockito.spy(new Loan(1, 100000));
        Mockito.doReturn(Rating.A).when(l).getRating();
        final Loan l2 = Mockito.spy(new Loan(1, 100)); // will not be recommended due to low amount
        Mockito.doReturn(Rating.A).when(l2).getRating();
        final LoanDescriptor ld = new LoanDescriptor(l);
        final List<RecommendedLoan> result =
                s.recommend(Arrays.asList(new LoanDescriptor(l2), ld), portfolio).collect(Collectors.toList());
        Assertions.assertThat(result).hasSize(1);
        final RecommendedLoan r = result.get(0);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(r.descriptor()).isEqualTo(ld);
            softly.assertThat(r.amount()).isEqualTo(BigDecimal.valueOf(Defaults.MINIMUM_INVESTMENT_IN_CZK));
            softly.assertThat(r.isConfirmationRequired()).isFalse();
        });
    }

    @Test
    public void properlyRecommendingInvestmentSize() {
        final BigDecimal loanAmount = BigDecimal.valueOf(100000.0);
        final Loan mockLoan = Mockito.mock(Loan.class);
        Mockito.when(mockLoan.getRating()).thenReturn(Rating.A);
        Mockito.when(mockLoan.getRemainingInvestment()).thenReturn(loanAmount.doubleValue());
        Mockito.when(mockLoan.getAmount()).thenReturn(loanAmount.doubleValue());

        final ParsedStrategy s = new ParsedStrategy(DefaultPortfolio.PROGRESSIVE);
        final NaturalLanguageInvestmentStrategy overallStrategy = new NaturalLanguageInvestmentStrategy(s);
        final int actualInvestment = overallStrategy.recommendInvestmentAmount(mockLoan, Integer.MAX_VALUE);
        Assertions.assertThat(actualInvestment).isLessThanOrEqualTo(s.getMinimumInvestmentSizeInCzk(Rating.A));

        // with balance just a little less than the recommended investment
        final int adjustedForBalance = overallStrategy.recommendInvestmentAmount(mockLoan, actualInvestment - 1);
        Assertions.assertThat(adjustedForBalance).isLessThanOrEqualTo(actualInvestment);

        // and make sure the recommendation is 200 times X, where X is a positive integer
        final BigDecimal remainder = BigDecimal.valueOf(adjustedForBalance)
                .remainder(BigDecimal.valueOf(Defaults.MINIMUM_INVESTMENT_IN_CZK));
        Assertions.assertThat(remainder.intValue()).isEqualTo(0);
    }

}
