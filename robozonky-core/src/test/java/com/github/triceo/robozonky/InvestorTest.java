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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.github.triceo.robozonky.api.events.Event;
import com.github.triceo.robozonky.api.events.EventListener;
import com.github.triceo.robozonky.api.events.EventRegistry;
import com.github.triceo.robozonky.api.events.InvestmentMadeEvent;
import com.github.triceo.robozonky.api.events.InvestmentRequestedEvent;
import com.github.triceo.robozonky.api.events.LoanEvaluationEvent;
import com.github.triceo.robozonky.api.events.StrategyCompleteEvent;
import com.github.triceo.robozonky.api.events.StrategyStartedEvent;
import com.github.triceo.robozonky.api.remote.InvestingZonkyApi;
import com.github.triceo.robozonky.api.remote.ZonkyApi;
import com.github.triceo.robozonky.api.remote.entities.BlockedAmount;
import com.github.triceo.robozonky.api.remote.entities.Investment;
import com.github.triceo.robozonky.api.remote.entities.Loan;
import com.github.triceo.robozonky.api.remote.entities.Statistics;
import com.github.triceo.robozonky.api.strategies.InvestmentStrategy;
import com.github.triceo.robozonky.api.strategies.PortfolioOverview;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.jboss.resteasy.spi.BadRequestException;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public class InvestorTest {

    private static Loan getMockLoanWithId(final int id) {
        return InvestorTest.getMockLoanWithIdAndAmount(id, 1000);
    }

    private static Loan getMockLoanWithIdAndAmount(final int id, final int amount) {
        final Loan l = Mockito.mock(Loan.class);
        Mockito.when(l.getId()).thenReturn(id);
        Mockito.when(l.getRemainingInvestment()).thenReturn((double)amount);
        return l;
    }

    @Test(expected = BadRequestException.class)
    public void terminateOnFailedInvestment() {
        // the strategy will recommend two different investments
        final Loan mockLoan1 = InvestorTest.getMockLoanWithId(1);
        final InvestmentStrategy strategyMock = Mockito.mock(InvestmentStrategy.class);
        Mockito.when(strategyMock.getMatchingLoans(Mockito.any(), Mockito.any()))
                .thenReturn(Collections.singletonList(mockLoan1));
        Mockito.when(strategyMock.recommendInvestmentAmount(Mockito.any(), Mockito.any())).thenReturn(400);
        // fail on the first loan, accept the second
        final InvestingZonkyApi mockApi = Mockito.mock(InvestingZonkyApi.class);
        Mockito.when(mockApi.getLoan(Mockito.eq(mockLoan1.getId()))).thenReturn(mockLoan1);
        Mockito.doThrow(BadRequestException.class)
                .when(mockApi).invest(Mockito.argThat(new InvestorTest.InvestmentBaseMatcher(mockLoan1)));
        // finally test
        final Investor investor = new Investor(mockApi, BigDecimal.valueOf(1000));
        investor.invest(strategyMock, Collections.singletonList(mockLoan1));
    }

    @Test
    public void properEventsFiring() {
        // create two loans to exercise the strategy
        final int loan1Id = 1;
        final Loan loan1 = InvestorTest.getMockLoanWithId(loan1Id);
        final int loan2Id = 2;
        final Loan loan2 = InvestorTest.getMockLoanWithId(loan2Id);
        // prepare the strategy; loan1 is acceptable, subsequently loan2 is
        final InvestmentStrategy strategy = Mockito.mock(InvestmentStrategy.class);
        Mockito.when(strategy.getMatchingLoans(ArgumentMatchers.argThat((a) -> a != null && a.size() == 2),
                ArgumentMatchers.any())).thenReturn(Collections.singletonList(loan1));
        Mockito.when(strategy.getMatchingLoans(ArgumentMatchers.argThat((a) -> a != null && a.size() == 1),
                ArgumentMatchers.any())).thenReturn(Collections.singletonList(loan2));
        Mockito.when(strategy.recommendInvestmentAmount(ArgumentMatchers.eq(loan1), ArgumentMatchers.any()))
                .thenReturn(400);
        Mockito.when(strategy.recommendInvestmentAmount(ArgumentMatchers.eq(loan2), ArgumentMatchers.any()))
                .thenReturn(0); // do not invest the second time
        // configure API to retrieve the loans
        final ZonkyApi api = Mockito.mock(ZonkyApi.class);
        Mockito.when(api.getLoan(ArgumentMatchers.eq(loan1Id))).thenReturn(loan1);
        Mockito.when(api.getLoan(ArgumentMatchers.eq(loan2Id))).thenReturn(loan2);
        // execute the actual test
        final EventListener<Event> listener = Mockito.mock(EventListener.class);
        EventRegistry.INSTANCE.addListener(listener);
        final Investor investor = new Investor(api, BigDecimal.valueOf(1000));
        final Collection<Investment> result = investor.invest(strategy, Arrays.asList(loan1, loan2));
        Assertions.assertThat(result).hasSize(1);
        // and check for the proper event listener invocations
        Mockito.verify(listener, Mockito.times(6)).handle(ArgumentMatchers.any());
        Mockito.verify(listener, Mockito.times(1)).handle(ArgumentMatchers.any(StrategyStartedEvent.class));
        Mockito.verify(listener, Mockito.times(2)).handle(ArgumentMatchers.any(LoanEvaluationEvent.class));
        Mockito.verify(listener, Mockito.times(1)).handle(ArgumentMatchers.any(InvestmentRequestedEvent.class));
        Mockito.verify(listener, Mockito.times(1)).handle(ArgumentMatchers.any(InvestmentMadeEvent.class));
        Mockito.verify(listener, Mockito.times(1)).handle(ArgumentMatchers.any(StrategyCompleteEvent.class));
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
        final Loan success = InvestorTest.getMockLoanWithIdAndAmount(5, amount);
        final Statistics stats = Mockito.mock(Statistics.class);
        Mockito.when(stats.getRiskPortfolio()).thenReturn(Collections.emptyList());
        // prepare pre-conditions for the above loans
        final InvestmentStrategy strategy = Mockito.mock(InvestmentStrategy.class);
        Mockito.when(strategy.recommendInvestmentAmount(Mockito.eq(overBalance), Mockito.any()))
                .thenReturn(balance.intValue() + 1);
        Mockito.when(strategy.recommendInvestmentAmount(Mockito.eq(underMinimum), Mockito.any())).thenReturn(0);
        Mockito.when(strategy.recommendInvestmentAmount(Mockito.eq(overAmount), Mockito.any()))
                .thenReturn(amount + 1);
        Mockito.when(strategy.recommendInvestmentAmount(Mockito.eq(success), Mockito.any())).thenReturn(amount / 2);
        final List<Loan> allLoans = Arrays.asList(overBalance, underMinimum, overAmount, success);
        // mock investing api
        final InvestingZonkyApi api = Mockito.mock(InvestingZonkyApi.class);
        for (final Loan l: allLoans) {
            Mockito.when(api.getLoan(Mockito.eq(l.getId()))).thenReturn(l);
        }
        // and now actually test that the succeeding loan will be invested into ...
        final Investor i = new Investor(api, balance);
        Mockito.when(strategy.getMatchingLoans(Mockito.any(), Mockito.any())).thenReturn(allLoans);
        final Optional<Investment> result = i.investOnce(strategy, allLoans,
                PortfolioOverview.calculate(balance, stats, Collections.emptyList()));
        Assertions.assertThat(result).isPresent();
        Assertions.assertThat(result.get().getLoanId()).isEqualTo(success.getId());
        Mockito.verify(api, Mockito.times(1)).invest(Mockito.any());
        // ... no matter which place it takes
        Mockito.when(strategy.getMatchingLoans(Mockito.any(), Mockito.any()))
                .thenReturn(Arrays.asList(success, overBalance, underMinimum, overAmount));
        final Optional<Investment> result2 = i.investOnce(strategy, allLoans,
                PortfolioOverview.calculate(balance, stats, Collections.emptyList()));
        Assertions.assertThat(result2).isPresent();
        Assertions.assertThat(result2.get().getLoanId()).isEqualTo(success.getId());
        Mockito.verify(api, Mockito.times(2)).invest(Mockito.any());
        // ... even when nothing is accepted
        final Loan alreadyPresent = InvestorTest.getMockLoanWithIdAndAmount(6, 10000);
        Mockito.when(strategy.getMatchingLoans(Mockito.any(), Mockito.any()))
                .thenReturn(Arrays.asList(overBalance, underMinimum, overAmount, alreadyPresent));
        final Investment alreadyPresentInvestment = new Investment(alreadyPresent, 200);
        final Optional<Investment> result3 = i.investOnce(strategy, null,
                PortfolioOverview.calculate(balance, stats, Collections.singletonList(alreadyPresentInvestment)));
        Assertions.assertThat(result3).isEmpty();
    }

    private static class InvestmentBaseMatcher implements ArgumentMatcher<Investment> {
        private final Loan matching;

        public InvestmentBaseMatcher(final Loan matching) {
            this.matching = matching;
        }

        @Override
        public boolean matches(final Investment argument) {
            return argument.getLoanId() == matching.getId();
        }
    }

    @Test
    public void properBlockedAmountRetrieval() {
        final int loan1id = 1, loan1amount = 100;
        final Loan l1 = InvestorTest.getMockLoanWithIdAndAmount(loan1id, loan1amount);
        final int loan2id = loan1id + 1, loan2amount = 200;
        final Loan l2 = InvestorTest.getMockLoanWithIdAndAmount(loan2id, loan2amount);
        final int loan3amount = 400;
        final ZonkyApi api = Mockito.mock(ZonkyApi.class);
        Mockito.when(api.getLoan(Mockito.eq(loan1id))).thenReturn(l1);
        Mockito.when(api.getLoan(Mockito.eq(loan2id))).thenReturn(l2);
        Mockito.when(api.getBlockedAmounts(Mockito.anyInt(), Mockito.anyInt())).thenReturn(
                Arrays.asList(new BlockedAmount(0, 1000), new BlockedAmount(loan1id, loan1amount),
                        new BlockedAmount(loan2id, loan2amount), new BlockedAmount(loan1id, loan3amount))
        );
        final List<Investment> result = Investor.retrieveInvestmentsRepresentedByBlockedAmounts(api);
        // the 0 ID blocked amount is Zonky's investors' fee, which should not be looked up as a loan
        final SoftAssertions softly = new SoftAssertions();
        softly.assertThat(result).hasSize(2);
        final Investment i1 = result.get(0);
        softly.assertThat(i1.getLoanId()).isEqualTo(loan1id);
        softly.assertThat(i1.getAmount()).isEqualTo(loan1amount + loan3amount);
        final Investment i2 = result.get(1);
        softly.assertThat(i2.getLoanId()).isEqualTo(loan2id);
        softly.assertThat(i2.getAmount()).isEqualTo(loan2amount);
        softly.assertAll();
    }

}
