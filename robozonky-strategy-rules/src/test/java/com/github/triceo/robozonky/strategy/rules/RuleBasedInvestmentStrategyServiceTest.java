/*
 * Copyright 2016 Lukáš Petrovický
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

package com.github.triceo.robozonky.strategy.rules;

import java.io.File;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import com.github.triceo.robozonky.PortfolioOverview;
import com.github.triceo.robozonky.remote.Loan;
import com.github.triceo.robozonky.remote.Rating;
import com.github.triceo.robozonky.strategy.InvestmentStrategy;
import com.github.triceo.robozonky.strategy.InvestmentStrategyParseException;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.Mockito;

public class RuleBasedInvestmentStrategyServiceTest {

    private static final File FILE =
            new File("src/test/resources/com/github/triceo/robozonky/strategy/rules/ExampleStrategy.xlsx");

    @Test
    public void simple() throws InvestmentStrategyParseException {
        final RuleBasedInvestmentStrategyService s = new RuleBasedInvestmentStrategyService();
        Assertions.assertThat(s.isSupported(RuleBasedInvestmentStrategyServiceTest.FILE)).isTrue();
        final InvestmentStrategy is = s.parse(RuleBasedInvestmentStrategyServiceTest.FILE);
        Assertions.assertThat(is).isNotNull();
        // let's make up some loans; B will not be accepted, D will be prioritized over A
        final Loan aaaaa = Mockito.mock(Loan.class); // will not be accepted since AAAAA are ignored
        Mockito.when(aaaaa.getId()).thenReturn(1);
        Mockito.when(aaaaa.getRating()).thenReturn(Rating.AAAAA);
        final Loan b = Mockito.mock(Loan.class); // will not be accepted, asking for too much money
        Mockito.when(b.getId()).thenReturn(2);
        Mockito.when(b.getRating()).thenReturn(Rating.B);
        Mockito.when(b.getAmount()).thenReturn(300000.0);
        final Loan aa = Mockito.mock(Loan.class); // will not be accepted, asking for too long a term
        Mockito.when(aa.getId()).thenReturn(3);
        Mockito.when(aa.getRating()).thenReturn(Rating.AA);
        Mockito.when(aa.getTermInMonths()).thenReturn(50);
        final Loan aaa = Mockito.mock(Loan.class); // will not be accepted, we have too many
        Mockito.when(aaa.getId()).thenReturn(4);
        Mockito.when(aaa.getRating()).thenReturn(Rating.AAA);
        final Loan aaaa = Mockito.mock(Loan.class); // will be accepted
        Mockito.when(aaaa.getId()).thenReturn(5);
        Mockito.when(aaaa.getRating()).thenReturn(Rating.AAAA);
        Mockito.when(aaaa.getTermInMonths()).thenReturn(30);
        final Loan d = Mockito.mock(Loan.class); // will be accepted and prioritized over AAAA
        Mockito.when(d.getId()).thenReturn(6);
        Mockito.when(d.getAmount()).thenReturn(50000.0);
        Mockito.when(d.getRating()).thenReturn(Rating.D);
        // prepare portfolio
        final PortfolioOverview portfolio = Mockito.mock(PortfolioOverview.class);
        Mockito.when(portfolio.getCzkAvailable()).thenReturn(BigDecimal.valueOf(10000));
        Mockito.when(portfolio.getCzkInvested()).thenReturn(BigDecimal.valueOf(100000));
        Arrays.stream(Rating.values())
                .forEach(r -> Mockito.when(portfolio.getShareOnInvestment(r)).thenReturn(BigDecimal.ZERO));
        Mockito.when(portfolio.getShareOnInvestment(Rating.AAA)).thenReturn(BigDecimal.ONE);
        // check investing logic
        final List<Loan> loans = Arrays.asList(aaaaa, aaaa, aaa, aa, b, d);
        final List<Loan> result = is.getMatchingLoans(loans, portfolio);
        Assertions.assertThat(result).containsExactly(d, aaaa);
        Assertions.assertThat(is.recommendInvestmentAmount(d, portfolio)).isEqualTo(200);
        Assertions.assertThat(is.recommendInvestmentAmount(aaaa, portfolio)).isEqualTo(400);
        Assertions.assertThat(is.recommendInvestmentAmount(aa, portfolio)).isZero();
    }

}
