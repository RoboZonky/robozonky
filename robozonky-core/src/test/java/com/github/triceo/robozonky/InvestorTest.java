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
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.github.triceo.robozonky.remote.InvestingZonkyApi;
import com.github.triceo.robozonky.remote.Investment;
import com.github.triceo.robozonky.remote.Loan;
import com.github.triceo.robozonky.remote.Rating;
import com.github.triceo.robozonky.remote.RiskPortfolio;
import com.github.triceo.robozonky.remote.Statistics;
import com.github.triceo.robozonky.strategy.InvestmentStrategy;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.Mockito;

public class InvestorTest {

    private static Investment getMockInvestmentWithId(final int id) {
        final Investment i = Mockito.mock(Investment.class);
        Mockito.when(i.getLoanId()).thenReturn(id);
        return i;
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

    private static void assertProperRatingShare(final Map<Rating, BigDecimal> result, final Rating r, final int amount,
                                                final int total) {
        final BigDecimal expectedShare
                = BigDecimal.valueOf(amount).divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_EVEN);
        Assertions.assertThat(result.get(r)).isEqualTo(expectedShare);
    }

    private static List<Investment> getMockInvestmentWithBalance(final int loanAmount) {
        final Investment i = Mockito.mock(Investment.class);
        Mockito.when(i.getAmount()).thenReturn(loanAmount);
        return Collections.singletonList(i);
    }

    @Test
    public void properRatingShareCalculation() {
        // mock necessary structures
        final int amountAA = 300, amountB = 200, amountD = 100;
        final int totalPie = amountAA + amountB + amountD;
        final RiskPortfolio riskAA = new RiskPortfolio(Rating.AA, -1, amountAA, -1, -1);
        final RiskPortfolio riskB = new RiskPortfolio(Rating.B, -1, amountB, -1, -1);
        final RiskPortfolio riskD = new RiskPortfolio(Rating.D, -1, amountD, -1, -1);
        final Statistics stats = Mockito.mock(Statistics.class);
        Mockito.when(stats.getRiskPortfolio()).thenReturn(Arrays.asList(riskAA, riskB, riskD));

        // check standard operation
        Map<Rating, BigDecimal> result = Investor.calculateSharesPerRating(stats, Collections.emptyList());
        InvestorTest.assertProperRatingShare(result, Rating.AA, amountAA, totalPie);
        InvestorTest.assertProperRatingShare(result, Rating.B, amountB, totalPie);
        InvestorTest.assertProperRatingShare(result, Rating.D, amountD, totalPie);
        InvestorTest.assertProperRatingShare(result, Rating.AAAAA, 0, totalPie); // test other ratings included
        InvestorTest.assertProperRatingShare(result, Rating.AAAA, 0, totalPie);
        InvestorTest.assertProperRatingShare(result, Rating.AAA, 0, totalPie);
        InvestorTest.assertProperRatingShare(result, Rating.A, 0, totalPie);
        InvestorTest.assertProperRatingShare(result, Rating.C, 0, totalPie);
        // check operation with offline investments
        final int increment = 200, newTotalPie = totalPie + increment;
        final List<Investment> investments = InvestorTest.getMockInvestmentWithBalance(increment);
        final Investment i = investments.get(0);
        Mockito.when(i.getRating()).thenReturn(Rating.D);
        result = Investor.calculateSharesPerRating(stats, investments);
        InvestorTest.assertProperRatingShare(result, Rating.AA, amountAA, newTotalPie);
        InvestorTest.assertProperRatingShare(result, Rating.B, amountB, newTotalPie);
        InvestorTest.assertProperRatingShare(result, Rating.D, amountD + increment, newTotalPie);
        InvestorTest.assertProperRatingShare(result, Rating.AAAAA, 0, newTotalPie); // test other ratings included
        InvestorTest.assertProperRatingShare(result, Rating.AAAA, 0, newTotalPie);
        InvestorTest.assertProperRatingShare(result, Rating.AAA, 0, newTotalPie);
        InvestorTest.assertProperRatingShare(result, Rating.A, 0, newTotalPie);
        InvestorTest.assertProperRatingShare(result, Rating.C, 0, newTotalPie);
    }

    @Test
    public void investUnderMinimum() {
        final double remainingLoanAmount = InvestmentStrategy.MINIMAL_INVESTMENT_ALLOWED - 1.0;
        final BigDecimal remainingBalance = BigDecimal.valueOf(InvestmentStrategy.MINIMAL_INVESTMENT_ALLOWED + 1);
        // mock
        final Loan mockLoan = Mockito.mock(Loan.class);
        Mockito.when(mockLoan.getId()).thenReturn(1);
        Mockito.when(mockLoan.getAmount()).thenReturn(remainingLoanAmount);
        // test investing under minimum loan amount
        final Optional<Investment> result = Investor.invest(null, mockLoan, remainingBalance.intValue(),
                remainingBalance);
        Assertions.assertThat(result).isEmpty();
    }

    @Test
    public void investOk() {
        final double remainingLoanAmount = 10000;
        final BigDecimal remainingBalance = BigDecimal.valueOf(1000);
        final int loanId = 1;
        // mock
        final Loan mockLoan = Mockito.mock(Loan.class);
        Mockito.when(mockLoan.getId()).thenReturn(loanId);
        Mockito.when(mockLoan.getAmount()).thenReturn(remainingLoanAmount);
        final InvestingZonkyApi api = Mockito.mock(InvestingZonkyApi.class);
        // test OK
        final Optional<Investment> result = Investor.invest(api, mockLoan, remainingBalance.intValue(),
                remainingBalance);
        Assertions.assertThat(result).isNotEmpty();
        Assertions.assertThat(result.get().getLoanId()).isEqualTo(loanId);
        // test fail
        Mockito.doThrow(RuntimeException.class).when(api).invest(Mockito.any(Investment.class));
        final Optional<Investment> result2 = Investor.invest(api, mockLoan, remainingBalance.intValue(),
                remainingBalance);
        Assertions.assertThat(result2).isEmpty();
    }

}
