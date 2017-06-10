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

package com.github.triceo.robozonky.strategy.rules;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.triceo.robozonky.api.remote.entities.Loan;
import com.github.triceo.robozonky.api.remote.enums.Rating;
import com.github.triceo.robozonky.api.strategies.InvestmentStrategy;
import com.github.triceo.robozonky.api.strategies.LoanDescriptor;
import com.github.triceo.robozonky.api.strategies.PortfolioOverview;
import com.github.triceo.robozonky.api.strategies.Recommendation;
import com.github.triceo.robozonky.internal.api.Defaults;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.Mockito;

public class RuleBasedInvestmentStrategyServiceTest {

    private static final InputStream FILE =
            RuleBasedInvestmentStrategyServiceTest.class.getResourceAsStream("ExampleStrategy.xlsx");

    @Test
    public void simple() throws IOException {
        final RuleBasedInvestmentStrategyService s = new RuleBasedInvestmentStrategyService();
        final Optional<InvestmentStrategy> ois = s.parse(RuleBasedInvestmentStrategyServiceTest.FILE);
        Assertions.assertThat(ois).isPresent();
        final InvestmentStrategy is = ois.get();
        // let's make up some marketplace; B will not be accepted, D will be prioritized over A
        final Loan aaaaa = Mockito.mock(Loan.class); // will not be accepted since AAAAA are ignored
        Mockito.when(aaaaa.getId()).thenReturn(1);
        Mockito.when(aaaaa.getRemainingInvestment()).thenReturn(100000.0);
        Mockito.when(aaaaa.getDatePublished()).thenReturn(OffsetDateTime.ofInstant(Instant.EPOCH, Defaults.ZONE_ID));
        Mockito.when(aaaaa.getRating()).thenReturn(Rating.AAAAA);
        final Loan b = Mockito.mock(Loan.class); // will not be accepted, asking for too much money
        Mockito.when(b.getId()).thenReturn(2);
        Mockito.when(b.getRemainingInvestment()).thenReturn(100000.0);
        Mockito.when(b.getDatePublished()).thenReturn(OffsetDateTime.ofInstant(Instant.EPOCH, Defaults.ZONE_ID));
        Mockito.when(b.getRating()).thenReturn(Rating.B);
        Mockito.when(b.getAmount()).thenReturn(300000.0);
        final Loan aa = Mockito.mock(Loan.class); // will not be accepted, asking for too long a term
        Mockito.when(aa.getId()).thenReturn(3);
        Mockito.when(aa.getRemainingInvestment()).thenReturn(100000.0);
        Mockito.when(aa.getDatePublished()).thenReturn(OffsetDateTime.ofInstant(Instant.EPOCH, Defaults.ZONE_ID));
        Mockito.when(aa.getRating()).thenReturn(Rating.AA);
        Mockito.when(aa.getTermInMonths()).thenReturn(50);
        final Loan aaa = Mockito.mock(Loan.class); // will not be accepted, we have too many
        Mockito.when(aaa.getId()).thenReturn(4);
        Mockito.when(aaa.getRemainingInvestment()).thenReturn(100000.0);
        Mockito.when(aaa.getDatePublished()).thenReturn(OffsetDateTime.ofInstant(Instant.EPOCH, Defaults.ZONE_ID));
        Mockito.when(aaa.getRating()).thenReturn(Rating.AAA);
        final Loan aaaa = Mockito.mock(Loan.class); // will be accepted
        Mockito.when(aaaa.getId()).thenReturn(5);
        Mockito.when(aaaa.getRemainingInvestment()).thenReturn(100000.0);
        Mockito.when(aaaa.getRating()).thenReturn(Rating.AAAA);
        Mockito.when(aaaa.getDatePublished()).thenReturn(OffsetDateTime.ofInstant(Instant.EPOCH, Defaults.ZONE_ID));
        Mockito.when(aaaa.getTermInMonths()).thenReturn(30);
        final Loan c = Mockito.mock(Loan.class); // will be rejected by the filter
        Mockito.when(c.getId()).thenReturn(6);
        Mockito.when(c.getRemainingInvestment()).thenReturn(100000.0);
        Mockito.when(c.getRating()).thenReturn(Rating.C);
        Mockito.when(c.getDatePublished()).thenReturn(OffsetDateTime.ofInstant(Instant.EPOCH, Defaults.ZONE_ID));
        Mockito.when(c.getTermInMonths()).thenReturn(30);
        final Loan d = Mockito.mock(Loan.class); // will be accepted and prioritized over AAAA
        Mockito.when(d.getId()).thenReturn(7);
        Mockito.when(d.getRemainingInvestment()).thenReturn(100000.0);
        Mockito.when(d.getDatePublished()).thenReturn(OffsetDateTime.ofInstant(Instant.EPOCH, Defaults.ZONE_ID));
        Mockito.when(d.getAmount()).thenReturn(50000.0);
        Mockito.when(d.getRating()).thenReturn(Rating.D);
        // prepare portfolio
        final PortfolioOverview portfolio = Mockito.mock(PortfolioOverview.class);
        Mockito.when(portfolio.getCzkAvailable()).thenReturn(10000);
        Mockito.when(portfolio.getCzkInvested()).thenReturn(100000);
        Arrays.stream(Rating.values())
                .forEach(r -> Mockito.when(portfolio.getShareOnInvestment(r)).thenReturn(BigDecimal.ZERO));
        Mockito.when(portfolio.getShareOnInvestment(Rating.AAA)).thenReturn(BigDecimal.ONE);
        // check investing logic
        final Map<Loan, LoanDescriptor> loans = Stream.of(aaaaa, aaaa, aaa, aa, b, c, d)
                .collect(Collectors.toMap(Function.identity(), l -> new LoanDescriptor(l, Duration.ofSeconds(100))));
        final List<Recommendation> result = is.recommend(loans.values(), portfolio);
        Assertions.assertThat(result).containsOnly(loans.get(d).recommend(200).get(),
                loans.get(aaaa).recommend(400).get());
    }

}
