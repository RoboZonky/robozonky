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

package com.github.triceo.robozonky;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.github.triceo.robozonky.remote.BlockedAmount;
import com.github.triceo.robozonky.remote.InvestingZonkyApi;
import com.github.triceo.robozonky.remote.Investment;
import com.github.triceo.robozonky.remote.Loan;
import com.github.triceo.robozonky.remote.ZonkyApi;
import com.github.triceo.robozonky.remote.ZotifyApi;
import com.github.triceo.robozonky.strategy.InvestmentStrategy;
import org.assertj.core.api.Assertions;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;

public class InvestorTest {

    private static Investment getMockInvestmentWithId(final int id) {
        final Investment i = Mockito.mock(Investment.class);
        Mockito.when(i.getLoanId()).thenReturn(id);
        return i;
    }

    private static Loan getMockLoanWithId(final int id) {
        return InvestorTest.getMockLoanWithIdAndAmount(id, 1000);
    }

    private static Loan getMockLoanWithIdAndAmount(final int id, final int amount) {
        final Loan l = Mockito.mock(Loan.class);
        Mockito.when(l.getId()).thenReturn(id);
        Mockito.when(l.getAmount()).thenReturn((double)amount);
        return l;
    }

    @Test
    public void mergingTwoInvestmentCollectionsWorksProperly() {
        final Investment I1 = InvestorTest.getMockInvestmentWithId(1);
        final Investment I2 = InvestorTest.getMockInvestmentWithId(2);
        final Investment I3 = InvestorTest.getMockInvestmentWithId(3);

        // two identical investments will result in one
        final List<Investment> a = Arrays.asList(I1, I2);
        final List<Investment> b = Arrays.asList(I2, I3);
        Assertions.assertThat(Investor.mergeInvestments(a, b)).containsExactly(I1, I2, I3);

        // toy around with empty lists
        Assertions.assertThat(Investor.mergeInvestments(Collections.emptyList(), Collections.emptyList())).isEmpty();
        Assertions.assertThat(Investor.mergeInvestments(Collections.emptyList(), a))
                .containsExactly((Investment[]) a.toArray());
        Assertions.assertThat(Investor.mergeInvestments(b, Collections.emptyList()))
                .containsExactly((Investment[]) b.toArray());

        // standard merging also works
        final List<Investment> c = Collections.singletonList(I3);
        Assertions.assertThat(Investor.mergeInvestments(a, c)).containsExactly(I1, I2, I3);

        // reverse-order merging works
        final List<Investment> d = Arrays.asList(I2, I1);
        Assertions.assertThat(Investor.mergeInvestments(a, d)).containsExactly(I1, I2);

        // two non-identical loans with same ID are merged in the order in which they came
        final Investment I3_2 = InvestorTest.getMockInvestmentWithId(3);
        final List<Investment> e = Collections.singletonList(I3_2);
        Assertions.assertThat(Investor.mergeInvestments(c, e)).containsExactly(I3);
        Assertions.assertThat(Investor.mergeInvestments(e, c)).containsExactly(I3_2);
    }

    @Test
    public void askStrategyForLoans() {
        // prepare loans so that only one will survive
        final Loan l1 = InvestorTest.getMockLoanWithId(1);
        final Loan l2 = InvestorTest.getMockLoanWithId(2);
        final Investment i1 = InvestorTest.getMockInvestmentWithId(l1.getId());
        // mock API operations
        final ZotifyApi api = Mockito.mock(ZotifyApi.class);
        Mockito.when(api.getLoans()).thenReturn(Arrays.asList(l1, l2));
        final PortfolioOverview portfolio = Mockito.mock(PortfolioOverview.class);
        final InvestmentStrategy strategy = Mockito.mock(InvestmentStrategy.class);
        // perform
        final Investor investor = new Investor(Mockito.mock(ZonkyApi.class), api, strategy, BigDecimal.valueOf(1000));
        investor.askStrategyForLoans(Collections.singletonList(i1), portfolio);
        // make sure that only the one loan makes it into the strategy API
        Mockito.verify(strategy, Mockito.times(1)).getMatchingLoans(Collections.singletonList(l2), portfolio);
    }

    @Test
    public void investOnRecommendations() {
        // prepare loans so that only one will survive
        final BigDecimal balance = BigDecimal.valueOf(1000);
        final Loan overBalance = InvestorTest.getMockLoanWithIdAndAmount(1, balance.intValue() * 2);
        final Loan underMinimum =
                InvestorTest.getMockLoanWithIdAndAmount(2, InvestmentStrategy.MINIMAL_INVESTMENT_ALLOWED);
        final int amount = 500;
        final Loan overAmount = InvestorTest.getMockLoanWithIdAndAmount(3, amount);
        final Loan failing = InvestorTest.getMockLoanWithIdAndAmount(4, 400);
        final Loan success = InvestorTest.getMockLoanWithIdAndAmount(5, amount);
        // prepare pre-conditions for the above loans
        final PortfolioOverview portfolio = Mockito.mock(PortfolioOverview.class);
        Mockito.when(portfolio.getCzkAvailable()).thenReturn(balance);
        final InvestmentStrategy strategy = Mockito.mock(InvestmentStrategy.class);
        Mockito.when(strategy.recommendInvestmentAmount(overBalance, portfolio)).thenReturn(balance.intValue() + 1);
        Mockito.when(strategy.recommendInvestmentAmount(underMinimum, portfolio)).thenReturn(0);
        Mockito.when(strategy.recommendInvestmentAmount(overAmount, portfolio)).thenReturn(amount + 1);
        Mockito.when(strategy.recommendInvestmentAmount(failing, portfolio)).thenReturn(200);
        Mockito.when(strategy.recommendInvestmentAmount(success, portfolio)).thenReturn(amount / 2);
        final InvestingZonkyApi api = Mockito.mock(InvestingZonkyApi.class);
        Mockito.doThrow(IllegalStateException.class)
                .when(api).invest(Matchers.argThat(new InvestorTest.InvestmentBaseMatcher(failing)));
        // and now actually test that the succeeding loan will be invested into ...
        final Investor i = new Investor(api, null, strategy, balance);
        final Optional<Investment> result =
                i.findLoanAndInvest(Arrays.asList(overBalance, underMinimum, overAmount, failing, success), portfolio);
        Assertions.assertThat(result).isPresent();
        Assertions.assertThat(result.get().getLoanId()).isEqualTo(success.getId());
        Mockito.verify(api, Mockito.times(2)).invest(Matchers.any());
        // ... no matter which place it takes
        final Optional<Investment> result2 =
                i.findLoanAndInvest(Arrays.asList(success, overBalance, underMinimum, overAmount, failing), portfolio);
        Assertions.assertThat(result2).isPresent();
        Assertions.assertThat(result2.get().getLoanId()).isEqualTo(success.getId());
        Mockito.verify(api, Mockito.times(3)).invest(Matchers.any());
        // ... even when nothing is accepted
        final Optional<Investment> result3 =
                i.findLoanAndInvest(Arrays.asList(overBalance, underMinimum, overAmount), portfolio);
        Assertions.assertThat(result3).isEmpty();
    }

    private static class InvestmentBaseMatcher extends BaseMatcher<Investment> {
        private final Loan failing;

        public InvestmentBaseMatcher(final Loan failing) {
            this.failing = failing;
        }

        @Override
        public void describeTo(final Description description) {
            description.appendText("Matches only the investment related to a loan that is supposed to fail.");
        }

        @Override
        public boolean matches(final Object item) {
            return ((Investment) item).getLoanId() == failing.getId();
        }
    }

    @Test
    public void properBlockedAmountRetrieval() {
        final int loan1id = 1, loan1amount = 100;
        final Loan l1 = InvestorTest.getMockLoanWithIdAndAmount(loan1id, loan1amount);
        final int loan2Id = 2, loan2amount = 200;
        final Loan l2 = InvestorTest.getMockLoanWithIdAndAmount(loan2Id, loan2amount);
        final ZonkyApi api = Mockito.mock(ZonkyApi.class);
        Mockito.when(api.getLoan(loan1id)).thenReturn(l1);
        Mockito.when(api.getLoan(loan2Id)).thenReturn(l2);
        Mockito.when(api.getBlockedAmounts()).thenReturn(
                Arrays.asList(new BlockedAmount(0, 1000), new BlockedAmount(loan1id, loan1amount),
                        new BlockedAmount(loan2Id, loan2amount))
        );
        final List<Investment> result = Investor.retrieveInvestmentsRepresentedByBlockedAmounts(api);
        // the 0 ID blocked amount is Zonky's investors' fee, which should not be looked up as a loan
        Assertions.assertThat(result).hasSize(2);
        Assertions.assertThat(result.get(0).getLoanId()).isEqualTo(l1.getId());
        Assertions.assertThat(result.get(1).getLoanId()).isEqualTo(l2.getId());
    }
}
