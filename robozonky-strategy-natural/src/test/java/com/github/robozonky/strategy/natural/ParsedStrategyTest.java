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

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import com.github.robozonky.api.remote.entities.Participation;
import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.InvestmentDescriptor;
import com.github.robozonky.api.strategies.LoanDescriptor;
import com.github.robozonky.api.strategies.ParticipationDescriptor;
import com.github.robozonky.strategy.natural.conditions.MarketplaceFilter;
import com.github.robozonky.strategy.natural.conditions.MarketplaceFilterCondition;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.*;

class ParsedStrategyTest {

    private static Loan mockLoan(final int amount) {
        return Loan.custom()
                .setId(1)
                .setRating(Rating.A)
                .setAmount(amount)
                .build();
    }

    private static ParticipationDescriptor mockParticipationDescriptor(final Loan loan) {
        final Participation p = mock(Participation.class);
        doReturn(loan.getTermInMonths()).when(p).getRemainingInstalmentCount();
        return new ParticipationDescriptor(p, () -> loan);
    }

    @Test
    void construct() {
        final DefaultPortfolio portfolio = DefaultPortfolio.PROGRESSIVE;
        final ParsedStrategy strategy = new ParsedStrategy(portfolio); // test for default values
        assertSoftly(softly -> {
            softly.assertThat(strategy.getMinimumBalance()).isEqualTo(0);
            softly.assertThat(strategy.getMaximumInvestmentSizeInCzk()).isEqualTo(Long.MAX_VALUE);
            softly.assertThat(strategy.getMinimumShare(Rating.A))
                    .isEqualTo(portfolio.getDefaultShare(Rating.A));
            softly.assertThat(strategy.getMaximumShare(Rating.B))
                    .isEqualTo(portfolio.getDefaultShare(Rating.B));
            softly.assertThat(strategy.getMinimumInvestmentSizeInCzk(Rating.C)).isEqualTo(0);
            softly.assertThat(strategy.getMaximumInvestmentSizeInCzk(Rating.D)).isEqualTo(20_000);
            softly.assertThat(
                    strategy.needsConfirmation(new LoanDescriptor(ParsedStrategyTest.mockLoan(2)))).isFalse();
        });
    }

    @Test
    void sellOffStarted() {
        final DefaultPortfolio portfolio = DefaultPortfolio.EMPTY;
        final DefaultValues values = new DefaultValues(portfolio);
        // activate default sell-off 3 months before the given date, which is already in the past
        values.setExitProperties(new ExitProperties(LocalDate.now().plusMonths(2)));
        final ParsedStrategy strategy = new ParsedStrategy(values, Collections.emptyList());
        // no loan or participation should be bought; every investment should be sold
        final Loan l = ParsedStrategyTest.mockLoan(1000);
        final LoanDescriptor ld = new LoanDescriptor(l);
        final ParticipationDescriptor pd = ParsedStrategyTest.mockParticipationDescriptor(l);
        final Investment i = Investment.fresh(l, 200);
        final InvestmentDescriptor id = new InvestmentDescriptor(i, () -> l);
        assertSoftly(softly -> {
            softly.assertThat(strategy.getApplicableLoans(Collections.singleton(ld))).isEmpty();
            softly.assertThat(strategy.getApplicableParticipations(Collections.singleton(pd))).isEmpty();
            softly.assertThat(strategy.getApplicableInvestments(Collections.singleton(id))).containsOnly(id);
        });
    }

    @Test
    void exitButNoSelloff() {
        final DefaultPortfolio portfolio = DefaultPortfolio.EMPTY;
        final DefaultValues values = new DefaultValues(portfolio);
        values.setExitProperties(new ExitProperties(LocalDate.now().plusMonths(6))); // exit active, no sell-off yet
        final ParsedStrategy strategy = new ParsedStrategy(values, Collections.emptyList(), Collections.emptyMap(),
                                                           new FilterSupplier(values, Collections.emptySet(),
                                                                              Collections.emptySet(),
                                                                              Collections.emptySet()));
        // no loan or participation should be bought; every investment should be sold
        final Loan loanUnder = ParsedStrategyTest.mockLoan(1000);
        final Loan loanOver = Loan.custom()
                .setId(2)
                .setAmount(2000)
                .setTermInMonths(84)
                .build();
        final LoanDescriptor ldOver = new LoanDescriptor(loanOver);
        final LoanDescriptor ldUnder = new LoanDescriptor(loanUnder);
        final ParticipationDescriptor pdOver = ParsedStrategyTest.mockParticipationDescriptor(loanOver);
        final ParticipationDescriptor pdUnder = ParsedStrategyTest.mockParticipationDescriptor(loanUnder);
        final Investment iUnder = Investment.fresh(loanUnder, 200);
        final InvestmentDescriptor idUnder = new InvestmentDescriptor(iUnder, () -> loanUnder);
        final Investment iOver = Investment.fresh(loanOver, 200);
        final InvestmentDescriptor idOver = new InvestmentDescriptor(iOver, () -> loanOver);
        assertSoftly(softly -> {
            softly.assertThat(strategy.getApplicableLoans(Arrays.asList(ldOver, ldUnder))).containsOnly(ldUnder);
            softly.assertThat(strategy.getApplicableParticipations(Arrays.asList(pdOver, pdUnder)))
                    .containsOnly(pdUnder);
            softly.assertThat(strategy.getApplicableInvestments(Arrays.asList(idOver, idUnder))).isEmpty();
        });
    }

    @Test
    void conditions() {
        final DefaultPortfolio portfolio = DefaultPortfolio.PROGRESSIVE;
        final ParsedStrategy strategy = new ParsedStrategy(portfolio); // test for default values
        assertThat(strategy.getApplicableLoans(Collections.emptyList())).isEmpty();
        // add loan; without filters, should be applicable
        final Loan loan = ParsedStrategyTest.mockLoan(2);
        final LoanDescriptor ld = new LoanDescriptor(loan);
        assertThat(strategy.getApplicableLoans(Collections.singletonList(ld))).contains(ld);
        // now add a filter and see no loans applicable
        final MarketplaceFilter f = mock(MarketplaceFilter.class);
        when(f.test(eq(Wrapper.wrap(ld)))).thenReturn(true);
        final ParsedStrategy strategy2 = new ParsedStrategy(portfolio, Collections.singleton(f));
        assertThat(strategy2.getApplicableLoans(Collections.singletonList(ld))).isEmpty();
    }

    @Test
    void shares() {
        final DefaultPortfolio portfolio = DefaultPortfolio.EMPTY;
        final DefaultValues values = new DefaultValues(portfolio);
        final PortfolioShare share = new PortfolioShare(Rating.D, 50, 100);
        final ParsedStrategy strategy = new ParsedStrategy(values, Collections.singleton(share),
                                                           Collections.emptyMap());
        assertSoftly(softly -> {
            softly.assertThat(strategy.getMinimumShare(Rating.D)).isEqualTo(50);
            softly.assertThat(strategy.getMaximumShare(Rating.D)).isEqualTo(100);
        });
    }

    @Test
    void investmentSizes() {
        final DefaultPortfolio portfolio = DefaultPortfolio.EMPTY;
        final DefaultValues values = new DefaultValues(portfolio);
        final InvestmentSize size = new InvestmentSize(600, 1000);
        final ParsedStrategy strategy = new ParsedStrategy(values, Collections.emptyList(),
                                                           Collections.singletonMap(Rating.D, size));
        assertSoftly(softly -> {
            softly.assertThat(strategy.getMinimumInvestmentSizeInCzk(Rating.D)).isEqualTo(600);
            softly.assertThat(strategy.getMaximumInvestmentSizeInCzk(Rating.D)).isEqualTo(1000);
        });
    }

    @Test
    void matchesAlsoSellFilter() {
        final MarketplaceFilter accepting = MarketplaceFilter.of(MarketplaceFilterCondition.alwaysAccepting());
        final Collection<MarketplaceFilter> filters = Collections.singleton(accepting);
        final DefaultValues v = new DefaultValues(DefaultPortfolio.PROGRESSIVE);
        final FilterSupplier s = new FilterSupplier(v, Collections.emptySet(), Collections.emptySet(), filters);
        final ParsedStrategy ps = new ParsedStrategy(v, Collections.emptyList(), Collections.emptyMap(), s);
        final Loan l = ParsedStrategyTest.mockLoan(200_000);
        final LoanDescriptor ld = new LoanDescriptor(l);
        assertThat(ps.getApplicableLoans(Collections.singleton(ld))).isEmpty();
        final Participation p = mock(Participation.class);
        final ParticipationDescriptor pd = new ParticipationDescriptor(p, () -> l);
        assertThat(ps.getApplicableParticipations(Collections.singleton(pd))).isEmpty();
    }
}
