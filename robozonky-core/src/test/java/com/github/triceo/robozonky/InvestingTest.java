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
import java.util.Optional;

import com.github.triceo.robozonky.remote.Investment;
import com.github.triceo.robozonky.remote.Loan;
import com.github.triceo.robozonky.remote.ZonkyApi;
import com.github.triceo.robozonky.strategy.InvestmentStrategy;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.Mockito;

public class InvestingTest {

    @Test
    public void investUnderMinimum() {
        final double remainingLoanAmount = Operations.MINIMAL_INVESTMENT_ALLOWED - 1.0;
        final BigDecimal remainingBalance = BigDecimal.valueOf(Operations.MINIMAL_INVESTMENT_ALLOWED + 1);
        // mock
        final Loan mockLoan = Mockito.mock(Loan.class);
        Mockito.when(mockLoan.getId()).thenReturn(1);
        Mockito.when(mockLoan.getAmount()).thenReturn(remainingLoanAmount);
        final InvestmentStrategy mockStrategy = Mockito.mock(InvestmentStrategy.class);
        Mockito.when(
                mockStrategy.recommendInvestmentAmount(
                        Mockito.eq(mockLoan),
                        Mockito.any(),
                        Mockito.eq(remainingBalance)))
                .thenReturn((int) remainingLoanAmount);
        final OperationsContext mockContext = Mockito.mock(OperationsContext.class);
        Mockito.when(mockContext.getStrategy()).thenReturn(mockStrategy);
        // test investing under minimum loan amount
        final Optional<Investment> result
                = Operations.actuallyInvest(mockContext, mockLoan, remainingBalance.intValue(), remainingBalance);
        Assertions.assertThat(result).isEmpty();
        // test investing under balance
        Mockito.when(
                mockStrategy.recommendInvestmentAmount(
                        Mockito.eq(mockLoan),
                        Mockito.any(),
                        Mockito.eq(remainingBalance)))
                .thenReturn(remainingBalance.add(BigDecimal.ONE).intValue());
        final Optional<Investment> result2
                = Operations.actuallyInvest(mockContext, mockLoan, remainingBalance.intValue(), remainingBalance);
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
        Mockito.when(
                mockStrategy.recommendInvestmentAmount(
                        Mockito.eq(mockLoan),
                        Mockito.any(),
                        Mockito.eq(remainingBalance)))
                .thenReturn(remainingBalance.intValue());
        final OperationsContext mockContext = Mockito.mock(OperationsContext.class);
        Mockito.when(mockContext.getStrategy()).thenReturn(mockStrategy);
        Mockito.when(mockContext.isDryRun()).thenReturn(true);
        // test
        final Optional<Investment> result
                = Operations.actuallyInvest(mockContext, mockLoan, remainingBalance.intValue(), remainingBalance);
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
        Mockito.when(
                mockStrategy.recommendInvestmentAmount(
                        Mockito.eq(mockLoan),
                        Mockito.any(),
                        Mockito.eq(remainingBalance)))
                .thenReturn(remainingBalance.intValue());
        final ZonkyApi api = Mockito.mock(ZonkyApi.class);
        final OperationsContext mockContext = Mockito.mock(OperationsContext.class);
        Mockito.when(mockContext.getStrategy()).thenReturn(mockStrategy);
        Mockito.when(mockContext.isDryRun()).thenReturn(false);
        Mockito.when(mockContext.getZonkyApi()).thenReturn(api);
        // test OK
        final Optional<Investment> result
                = Operations.actuallyInvest(mockContext, mockLoan, remainingBalance.intValue(), remainingBalance);
        Assertions.assertThat(result).isNotEmpty();
        Assertions.assertThat(result.get().getLoanId()).isEqualTo(loanId);
        // test fail
        Mockito.doThrow(RuntimeException.class).when(api).invest(Mockito.any(Investment.class));
        final Optional<Investment> result2
                = Operations.actuallyInvest(mockContext, mockLoan, remainingBalance.intValue(), remainingBalance);
        Assertions.assertThat(result2).isEmpty();
    }

}
