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
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Stream;

import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.strategies.InvestmentDescriptor;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.api.strategies.RecommendedInvestment;
import com.github.robozonky.api.strategies.SellStrategy;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NaturalLanguageSellStrategyTest {

    private InvestmentDescriptor mockDescriptor() {
        return mockDescriptor(mockInvestment());
    }

    private InvestmentDescriptor mockDescriptor(final Investment investment) {
        final Loan l = Loan.custom()
                .setId(1)
                .setAmount(100_000)
                .build();
        return new InvestmentDescriptor(investment, () -> l);
    }

    private final Investment mockInvestment() {
        return Investment.custom()
                .setRemainingPrincipal(BigDecimal.TEN)
                .build();
    }

    @Test
    void noLoansApplicable() {
        final ParsedStrategy p = spy(new ParsedStrategy(DefaultPortfolio.PROGRESSIVE));
        doReturn(Stream.empty()).when(p).getApplicableInvestments(any());
        final SellStrategy s = new NaturalLanguageSellStrategy(p);
        final PortfolioOverview portfolio = mock(PortfolioOverview.class);
        final Stream<RecommendedInvestment> result =
                s.recommend(Collections.singletonList(mockDescriptor()), portfolio);
        assertThat(result).isEmpty();
    }

    @Test
    void someLoansApplicable() {
        final ParsedStrategy p = spy(new ParsedStrategy(DefaultPortfolio.PROGRESSIVE));
        doAnswer(e -> {
            final Collection<InvestmentDescriptor> i = e.getArgument(0);
            return i.stream();
        }).when(p).getApplicableInvestments(any());
        final SellStrategy s = new NaturalLanguageSellStrategy(p);
        final PortfolioOverview portfolio = mock(PortfolioOverview.class);
        final Stream<RecommendedInvestment> result =
                s.recommend(Collections.singletonList(mockDescriptor()), portfolio);
        assertThat(result).hasSize(1);
    }
}
