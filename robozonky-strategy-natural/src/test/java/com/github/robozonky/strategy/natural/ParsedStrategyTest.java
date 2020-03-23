/*
 * Copyright 2020 The RoboZonky Project
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

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.Ratio;
import com.github.robozonky.api.remote.entities.Investment;
import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.api.remote.entities.Participation;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.InvestmentDescriptor;
import com.github.robozonky.api.strategies.LoanDescriptor;
import com.github.robozonky.api.strategies.ParticipationDescriptor;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.strategy.natural.conditions.MarketplaceFilter;
import com.github.robozonky.strategy.natural.conditions.MarketplaceFilterCondition;
import com.github.robozonky.test.mock.MockInvestmentBuilder;
import com.github.robozonky.test.mock.MockLoanBuilder;

class ParsedStrategyTest {

    private static final PortfolioOverview FOLIO = mock(PortfolioOverview.class);

    private static Loan mockLoan(final int amount) {
        return new MockLoanBuilder()
            .setRating(Rating.A)
            .setAmount(amount)
            .build();
    }

    private static ParticipationDescriptor mockParticipationDescriptor(final Loan loan) {
        final Participation p = mock(Participation.class);
        doReturn(loan.getTermInMonths()).when(p)
            .getRemainingInstalmentCount();
        return new ParticipationDescriptor(p, () -> loan);
    }

    @Test
    void construct() {
        final DefaultPortfolio portfolio = DefaultPortfolio.PROGRESSIVE;
        final ParsedStrategy strategy = new ParsedStrategy(portfolio); // test for default values
        assertThat(strategy.getMinimumVersion()).isEmpty();
        strategy.setMinimumVersion(new RoboZonkyVersion(1, 2, 3));
        assertSoftly(softly -> {
            softly.assertThat(strategy.isInvestingEnabled())
                .isTrue();
            softly.assertThat(strategy.isPurchasingEnabled())
                .isFalse();
            softly.assertThat(strategy.getMinimumVersion())
                .isNotEmpty();
            softly.assertThat(strategy.getMaximumInvestmentSize())
                .isEqualTo(Money.from(Long.MAX_VALUE));
            softly.assertThat(strategy.getPermittedShare(Rating.B))
                .isEqualTo(portfolio.getDefaultShare(Rating.B));
            softly.assertThat(strategy.getMinimumInvestmentSize(Rating.C))
                .isEqualTo(Money.from(0));
            softly.assertThat(strategy.getMaximumInvestmentSize(Rating.D))
                .isEqualTo(Money.from(20_000));
        });
    }

    @Test
    void sellOffStarted() {
        final DefaultPortfolio portfolio = DefaultPortfolio.EMPTY;
        final DefaultValues values = new DefaultValues(portfolio);
        // activate default sell-off 3 months before the given date, which is already in the past
        values.setExitProperties(new ExitProperties(LocalDate.now()
            .plusMonths(2)));
        final ParsedStrategy strategy = new ParsedStrategy(values, Collections.emptyList());
        // no loan or participation should be bought; every investment should be sold
        final Loan l = ParsedStrategyTest.mockLoan(1000);
        final LoanDescriptor ld = new LoanDescriptor(l);
        final ParticipationDescriptor pd = ParsedStrategyTest.mockParticipationDescriptor(l);
        final Investment i = MockInvestmentBuilder.fresh(l, 200)
            .build();
        final InvestmentDescriptor id = new InvestmentDescriptor(i, () -> l);
        assertSoftly(softly -> {
            softly.assertThat(strategy.isPurchasingEnabled())
                .isFalse();
            softly.assertThat(strategy.isInvestingEnabled())
                .isTrue();
            softly.assertThat(strategy.getApplicableLoans(Stream.of(ld), FOLIO))
                .isEmpty();
            softly.assertThat(strategy.getApplicableParticipations(Stream.of(pd), FOLIO))
                .isEmpty();
            softly.assertThat(strategy.getMatchingSellFilters(Stream.of(id), FOLIO))
                .containsOnly(id);
        });
    }

    @Test
    void exitButNoSelloff() {
        final DefaultPortfolio portfolio = DefaultPortfolio.EMPTY;
        final DefaultValues values = new DefaultValues(portfolio);
        values.setExitProperties(new ExitProperties(LocalDate.now()
            .plusMonths(6))); // exit active, no sell-off yet
        final ParsedStrategy strategy = new ParsedStrategy(values, Collections.emptyList(), Collections.emptyMap(),
                Collections.emptyMap(),
                new FilterSupplier(values, Collections.emptySet(),
                        Collections.emptySet(),
                        Collections.emptySet()));
        // no loan or participation should be bought; every investment should be sold
        final Loan loanUnder = ParsedStrategyTest.mockLoan(1000);
        final Loan loanOver = new MockLoanBuilder()
            .setAmount(2000)
            .setTermInMonths(84)
            .build();
        final LoanDescriptor ldOver = new LoanDescriptor(loanOver);
        final LoanDescriptor ldUnder = new LoanDescriptor(loanUnder);
        final ParticipationDescriptor pdOver = ParsedStrategyTest.mockParticipationDescriptor(loanOver);
        final ParticipationDescriptor pdUnder = ParsedStrategyTest.mockParticipationDescriptor(loanUnder);
        final Investment iUnder = MockInvestmentBuilder.fresh(loanUnder, 200)
            .build();
        final InvestmentDescriptor idUnder = new InvestmentDescriptor(iUnder, () -> loanUnder);
        final Investment iOver = MockInvestmentBuilder.fresh(loanOver, 200)
            .build();
        final InvestmentDescriptor idOver = new InvestmentDescriptor(iOver, () -> loanOver);
        assertSoftly(softly -> {
            softly.assertThat(strategy.isPurchasingEnabled())
                .isTrue();
            softly.assertThat(strategy.isInvestingEnabled())
                .isTrue();
            softly.assertThat(strategy.getApplicableLoans(Stream.of(ldOver, ldUnder), FOLIO))
                .containsOnly(ldUnder);
            softly.assertThat(strategy.getApplicableParticipations(Stream.of(pdOver, pdUnder), FOLIO))
                .containsOnly(pdUnder);
            softly.assertThat(strategy.getMatchingSellFilters(Stream.of(idOver, idUnder), FOLIO))
                .isEmpty();
        });
    }

    @Test
    void conditions() {
        final DefaultPortfolio portfolio = DefaultPortfolio.PROGRESSIVE;
        final ParsedStrategy strategy = new ParsedStrategy(portfolio); // test for default values
        assertThat(strategy.getApplicableLoans(Stream.empty(), FOLIO)).isEmpty();
        // add loan; without filters, should be applicable
        final Loan loan = ParsedStrategyTest.mockLoan(2);
        final LoanDescriptor ld = new LoanDescriptor(loan);
        assertThat(strategy.getApplicableLoans(Stream.of(ld), FOLIO)).contains(ld);
        // now add a filter and see no loans applicable
        final MarketplaceFilter f = mock(MarketplaceFilter.class);
        when(f.test(eq(Wrapper.wrap(ld, FOLIO)))).thenReturn(true);
        final ParsedStrategy strategy2 = new ParsedStrategy(portfolio, Collections.singleton(f));
        assertThat(strategy2.getApplicableLoans(Stream.of(ld), FOLIO)).isEmpty();
    }

    @Test
    void shares() {
        final DefaultPortfolio portfolio = DefaultPortfolio.EMPTY;
        final DefaultValues values = new DefaultValues(portfolio);
        final PortfolioShare share = new PortfolioShare(Rating.D, Ratio.fromPercentage(100));
        final ParsedStrategy strategy = new ParsedStrategy(values, Collections.singleton(share),
                Collections.emptyMap(), Collections.emptyMap());
        assertThat(strategy.getPermittedShare(Rating.D)).isEqualTo(Ratio.ONE);
    }

    @Test
    void investmentSizes() {
        final DefaultPortfolio portfolio = DefaultPortfolio.EMPTY;
        final DefaultValues values = new DefaultValues(portfolio);
        final MoneyRange size = new MoneyRange(600, 1000);
        final ParsedStrategy strategy = new ParsedStrategy(values, Collections.emptyList(),
                Collections.singletonMap(Rating.D, size),
                Collections.emptyMap());
        assertSoftly(softly -> {
            softly.assertThat(strategy.getMinimumInvestmentSize(Rating.D))
                .isEqualTo(Money.from(600));
            softly.assertThat(strategy.getMaximumInvestmentSize(Rating.D))
                .isEqualTo(Money.from(1_000));
        });
    }

    @Test
    void purchaseSizes() {
        final DefaultPortfolio portfolio = DefaultPortfolio.EMPTY;
        final DefaultValues values = new DefaultValues(portfolio);
        final MoneyRange size = new MoneyRange(1000);
        final ParsedStrategy strategy = new ParsedStrategy(values, Collections.emptyList(), Collections.emptyMap(),
                Collections.singletonMap(Rating.D, size));
        assertSoftly(softly -> {
            softly.assertThat(strategy.getMinimumPurchaseSize(Rating.D))
                .isEqualTo(Money.from(0));
            softly.assertThat(strategy.getMaximumPurchaseSize(Rating.D))
                .isEqualTo(Money.from(1_000));
        });
    }

    @Test
    void matchesAlsoSellFilter() {
        final MarketplaceFilter accepting = MarketplaceFilter.of(MarketplaceFilterCondition.alwaysAccepting());
        final Collection<MarketplaceFilter> filters = Collections.singleton(accepting);
        final DefaultValues v = new DefaultValues(DefaultPortfolio.PROGRESSIVE);
        final FilterSupplier s = new FilterSupplier(v, Collections.emptySet(), Collections.emptySet(), filters);
        final ParsedStrategy ps = new ParsedStrategy(v, Collections.emptyList(), Collections.emptyMap(),
                Collections.emptyMap(), s);
        final Loan l = ParsedStrategyTest.mockLoan(200_000);
        final LoanDescriptor ld = new LoanDescriptor(l);
        assertThat(ps.getApplicableLoans(Stream.of(ld), FOLIO)).isEmpty();
        final Participation p = mock(Participation.class);
        final ParticipationDescriptor pd = new ParticipationDescriptor(p, () -> l);
        assertThat(ps.getApplicableParticipations(Stream.of(pd), FOLIO)).isEmpty();
    }
}
