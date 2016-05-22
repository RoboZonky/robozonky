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
import java.util.Optional;
import java.util.concurrent.Future;

import com.github.triceo.robozonky.remote.Investment;
import com.github.triceo.robozonky.remote.Loan;
import com.github.triceo.robozonky.remote.Rating;
import com.github.triceo.robozonky.remote.Ratings;
import com.github.triceo.robozonky.remote.RiskPortfolio;
import com.github.triceo.robozonky.remote.Statistics;
import com.github.triceo.robozonky.remote.ZonkyAPI;
import com.github.triceo.robozonky.strategy.InvestmentStrategy;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.Mockito;

public class InvestingTest {

    @Test
    public void investWithAlreadyInvestedLoan() {
        // mock
        final Loan mockLoan = Mockito.mock(Loan.class);
        Mockito.when(mockLoan.getId()).thenReturn(1);
        final Investment mockInvestment = new Investment(mockLoan, 200);
        // test
        final Optional<Investment> result
                = Operations.actuallyInvest(null, mockLoan, Collections.singletonList(mockInvestment), BigDecimal.ZERO);
        Assertions.assertThat(result).isEmpty();
    }

    @Test
    public void investWithUnacceptableLoan() {
        // mock
        final Loan mockLoan = Mockito.mock(Loan.class);
        Mockito.when(mockLoan.getId()).thenReturn(1);
        final InvestmentStrategy mockStrategy = Mockito.mock(InvestmentStrategy.class);
        Mockito.when(mockStrategy.isAcceptable(mockLoan)).thenReturn(false);
        final OperationsContext mockContext = Mockito.mock(OperationsContext.class);
        Mockito.when(mockContext.getStrategy()).thenReturn(mockStrategy);
        // test
        final Optional<Investment> result
                = Operations.actuallyInvest(mockContext, mockLoan, Collections.emptyList(), BigDecimal.ZERO);
        Assertions.assertThat(result).isEmpty();
    }

    @Test
    public void investUnderMinimum() {
        final double remainingLoanAmount = Operations.MINIMAL_INVESTMENT_ALLOWED - 1.0;
        final BigDecimal remainingBalance = BigDecimal.valueOf(Operations.MINIMAL_INVESTMENT_ALLOWED + 1);
        // mock
        final Loan mockLoan = Mockito.mock(Loan.class);
        Mockito.when(mockLoan.getId()).thenReturn(1);
        Mockito.when(mockLoan.getAmount()).thenReturn(remainingLoanAmount);
        final InvestmentStrategy mockStrategy = Mockito.mock(InvestmentStrategy.class);
        Mockito.when(mockStrategy.isAcceptable(mockLoan)).thenReturn(true);
        Mockito.when(mockStrategy.recommendInvestmentAmount(mockLoan, remainingBalance))
                .thenReturn((int)remainingLoanAmount);
        final OperationsContext mockContext = Mockito.mock(OperationsContext.class);
        Mockito.when(mockContext.getStrategy()).thenReturn(mockStrategy);
        // test investing under minimum loan amount
        final Optional<Investment> result
                = Operations.actuallyInvest(mockContext, mockLoan, Collections.emptyList(), remainingBalance);
        Assertions.assertThat(result).isEmpty();
        // test investing under balance
        Mockito.when(mockStrategy.recommendInvestmentAmount(mockLoan, remainingBalance))
                .thenReturn(remainingBalance.add(BigDecimal.ONE).intValue());
        final Optional<Investment> result2
                = Operations.actuallyInvest(mockContext, mockLoan, Collections.emptyList(), remainingBalance);
        Assertions.assertThat(result2).isEmpty();
    }

    @Test
    public void investOkInDryRun() {
        final double remainingLoanAmount = 10000;
        final BigDecimal remainingBalance = BigDecimal.valueOf(1000);
        final int loanId = 1;
        // mock
        final Loan mockLoan = Mockito.mock(Loan.class);
        Mockito.when(mockLoan.getId()).thenReturn(loanId);
        Mockito.when(mockLoan.getAmount()).thenReturn(remainingLoanAmount);
        final InvestmentStrategy mockStrategy = Mockito.mock(InvestmentStrategy.class);
        Mockito.when(mockStrategy.isAcceptable(mockLoan)).thenReturn(true);
        Mockito.when(mockStrategy.recommendInvestmentAmount(mockLoan, remainingBalance))
                .thenReturn(remainingBalance.intValue());
        final OperationsContext mockContext = Mockito.mock(OperationsContext.class);
        Mockito.when(mockContext.getStrategy()).thenReturn(mockStrategy);
        Mockito.when(mockContext.isDryRun()).thenReturn(true);
        // test
        final Optional<Investment> result
                = Operations.actuallyInvest(mockContext, mockLoan, Collections.emptyList(), remainingBalance);
        Assertions.assertThat(result).isNotEmpty();
        Assertions.assertThat(result.get().getLoanId()).isEqualTo(loanId);
    }

    @Test
    public void investOkInNormalRun() {
        final double remainingLoanAmount = 10000;
        final BigDecimal remainingBalance = BigDecimal.valueOf(1000);
        final int loanId = 1;
        // mock
        final Loan mockLoan = Mockito.mock(Loan.class);
        Mockito.when(mockLoan.getId()).thenReturn(loanId);
        Mockito.when(mockLoan.getAmount()).thenReturn(remainingLoanAmount);
        final InvestmentStrategy mockStrategy = Mockito.mock(InvestmentStrategy.class);
        Mockito.when(mockStrategy.isAcceptable(mockLoan)).thenReturn(true);
        Mockito.when(mockStrategy.recommendInvestmentAmount(mockLoan, remainingBalance))
                .thenReturn(remainingBalance.intValue());
        final ZonkyAPI api = Mockito.mock(ZonkyAPI.class);
        final OperationsContext mockContext = Mockito.mock(OperationsContext.class);
        Mockito.when(mockContext.getStrategy()).thenReturn(mockStrategy);
        Mockito.when(mockContext.isDryRun()).thenReturn(false);
        Mockito.when(mockContext.getAPI()).thenReturn(api);
        // test OK
        final Optional<Investment> result
                = Operations.actuallyInvest(mockContext, mockLoan, Collections.emptyList(), remainingBalance);
        Assertions.assertThat(result).isNotEmpty();
        Assertions.assertThat(result.get().getLoanId()).isEqualTo(loanId);
        // test fail
        Mockito.doThrow(RuntimeException.class).when(api).invest(Mockito.any(Investment.class));
        final Optional<Investment> result2
                = Operations.actuallyInvest(mockContext, mockLoan, Collections.emptyList(), remainingBalance);
        Assertions.assertThat(result2).isEmpty();
    }

    @Test
    public void identifyLoansFailedFuture() throws Exception {
        final Future<Collection<Loan>> future = Mockito.mock(Future.class);
        // check empty future call
        Mockito.when(future.get()).thenReturn(Collections.emptyList());
        final Optional<Investment> result = Operations.identifyLoanToInvest(null, null, future,
                Collections.emptyList(), null);
        Assertions.assertThat(result).isEmpty();
        // check failing future call
        Mockito.doThrow(InterruptedException.class).when(future).get();
        final Optional<Investment> result2 = Operations.identifyLoanToInvest(null, null, future,
                Collections.emptyList(), null);
        Assertions.assertThat(result2).isEmpty();
    }

    private static Loan mockLoan(final int id, final double amount, final int term) {
        final Loan loan = Mockito.mock(Loan.class);
        Mockito.when(loan.getAmount()).thenReturn(amount);
        Mockito.when(loan.getId()).thenReturn(id);
        Mockito.when(loan.getTermInMonths()).thenReturn(term);
        return loan;
    }

    private static void testLoanLength(final OperationsContext context, final Future<Collection<Loan>> future,
                                       final Loan compare) {
        final Optional<Investment> result = Operations.identifyLoanToInvest(context, null, future,
                Collections.emptyList(), BigDecimal.valueOf(1000));
        Assertions.assertThat(result).isNotEmpty();
        final Investment shortInvestment = result.get();
        Assertions.assertThat(shortInvestment.getLoanId()).isEqualTo(compare.getId());
    }

    @Test
    public void identifyLoansAndInvest() throws Exception {
        final Loan shortLoan = InvestingTest.mockLoan(1, 100000.0, 10);
        final Loan longLoan = InvestingTest.mockLoan(2, 200000.0, 20);
        // mock future to return two loans
        final Future<Collection<Loan>> future = Mockito.mock(Future.class);
        Mockito.when(future.get()).thenReturn(Arrays.asList(shortLoan, longLoan));
        // mock strategy to return a default recommended investment amount on an acceptable loan
        final InvestmentStrategy strategy = Mockito.mock(InvestmentStrategy.class);
        Mockito.when(strategy.isAcceptable(Mockito.any(Loan.class))).thenReturn(true);
        Mockito.when(strategy.recommendInvestmentAmount(Mockito.any(Loan.class), Mockito.any(BigDecimal.class)))
                .thenReturn(500);
        // mock API to perform loans just fine
        final ZonkyAPI api = Mockito.mock(ZonkyAPI.class);
        final OperationsContext context = new OperationsContext(api, strategy, false, -1, 2);
        // test preference for shorter terms
        Mockito.when(strategy.prefersLongerTerms(Mockito.any(Rating.class))).thenReturn(false);
        InvestingTest.testLoanLength(context, future, shortLoan);
        // test preference for longer terms
        Mockito.when(strategy.prefersLongerTerms(Mockito.any(Rating.class))).thenReturn(true);
        InvestingTest.testLoanLength(context, future, longLoan);
        // mock API to perform no loans
        Mockito.doThrow(InterruptedException.class).when(api).invest(Mockito.any(Investment.class));
        final Optional<Investment> result = Operations.identifyLoanToInvest(context, null, future,
                Collections.emptyList(), BigDecimal.valueOf(1000));
        Assertions.assertThat(result).isEmpty();
    }

    @Test
    public void rankLoansAndidentifyLoansAndInvest() throws Exception {
        final Loan shortLoanA = InvestingTest.mockLoan(1, 100000.0, 10);
        final Loan longLoanA = InvestingTest.mockLoan(2, 200000.0, 20);
        final Loan shortLoanB = InvestingTest.mockLoan(3, 50000.0, 5);
        final Loan longLoanB = InvestingTest.mockLoan(4, 300000.0, 25);
        // mock strategy to return a default recommended investment amount on an acceptable loan
        final InvestmentStrategy strategy = Mockito.mock(InvestmentStrategy.class);
        Mockito.when(strategy.isAcceptable(Mockito.any(Loan.class))).thenReturn(true);
        Mockito.when(strategy.recommendInvestmentAmount(Mockito.any(Loan.class), Mockito.any(BigDecimal.class)))
                .thenReturn(500);
        // mock API to perform loans just fine
        final ZonkyAPI api = Mockito.mock(ZonkyAPI.class);
        // mock API to return loans we need
        Mockito.when(strategy.prefersLongerTerms(Rating.A)).thenReturn(false);
        Mockito.when(api.getLoans(Ratings.of(Rating.A), 200))
                .thenReturn(Arrays.asList(shortLoanA, longLoanA));
        Mockito.when(strategy.prefersLongerTerms(Rating.B)).thenReturn(true);
        Mockito.when(api.getLoans(Ratings.of(Rating.B), 200))
                .thenReturn(Arrays.asList(shortLoanB, longLoanB));
        Mockito.when(strategy.getTargetShare(Rating.A)).thenReturn(BigDecimal.valueOf(0.01));
        Mockito.when(strategy.getTargetShare(Rating.B)).thenReturn(BigDecimal.valueOf(0.01));
        final OperationsContext ctx = new OperationsContext(api, strategy, false, -1, 2);
        // test that rating A, which is underinvested, will invest shorter loan
        final Statistics stats = Mockito.mock(Statistics.class);
        final RiskPortfolio riskA = new RiskPortfolio(Rating.A, -1, 0, -1, -1);
        final RiskPortfolio riskB = new RiskPortfolio(Rating.B, -1, 1000, -1, -1);
        Mockito.when(stats.getRiskPortfolio()).thenReturn(Arrays.asList(riskA, riskB));
        Mockito.when(api.getStatistics()).thenReturn(stats);
        final Optional<Investment> result = Operations.identifyLoanToInvest(ctx, Collections.emptyList(),
                BigDecimal.valueOf(1000));
        Assertions.assertThat(result).isNotEmpty();
        Assertions.assertThat(result.get().getLoanId()).isEqualTo(shortLoanA.getId());
        // test that rating B, which is newly underinvested, will invest longer loan
        final RiskPortfolio newRiskA = new RiskPortfolio(Rating.A, -1, 1000, -1, -1);
        final RiskPortfolio newRiskB = new RiskPortfolio(Rating.B, -1, 0, -1, -1);
        Mockito.when(stats.getRiskPortfolio()).thenReturn(Arrays.asList(newRiskA, newRiskB));
        final Optional<Investment> newResult = Operations.identifyLoanToInvest(ctx, Collections.emptyList(),
                BigDecimal.valueOf(1000));
        Assertions.assertThat(newResult).isNotEmpty();
        Assertions.assertThat(newResult.get().getLoanId()).isEqualTo(longLoanB.getId());
        // finish
        ctx.dispose();
    }

}
