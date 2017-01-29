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

package com.github.triceo.robozonky.strategy.simple;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.github.triceo.robozonky.api.remote.entities.Loan;
import com.github.triceo.robozonky.api.remote.enums.Rating;
import com.github.triceo.robozonky.api.strategies.LoanDescriptor;
import com.github.triceo.robozonky.api.strategies.PortfolioOverview;
import com.github.triceo.robozonky.api.strategies.Recommendation;
import com.github.triceo.robozonky.internal.api.Defaults;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public class SimpleInvestmentStategyTest {

    private static final Rating RATING_A = Rating.A;
    private static final Rating RATING_B = Rating.B;
    private static final int TESTED_TERM_LENGTH = 2;
    private static final int MAXIMUM_LOAN_INVESTMENT = 10000;
    private static final int MINIMUM_ASK = SimpleInvestmentStategyTest.MAXIMUM_LOAN_INVESTMENT / 10;
    private static final int MAXIMUM_ASK = SimpleInvestmentStategyTest.MAXIMUM_LOAN_INVESTMENT * 10;
    private static final BigDecimal MAXIMUM_LOAN_SHARE_A = BigDecimal.valueOf(0.01);
    private static final BigDecimal TARGET_SHARE_A = BigDecimal.valueOf(0.2);
    private static final StrategyPerRating STRATEGY_A = new StrategyPerRating(SimpleInvestmentStategyTest.RATING_A,
            SimpleInvestmentStategyTest.TARGET_SHARE_A, BigDecimal.ONE,
            SimpleInvestmentStategyTest.TESTED_TERM_LENGTH - 1, -1, 200,
            SimpleInvestmentStategyTest.MAXIMUM_LOAN_INVESTMENT, BigDecimal.ZERO,
            SimpleInvestmentStategyTest.MAXIMUM_LOAN_SHARE_A, SimpleInvestmentStategyTest.MINIMUM_ASK,
            SimpleInvestmentStategyTest.MAXIMUM_ASK, true, false
    );

    private static final BigDecimal MAXIMUM_LOAN_SHARE_B = BigDecimal.valueOf(0.001);
    private static final BigDecimal TARGET_SHARE_B = SimpleInvestmentStategyTest.TARGET_SHARE_A.add(BigDecimal.valueOf(0.5));
    private static final StrategyPerRating STRATEGY_B = new StrategyPerRating(SimpleInvestmentStategyTest.RATING_B,
            SimpleInvestmentStategyTest.TARGET_SHARE_B, BigDecimal.ONE,
            SimpleInvestmentStategyTest.TESTED_TERM_LENGTH - 1,
            SimpleInvestmentStategyTest.TESTED_TERM_LENGTH + 1, 200,
            SimpleInvestmentStategyTest.MAXIMUM_LOAN_INVESTMENT, BigDecimal.ZERO,
            SimpleInvestmentStategyTest.MAXIMUM_LOAN_SHARE_B, SimpleInvestmentStategyTest.MINIMUM_ASK,
            SimpleInvestmentStategyTest.MAXIMUM_ASK, false, false
    );

    private final SimpleInvestmentStrategy overallStrategy;

    public SimpleInvestmentStategyTest() {
        final Map<Rating, StrategyPerRating> strategies = new EnumMap<>(Rating.class);
        strategies.put(Rating.AAAAA, Mockito.mock(StrategyPerRating.class));
        strategies.put(Rating.AAAA, Mockito.mock(StrategyPerRating.class));
        strategies.put(Rating.AAA, Mockito.mock(StrategyPerRating.class));
        strategies.put(Rating.AA, Mockito.mock(StrategyPerRating.class));
        strategies.put(SimpleInvestmentStategyTest.RATING_A, SimpleInvestmentStategyTest.STRATEGY_A);
        strategies.put(SimpleInvestmentStategyTest.RATING_B, SimpleInvestmentStategyTest.STRATEGY_B);
        strategies.put(Rating.C, Mockito.mock(StrategyPerRating.class));
        strategies.put(Rating.D, Mockito.mock(StrategyPerRating.class));
        overallStrategy = new SimpleInvestmentStrategy(0, Integer.MAX_VALUE, strategies);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorEnsuringAllStrategiesPresent() {
        final Map<Rating, StrategyPerRating> strategies = new EnumMap<>(Rating.class);
        strategies.put(SimpleInvestmentStategyTest.RATING_A, SimpleInvestmentStategyTest.STRATEGY_A);
        strategies.put(SimpleInvestmentStategyTest.RATING_B, SimpleInvestmentStategyTest.STRATEGY_B);
        new SimpleInvestmentStrategy(0, Integer.MAX_VALUE, strategies);
    }

    @Test
    public void properlyRecommendingInvestmentSize() {
        final BigDecimal loanAmount = BigDecimal.valueOf(100000.0);
        final Loan mockLoan = Mockito.mock(Loan.class);
        Mockito.when(mockLoan.getRating()).thenReturn(SimpleInvestmentStategyTest.RATING_A);
        Mockito.when(mockLoan.getRemainingInvestment()).thenReturn(loanAmount.doubleValue());
        Mockito.when(mockLoan.getAmount()).thenReturn(loanAmount.doubleValue());

        // with unlimited balance
        final PortfolioOverview portfolio = Mockito.mock(PortfolioOverview.class);
        Mockito.when(portfolio.getCzkAvailable()).thenReturn(Integer.MAX_VALUE);
        Mockito.when(portfolio.getCzkInvested()).thenReturn(0);
        final int maxInvestment = Math.min(
                loanAmount.multiply(SimpleInvestmentStategyTest.MAXIMUM_LOAN_SHARE_A).intValue(),
                SimpleInvestmentStategyTest.MAXIMUM_LOAN_INVESTMENT);
        final int actualInvestment = overallStrategy.recommendInvestmentAmount(mockLoan, portfolio);
        Assertions.assertThat(actualInvestment).isLessThanOrEqualTo(maxInvestment);

        // with balance just a little less than the recommended investment
        Mockito.when(portfolio.getCzkAvailable()).thenReturn(actualInvestment - 1);
        final int adjustedForBalance = overallStrategy.recommendInvestmentAmount(mockLoan, portfolio);
        Assertions.assertThat(adjustedForBalance).isLessThanOrEqualTo(actualInvestment);

        // and make sure the recommendation is 200 times X, where X is a positive integer
        final BigDecimal remainder = BigDecimal.valueOf(adjustedForBalance)
                .remainder(BigDecimal.valueOf(Defaults.MINIMUM_INVESTMENT_IN_CZK));
        Assertions.assertThat(remainder.intValue()).isEqualTo(0);
    }

    private static Map<Rating, BigDecimal> prepareShareMap(final BigDecimal ratingA, final BigDecimal ratingB,
                                                           final BigDecimal ratingC) {
        final Map<Rating, BigDecimal> map = new EnumMap<>(Rating.class);
        Arrays.stream(Rating.values()).forEach(r -> map.put(r, BigDecimal.ZERO));
        map.put(Rating.A, ratingA);
        map.put(Rating.B, ratingB);
        map.put(Rating.C, ratingC);
        return Collections.unmodifiableMap(map);
    }

    private static void assertOrder(final List<Rating> result, final Rating... ratingsOrderedDown) {
        final Rating first = result.get(0);
        final Rating last = result.get(ratingsOrderedDown.length - 1);
        final SoftAssertions softly = new SoftAssertions();
        softly.assertThat(first).isGreaterThan(last);
        softly.assertThat(first).isEqualTo(ratingsOrderedDown[0]);
        softly.assertThat(last).isEqualTo(ratingsOrderedDown[ratingsOrderedDown.length - 1]);
        softly.assertAll();
    }

    private static void assertOrder(final List<Rating> result, final Rating r) {
        Assertions.assertThat(result.get(0)).isEqualTo(r);
    }

    private static Loan mockLoan(final int id, final double amount, final int term, final Rating rating) {
        final Loan loan = Mockito.mock(Loan.class);
        Mockito.when(loan.getRemainingInvestment()).thenReturn(amount * 2);
        Mockito.when(loan.getAmount()).thenReturn(amount);
        Mockito.when(loan.getId()).thenReturn(id);
        Mockito.when(loan.getTermInMonths()).thenReturn(term);
        Mockito.when(loan.getRating()).thenReturn(rating);
        Mockito.when(loan.getDatePublished()).thenReturn(OffsetDateTime.ofInstant(Instant.EPOCH, Defaults.ZONE_ID));
        return loan;
    }

    private static LoanDescriptor getLoanDescriptor(final Loan l) {
        return new LoanDescriptor(l, Duration.ofSeconds(100));
    }

    @Test
    public void testProperSorting() {
        final LoanDescriptor a1 =
                SimpleInvestmentStategyTest.getLoanDescriptor(SimpleInvestmentStategyTest.mockLoan(1, 2, 3, Rating.A));
        final LoanDescriptor b1 =
                SimpleInvestmentStategyTest.getLoanDescriptor(SimpleInvestmentStategyTest.mockLoan(4, 5, 6, Rating.B));
        final LoanDescriptor c1 =
                SimpleInvestmentStategyTest.getLoanDescriptor(SimpleInvestmentStategyTest.mockLoan(7, 8, 9, Rating.C));
        final LoanDescriptor a2 =
                SimpleInvestmentStategyTest.getLoanDescriptor(SimpleInvestmentStategyTest.mockLoan(10, 11, 12, Rating.A));
        final Collection<LoanDescriptor> loans = Arrays.asList(a1, b1, c1, a2, b1, c1);
        final Map<Rating, Collection<LoanDescriptor>> result = SimpleInvestmentStrategy.sortLoansByRating(loans);
        Assertions.assertThat(result).containsOnlyKeys(Rating.A, Rating.B, Rating.C);
        Assertions.assertThat(result.get(Rating.A)).containsExactly(a1, a2);
        Assertions.assertThat(result.get(Rating.B)).containsExactly(b1);
        Assertions.assertThat(result.get(Rating.C)).containsExactly(c1);
    }

    @Test
    public void properRankingOfRatings() {
        final BigDecimal targetShareA = BigDecimal.valueOf(0.001);
        final BigDecimal targetShareB = targetShareA.multiply(BigDecimal.TEN);
        final BigDecimal targetShareC = targetShareB.multiply(BigDecimal.TEN);

        final Map<Rating, StrategyPerRating> strategies =
                SimpleInvestmentStategyTest.mockStrategies(BigDecimal.ZERO);
        strategies.put(Rating.A, new StrategyPerRating(Rating.A, targetShareA, BigDecimal.ONE, 1, 1, 1, 1,
                BigDecimal.ZERO, BigDecimal.ONE, 1, 1, true, false));
        strategies.put(Rating.B, new StrategyPerRating(Rating.B, targetShareB, BigDecimal.ONE, 1, 1, 1, 1,
                BigDecimal.ZERO, BigDecimal.ONE, 1, 1, true, false));
        strategies.put(Rating.C, new StrategyPerRating(Rating.C, targetShareC, BigDecimal.ONE, 1, 1, 1, 1,
                BigDecimal.ZERO, BigDecimal.ONE, 1, 1, true, false));
        final SimpleInvestmentStrategy sis = new SimpleInvestmentStrategy(0, Integer.MAX_VALUE, strategies);

        // all ratings have zero share; C > B > A
        Map<Rating, BigDecimal> tmp = SimpleInvestmentStategyTest.prepareShareMap(BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO);
        SimpleInvestmentStategyTest.assertOrder(sis.rankRatingsByDemand(tmp), Rating.C, Rating.B, Rating.A);

        // A only; B, C overinvested
        tmp = SimpleInvestmentStategyTest.prepareShareMap(BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.TEN);
        SimpleInvestmentStategyTest.assertOrder(sis.rankRatingsByDemand(tmp), Rating.A);

        // B > C > A
        tmp = SimpleInvestmentStategyTest.prepareShareMap(BigDecimal.valueOf(0.00095), BigDecimal.ZERO,
                BigDecimal.valueOf(0.099));
        SimpleInvestmentStategyTest.assertOrder(sis.rankRatingsByDemand(tmp), Rating.B, Rating.C, Rating.A);
    }

    private static Map<Rating, StrategyPerRating> mockStrategies() {
        return SimpleInvestmentStategyTest.mockStrategies(BigDecimal.valueOf(0.1));
    }

    private static Map<Rating, StrategyPerRating> mockStrategies(final BigDecimal defaultTargetShare) {
        return Arrays.stream(Rating.values())
                .collect(Collectors.toMap(Function.identity(), r -> {
                    final StrategyPerRating s = Mockito.mock(StrategyPerRating.class);
                    Mockito.when(s.isLongerTermPreferred()).thenReturn(r.ordinal() % 2 == 0); // exercise both branches
                    Mockito.when(s.getRating()).thenReturn(r);
                    Mockito.when(s.getTargetShare()).thenReturn(defaultTargetShare);
                    Mockito.when(s.getMaximumShare()).thenReturn(BigDecimal.ONE);
                    Mockito.when(s.isAcceptable(ArgumentMatchers.any())).thenReturn(true);
                    Mockito.when(s.recommendInvestmentAmount(ArgumentMatchers.any()))
                            .thenReturn(Optional.of(new int[] {0, 100000}));
                    return s;
                }));
    }

    @Test
    public void doesNotAllowInvestingOverCeiling() {
        final int balance = 10000;
        final int ceiling = 100000;
        final SimpleInvestmentStrategy sis =
                new SimpleInvestmentStrategy(balance, ceiling, SimpleInvestmentStategyTest.mockStrategies());

        // all ratings have zero share; C > B > A
        final Map<Rating, BigDecimal> tmp =
                SimpleInvestmentStategyTest.prepareShareMap(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

        // prepare some loans
        final LoanDescriptor a1 = SimpleInvestmentStategyTest.getLoanDescriptor(SimpleInvestmentStategyTest.mockLoan
                (1, 200, 3, Rating.A));
        final LoanDescriptor b1 = SimpleInvestmentStategyTest.getLoanDescriptor(SimpleInvestmentStategyTest.mockLoan
                (4, 500, 6, Rating.B));
        final LoanDescriptor c1 = SimpleInvestmentStategyTest.getLoanDescriptor(SimpleInvestmentStategyTest.mockLoan
                (7, 800, 9, Rating.C));
        final LoanDescriptor a2 = SimpleInvestmentStategyTest.getLoanDescriptor(SimpleInvestmentStategyTest.mockLoan
                (10, 1100, 12, Rating.A));
        final List<LoanDescriptor> loans = Arrays.asList(a1, b1, c1, a2);

        // make sure we never go below the minimum balance
        final PortfolioOverview portfolio = Mockito.mock(PortfolioOverview.class);
        Mockito.when(portfolio.getSharesOnInvestment()).thenReturn(tmp);
        Mockito.when(portfolio.getCzkAvailable()).thenReturn(Integer.MAX_VALUE);
        Mockito.when(portfolio.getCzkInvested()).thenReturn(ceiling + 1);
        Assertions.assertThat(sis.recommend(loans, portfolio)).isEmpty();
        Mockito.when(portfolio.getCzkInvested()).thenReturn(ceiling);
        Assertions.assertThat(sis.recommend(loans, portfolio)).isNotEmpty();
        Mockito.when(portfolio.getCzkInvested()).thenReturn(ceiling - 1);
        Assertions.assertThat(sis.recommend(loans, portfolio)).isNotEmpty();
    }

    @Test
    public void properlySortsMatchesOnTerms() {
        final int balance = 10000;
        final int ceiling = 100000;
        final SimpleInvestmentStrategy sis =
                new SimpleInvestmentStrategy(balance, ceiling, SimpleInvestmentStategyTest.mockStrategies());

        // all ratings have zero share; C > B > A
        final Map<Rating, BigDecimal> tmp =
                SimpleInvestmentStategyTest.prepareShareMap(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

        // prepare some loans
        final int aTerm = 3, bTerm = 4;
        final LoanDescriptor a1 = SimpleInvestmentStategyTest.getLoanDescriptor(SimpleInvestmentStategyTest.mockLoan
                (1, 200, aTerm, Rating.A));
        final LoanDescriptor b1 = SimpleInvestmentStategyTest.getLoanDescriptor(SimpleInvestmentStategyTest.mockLoan
                (4, 500, bTerm, Rating.B));
        final LoanDescriptor b2 = SimpleInvestmentStategyTest.getLoanDescriptor(SimpleInvestmentStategyTest.mockLoan
                (7, 800, bTerm + 1, Rating.B));
        final LoanDescriptor a2 = SimpleInvestmentStategyTest.getLoanDescriptor(SimpleInvestmentStategyTest.mockLoan
                (10, 1100, aTerm + 1, Rating.A));
        final List<LoanDescriptor> loans = Arrays.asList(a1, b1, a2, b2);

        // prepare portfolio
        final PortfolioOverview portfolio = Mockito.mock(PortfolioOverview.class);
        Mockito.when(portfolio.getSharesOnInvestment()).thenReturn(tmp);
        Mockito.when(portfolio.getCzkAvailable()).thenReturn(Integer.MAX_VALUE);
        Mockito.when(portfolio.getCzkInvested()).thenReturn(0);

        // make sure the loans are properly sorted according to their terms
        final List<Recommendation> result = sis.recommend(loans, portfolio);
        Assertions.assertThat(result).hasSameSizeAs(loans);
        Assertions.assertThat(result.get(0).getLoanDescriptor()).isSameAs(a2);
        Assertions.assertThat(result.get(1).getLoanDescriptor()).isSameAs(a1);
        Assertions.assertThat(result.get(2).getLoanDescriptor()).isSameAs(b1);
        Assertions.assertThat(result.get(3).getLoanDescriptor()).isSameAs(b2);
    }

    @Test
    public void investingDoesNotGoOverBalance() { // https://github.com/triceo/robozonky/issues/62
        // prepare loan
        final int balance = 260;
        final int ceiling = 100000;
        final Rating rating = Rating.A;
        final Loan a1 = SimpleInvestmentStategyTest.mockLoan(1, 1000, 24, rating);
        // mock core strategies
        final Map<Rating, StrategyPerRating> s = SimpleInvestmentStategyTest.mockStrategies();
        final StrategyPerRating strategyMock = s.get(rating);
        Mockito.when(strategyMock.recommendInvestmentAmount(ArgumentMatchers.eq(a1)))
                .thenReturn(Optional.of(new int[] {200, 600}));
        final SimpleInvestmentStrategy sis = new SimpleInvestmentStrategy(balance, ceiling, s);
        // all ratings have zero share
        final Map<Rating, BigDecimal> tmp =
                SimpleInvestmentStategyTest.prepareShareMap(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        // prepare portfolio
        final PortfolioOverview portfolio = Mockito.mock(PortfolioOverview.class);
        Mockito.when(portfolio.getSharesOnInvestment()).thenReturn(tmp);
        Mockito.when(portfolio.getCzkAvailable()).thenReturn(balance);
        Mockito.when(portfolio.getCzkInvested()).thenReturn(0);

        // make sure the loans are properly sorted according to their terms
        final int result = sis.recommendInvestmentAmount(a1, portfolio);
        Assertions.assertThat(result).isEqualTo(200);
    }
}
