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

package com.github.triceo.robozonky.strategy.natural;

import java.util.Collections;
import java.util.stream.Stream;

import com.github.triceo.robozonky.api.remote.entities.Investment;
import com.github.triceo.robozonky.api.remote.entities.Loan;
import com.github.triceo.robozonky.api.strategies.InvestmentDescriptor;
import com.github.triceo.robozonky.api.strategies.PortfolioOverview;
import com.github.triceo.robozonky.api.strategies.RecommendedInvestment;
import com.github.triceo.robozonky.api.strategies.SellStrategy;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.Mockito;

public class NaturalLanguageSellStrategyTest {

    private final InvestmentDescriptor mockDescriptor() {
        return mockDescriptor(mock());
    }

    private final InvestmentDescriptor mockDescriptor(final Investment investment) {
        final Loan l = new Loan(1, 2);
        return new InvestmentDescriptor(investment, l);
    }

    private final Investment mock() {
        return Mockito.mock(Investment.class);
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
        final SellStrategy s = new NaturalLanguageSellStrategy(p);
        final PortfolioOverview portfolio = Mockito.mock(PortfolioOverview.class);
        Mockito.when(portfolio.getCzkAvailable()).thenReturn(p.getMinimumBalance());
        Mockito.when(portfolio.getCzkInvested()).thenReturn(p.getMaximumInvestmentSizeInCzk() - 1);
        final Stream<RecommendedInvestment> result =
                s.recommend(Collections.singletonList(mockDescriptor()), portfolio);
        Assertions.assertThat(result).isEmpty();
    }
}
