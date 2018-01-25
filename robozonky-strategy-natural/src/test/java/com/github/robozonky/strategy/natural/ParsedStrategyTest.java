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

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;

import com.github.robozonky.api.remote.entities.Investment;
import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.api.remote.entities.Participation;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.InvestmentDescriptor;
import com.github.robozonky.api.strategies.LoanDescriptor;
import com.github.robozonky.api.strategies.ParticipationDescriptor;
import com.github.robozonky.internal.api.Defaults;
import com.github.robozonky.strategy.natural.conditions.MarketplaceFilter;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

class ParsedStrategyTest {

    private static ParticipationDescriptor mock(final Loan loan) {
        final Participation p = Mockito.mock(Participation.class);
        Mockito.doReturn(loan.getTermInMonths()).when(p).getRemainingInstalmentCount();
        return new ParticipationDescriptor(p, loan);
    }

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
                    Defaults.MAXIMUM_INVESTMENT_IN_CZK);
            softly.assertThat(strategy.needsConfirmation(new LoanDescriptor(new Loan(1, 2)))).isFalse();
        });
    }

    @Test
    public void sellOffStarted() {
        final DefaultPortfolio portfolio = DefaultPortfolio.EMPTY;
        final DefaultValues values = new DefaultValues(portfolio);
        // activate default sell-off 3 months before the given date, which is already in the past
        values.setExitProperties(new ExitProperties(LocalDate.now().plusMonths(2)));
        final ParsedStrategy strategy = new ParsedStrategy(values, Collections.emptyList());
        // no loan or participation should be bought; every investment should be sold
        final Loan l = new Loan(1, 1000);
        final LoanDescriptor ld = new LoanDescriptor(l);
        final ParticipationDescriptor pd = mock(l);
        final Investment i = new Investment(l, 200);
        final InvestmentDescriptor id = new InvestmentDescriptor(i, l);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(strategy.getApplicableLoans(Collections.singleton(ld))).isEmpty();
            softly.assertThat(strategy.getApplicableParticipations(Collections.singleton(pd))).isEmpty();
            softly.assertThat(strategy.getApplicableInvestments(Collections.singleton(id))).containsOnly(id);
        });
    }

    @Test
    public void exitButNoSelloff() {
        final DefaultPortfolio portfolio = DefaultPortfolio.EMPTY;
        final DefaultValues values = new DefaultValues(portfolio);
        values.setExitProperties(new ExitProperties(LocalDate.now().plusMonths(6))); // exit active, no sell-off yet
        final ParsedStrategy strategy = new ParsedStrategy(values, Collections.emptyList(), Collections.emptyMap(),
                                                           new FilterSupplier(values, Collections.emptySet(),
                                                                              Collections.emptySet(),
                                                                              Collections.emptySet()));
        // no loan or participation should be bought; every investment should be sold
        final Loan loanUnder = new Loan(1, 1000);
        final Loan loanOver = Mockito.spy(new Loan(2, 1000));
        Mockito.when(loanOver.getTermInMonths()).thenReturn(84);
        final LoanDescriptor ldOver = new LoanDescriptor(loanOver);
        final LoanDescriptor ldUnder = new LoanDescriptor(loanUnder);
        final ParticipationDescriptor pdOver = mock(loanOver);
        final ParticipationDescriptor pdUnder = mock(loanUnder);
        final Investment iUnder = new Investment(loanUnder, 200);
        final InvestmentDescriptor idUnder = new InvestmentDescriptor(iUnder, loanUnder);
        final Investment iOver = new Investment(loanOver, 200);
        final InvestmentDescriptor idOver = new InvestmentDescriptor(iOver, loanOver);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(strategy.getApplicableLoans(Arrays.asList(ldOver, ldUnder))).containsOnly(ldUnder);
            softly.assertThat(strategy.getApplicableParticipations(Arrays.asList(pdOver, pdUnder)))
                    .containsOnly(pdUnder);
            softly.assertThat(strategy.getApplicableInvestments(Arrays.asList(idOver, idUnder))).isEmpty();
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
        final ParsedStrategy strategy2 = new ParsedStrategy(portfolio, Collections.singleton(f));
        Assertions.assertThat(strategy2.getApplicableLoans(Collections.singletonList(ld))).isEmpty();
    }

    @Test
    public void shares() {
        final DefaultPortfolio portfolio = DefaultPortfolio.EMPTY;
        final DefaultValues values = new DefaultValues(portfolio);
        final PortfolioShare share = new PortfolioShare(Rating.D, 50, 100);
        final ParsedStrategy strategy = new ParsedStrategy(values, Collections.singleton(share),
                                                           Collections.emptyMap());
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(strategy.getMinimumShare(Rating.D)).isEqualTo(50);
            softly.assertThat(strategy.getMaximumShare(Rating.D)).isEqualTo(100);
        });
    }

    @Test
    public void investmentSizes() {
        final DefaultPortfolio portfolio = DefaultPortfolio.EMPTY;
        final DefaultValues values = new DefaultValues(portfolio);
        final InvestmentSize size = new InvestmentSize(600, 1000);
        final ParsedStrategy strategy = new ParsedStrategy(values, Collections.emptyList(),
                                                           Collections.singletonMap(Rating.D, size));
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(strategy.getMinimumInvestmentSizeInCzk(Rating.D)).isEqualTo(600);
            softly.assertThat(strategy.getMaximumInvestmentSizeInCzk(Rating.D)).isEqualTo(1000);
        });
    }
}
