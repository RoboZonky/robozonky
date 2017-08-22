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

import java.util.Collections;

import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.LoanDescriptor;
import com.github.robozonky.internal.api.Defaults;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public class ParsedStrategyTest {

    @Test
    public void construct() {
        final DefaultPortfolio portfolio = DefaultPortfolio.PROGRESSIVE;
        final ParsedStrategy strategy = new ParsedStrategy(portfolio); // test for default values
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(strategy.getMinimumBalance()).isEqualTo(Defaults.MINIMUM_INVESTMENT_IN_CZK);
            softly.assertThat(strategy.getMaximumInvestmentSizeInCzk()).isEqualTo(Integer.MAX_VALUE);
            softly.assertThat(strategy.getMinimumShare(Rating.A))
                    .isEqualTo(portfolio.getDefaultShare(Rating.A));
            softly.assertThat(strategy.getMaximumShare(Rating.B))
                    .isEqualTo(portfolio.getDefaultShare(Rating.B));
            softly.assertThat(strategy.getMinimumInvestmentSizeInCzk(Rating.C)).isEqualTo(0);
            softly.assertThat(strategy.getMaximumInvestmentSizeInCzk(Rating.D)).isEqualTo(
                    Defaults.MINIMUM_INVESTMENT_IN_CZK);
            softly.assertThat(strategy.needsConfirmation(new LoanDescriptor(new Loan(1, 2)))).isFalse();
        });
    }

    @Test
    public void conditions() {
        final DefaultPortfolio portfolio = DefaultPortfolio.PROGRESSIVE;
        final ParsedStrategy strategy = new ParsedStrategy(portfolio); // test for default values
        Assertions.assertThat(strategy.getApplicableLoans(Collections.emptyList())).isEmpty();
        // add loan; without filters, should be applicable
        final Loan loan = new Loan(1, 2);
        final LoanDescriptor ld = new LoanDescriptor(loan);
        Assertions.assertThat(strategy.getApplicableLoans(Collections.singletonList(ld))).contains(ld);
        // now add a filter and see no loans applicable
        final MarketplaceFilter f = Mockito.mock(MarketplaceFilter.class);
        Mockito.when(f.test(ArgumentMatchers.eq(new Wrapper(loan)))).thenReturn(true);
        final ParsedStrategy strategy2 =
                new ParsedStrategy(portfolio, Collections.singletonMap(Boolean.TRUE, Collections.singleton(f)));
        Assertions.assertThat(strategy2.getApplicableLoans(Collections.singletonList(ld))).isEmpty();
    }

    @Test
    public void shares() {
        final DefaultPortfolio portfolio = DefaultPortfolio.EMPTY;
        final DefaultValues values = new DefaultValues(portfolio);
        final PortfolioShare share = new PortfolioShare(Rating.D, 50, 100);
        final ParsedStrategy strategy = new ParsedStrategy(values, Collections.singleton(share),
                                                           Collections.emptyList(), Collections.emptyMap(),
                                                           Collections.emptyList());
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(strategy.getMinimumShare(Rating.D)).isEqualTo(50);
            softly.assertThat(strategy.getMaximumShare(Rating.D)).isEqualTo(100);
        });
    }

    @Test
    public void investmentSizes() {
        final DefaultPortfolio portfolio = DefaultPortfolio.EMPTY;
        final DefaultValues values = new DefaultValues(portfolio);
        final InvestmentSize size = new InvestmentSize(Rating.D, 600, 1000);
        final ParsedStrategy strategy = new ParsedStrategy(values, Collections.emptyList(),
                                                           Collections.singleton(size), Collections.emptyMap(),
                                                           Collections.emptyList());
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(strategy.getMinimumInvestmentSizeInCzk(Rating.D)).isEqualTo(600);
            softly.assertThat(strategy.getMaximumInvestmentSizeInCzk(Rating.D)).isEqualTo(1000);
        });
    }
}
