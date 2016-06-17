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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.github.triceo.robozonky.remote.Loan;
import com.github.triceo.robozonky.remote.Rating;
import com.github.triceo.robozonky.remote.Investment;
import com.github.triceo.robozonky.remote.RiskPortfolio;
import com.github.triceo.robozonky.remote.Statistics;
import com.github.triceo.robozonky.remote.Wallet;
import com.github.triceo.robozonky.remote.ZonkyApi;
import com.github.triceo.robozonky.remote.ZotifyApi;
import com.github.triceo.robozonky.strategy.InvestmentStrategy;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.Mockito;

public class InvestingTest {

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
                = Operations.actuallyInvest(mockContext, mockLoan, BigDecimal.ZERO);
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
                = Operations.actuallyInvest(mockContext, mockLoan, remainingBalance);
        Assertions.assertThat(result).isEmpty();
        // test investing under balance
        Mockito.when(mockStrategy.recommendInvestmentAmount(mockLoan, remainingBalance))
                .thenReturn(remainingBalance.add(BigDecimal.ONE).intValue());
        final Optional<Investment> result2
                = Operations.actuallyInvest(mockContext, mockLoan, remainingBalance);
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
                = Operations.actuallyInvest(mockContext, mockLoan, remainingBalance);
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
        final ZonkyApi api = Mockito.mock(ZonkyApi.class);
        final OperationsContext mockContext = Mockito.mock(OperationsContext.class);
        Mockito.when(mockContext.getStrategy()).thenReturn(mockStrategy);
        Mockito.when(mockContext.isDryRun()).thenReturn(false);
        Mockito.when(mockContext.getZonkyApi()).thenReturn(api);
        // test OK
        final Optional<Investment> result
                = Operations.actuallyInvest(mockContext, mockLoan, remainingBalance);
        Assertions.assertThat(result).isNotEmpty();
        Assertions.assertThat(result.get().getLoanId()).isEqualTo(loanId);
        // test fail
        Mockito.doThrow(RuntimeException.class).when(api).invest(Mockito.any(Investment.class));
        final Optional<Investment> result2
                = Operations.actuallyInvest(mockContext, mockLoan, remainingBalance);
        Assertions.assertThat(result2).isEmpty();
    }

    private static Loan mockLoan(final int id, final double amount, final int term, final Rating rating) {
        final Loan loan = Mockito.mock(Loan.class);
        Mockito.when(loan.getRemainingInvestment()).thenReturn(amount * 2);
        Mockito.when(loan.getAmount()).thenReturn(amount);
        Mockito.when(loan.getId()).thenReturn(id);
        Mockito.when(loan.getTermInMonths()).thenReturn(term);
        Mockito.when(loan.getRating()).thenReturn(rating);
        return loan;
    }

    private static Loan mockLoan(final int id, final double amount, final int term) {
        return InvestingTest.mockLoan(id, amount, term, null);
    }

    private static void testLoanLength(final OperationsContext context, final Collection<Loan> loans,
                                       final Loan compare) {
        final Optional<Investment> result = Operations.identifyLoanToInvest(context, null, loans,
                Collections.emptyList(), BigDecimal.valueOf(1000));
        Assertions.assertThat(result).isNotEmpty();
        final Investment shortInvestment = result.get();
        Assertions.assertThat(shortInvestment.getLoanId()).isEqualTo(compare.getId());
    }

    @Test
    public void identifyLoansAndInvest() throws Exception {
        final Loan shortLoan = InvestingTest.mockLoan(1, 100000.0, 10);
        final Loan longLoan = InvestingTest.mockLoan(2, 200000.0, 20);
        final Collection<Loan> loans = Arrays.asList(shortLoan, longLoan);
        // mock strategy to return a default recommended investment amount on an acceptable loan
        final InvestmentStrategy strategy = Mockito.mock(InvestmentStrategy.class);
        Mockito.when(strategy.isAcceptable(Mockito.any(Loan.class))).thenReturn(true);
        Mockito.when(strategy.recommendInvestmentAmount(Mockito.any(Loan.class), Mockito.any(BigDecimal.class)))
                .thenReturn(500);
        // mock API to perform loans just fine
        final ZonkyApi api = Mockito.mock(ZonkyApi.class);

        final OperationsContext context = new OperationsContext(api, TestUtil.newZotifyApi(), strategy, false, -1);
        // test preference for shorter terms
        Mockito.when(strategy.prefersLongerTerms(Mockito.any(Rating.class))).thenReturn(false);
        InvestingTest.testLoanLength(context, loans, shortLoan);
        // test preference for longer terms
        Mockito.when(strategy.prefersLongerTerms(Mockito.any(Rating.class))).thenReturn(true);
        InvestingTest.testLoanLength(context, loans, longLoan);
        // mock API to perform no loans
        Mockito.doThrow(InterruptedException.class).when(api).invest(Mockito.any(Investment.class));
        final Optional<Investment> result = Operations.identifyLoanToInvest(context, null, loans,
                Collections.emptyList(), BigDecimal.valueOf(1000));
        Assertions.assertThat(result).isEmpty();
    }

    @Test
    public void rankLoansAndIdentifyLoansAndInvest() throws Exception {
        final Loan shortLoanA = InvestingTest.mockLoan(1, 100000.0, 10, Rating.A);
        final Loan longLoanA = InvestingTest.mockLoan(2, 200000.0, 20, Rating.A);
        final Loan shortLoanB = InvestingTest.mockLoan(3, 50000.0, 5, Rating.B);
        final Loan longLoanB = InvestingTest.mockLoan(4, 300000.0, 25, Rating.B);
        // mock strategy to return a default recommended investment amount on an acceptable loan
        final InvestmentStrategy strategy = Mockito.mock(InvestmentStrategy.class);
        Mockito.when(strategy.isAcceptable(Mockito.any(Loan.class))).thenReturn(true);
        Mockito.when(strategy.recommendInvestmentAmount(Mockito.any(Loan.class), Mockito.any(BigDecimal.class)))
                .thenReturn(500);
        // mock API to perform loans just fine
        final ZonkyApi api = Mockito.mock(ZonkyApi.class);
        final ZotifyApi zotifyApi = Mockito.mock(ZotifyApi.class);
        // mock API to return loans we need
        Mockito.when(strategy.prefersLongerTerms(Rating.A)).thenReturn(false);
        Mockito.when(strategy.prefersLongerTerms(Rating.B)).thenReturn(true);
        Mockito.when(zotifyApi.getLoans()).thenReturn(Arrays.asList(shortLoanA, shortLoanB, longLoanA, longLoanB));
        Mockito.when(strategy.getTargetShare(Mockito.any(Rating.class))).thenReturn(BigDecimal.valueOf(0.01));
        final OperationsContext ctx = new OperationsContext(api, zotifyApi, strategy, false, -1);
        // test that rating A, which is underinvested, will invest shorter loan
        final Statistics stats = Mockito.mock(Statistics.class);
        final RiskPortfolio riskA = new RiskPortfolio(Rating.A, -1, 0, -1, -1);
        final RiskPortfolio riskB = new RiskPortfolio(Rating.B, -1, 1000, -1, -1);
        Mockito.when(stats.getRiskPortfolio()).thenReturn(Arrays.asList(riskA, riskB));
        final Optional<Investment> result = Operations.identifyLoanToInvest(ctx, stats, Collections.emptyList(),
                BigDecimal.valueOf(1000));
        Assertions.assertThat(result).isNotEmpty();
        Assertions.assertThat(result.get().getLoanId()).isEqualTo(shortLoanA.getId());
        // test that rating B, which is newly underinvested, will invest longer loan
        final RiskPortfolio newRiskA = new RiskPortfolio(Rating.A, -1, 1000, -1, -1);
        final RiskPortfolio newRiskB = new RiskPortfolio(Rating.B, -1, 0, -1, -1);
        Mockito.when(stats.getRiskPortfolio()).thenReturn(Arrays.asList(newRiskA, newRiskB));
        final Optional<Investment> newResult = Operations.identifyLoanToInvest(ctx, stats, Collections.emptyList(),
                BigDecimal.valueOf(1000));
        Assertions.assertThat(newResult).isNotEmpty();
        Assertions.assertThat(newResult.get().getLoanId()).isEqualTo(longLoanB.getId());
    }

    @Test
    public void topDownInvestingWithBalanceManagement() throws Exception {
        final Loan shortLoanA = InvestingTest.mockLoan(1, 100000.0, 10, Rating.A);
        final Loan longLoanA = InvestingTest.mockLoan(2, 200000.0, 20, Rating.A);
        final Loan shortLoanB = InvestingTest.mockLoan(3, 50000.0, 5, Rating.B);
        final Loan longLoanB = InvestingTest.mockLoan(4, 300000.0, 25, Rating.B);
        // mock strategy to return a default recommended investment amount on an acceptable loan
        final InvestmentStrategy strategy = Mockito.mock(InvestmentStrategy.class);
        Mockito.when(strategy.isAcceptable(Mockito.any(Loan.class))).thenReturn(true);
        Mockito.when(strategy.recommendInvestmentAmount(Mockito.any(Loan.class), Mockito.any(BigDecimal.class)))
                .thenReturn(400);
        // mock API to perform loans just fine
        final ZonkyApi api = Mockito.mock(ZonkyApi.class);
        final ZotifyApi zotifyApi = Mockito.mock(ZotifyApi.class);
        // mock API to return loans we need
        Mockito.when(strategy.prefersLongerTerms(Rating.A)).thenReturn(false);
        Mockito.when(strategy.prefersLongerTerms(Rating.B)).thenReturn(true);
        Mockito.when(zotifyApi.getLoans()).thenReturn(Arrays.asList(shortLoanA, longLoanA, shortLoanB, longLoanB));
        // both ratings are not represented at all; A is asking for 60 % representation, B for 30 %
        Mockito.when(strategy.getTargetShare(Rating.A)).thenReturn(BigDecimal.valueOf(0.6));
        Mockito.when(strategy.getTargetShare(Rating.B)).thenReturn(BigDecimal.valueOf(0.3));
        for (final Rating r: Rating.values()) { // mock values for all other ratings, to prevent NPEs in test
            if (r == Rating.A || r == Rating.B) {
                continue;
            }
            Mockito.when(strategy.getTargetShare(r)).thenReturn(BigDecimal.valueOf(0.01));
        }
        final Statistics stats = Mockito.mock(Statistics.class);
        final RiskPortfolio riskA = new RiskPortfolio(Rating.A, -1, 0, -1, -1);
        final RiskPortfolio riskB = new RiskPortfolio(Rating.B, -1, 0, -1, -1);
        Mockito.when(stats.getRiskPortfolio()).thenReturn(Arrays.asList(riskA, riskB));
        Mockito.when(api.getStatistics()).thenReturn(stats);
        final Wallet w = new Wallet(-1, -1, BigDecimal.valueOf(10000), BigDecimal.valueOf(9000));
        Mockito.when(api.getWallet()).thenReturn(w); // FIXME balance will not be updated during investing
        final OperationsContext ctx = new OperationsContext(api, zotifyApi, strategy, false, -1);
        // test that investments were made according to the strategy
        final List<Investment> result = new ArrayList<>(Operations.invest(ctx));
        Assertions.assertThat(result).hasSize(3);
        Assertions.assertThat(result.get(0).getLoanId()).isEqualTo(shortLoanA.getId());
        Assertions.assertThat(result.get(1).getLoanId()).isEqualTo(longLoanB.getId());
        Assertions.assertThat(result.get(2).getLoanId()).isEqualTo(longLoanA.getId());
    }

}
