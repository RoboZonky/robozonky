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
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.github.triceo.robozonky.authentication.Authentication;
import com.github.triceo.robozonky.authentication.Authenticator;
import com.github.triceo.robozonky.exceptions.LoginFailedException;
import com.github.triceo.robozonky.exceptions.LogoutFailedException;
import com.github.triceo.robozonky.remote.Investment;
import com.github.triceo.robozonky.remote.Rating;
import com.github.triceo.robozonky.remote.RiskPortfolio;
import com.github.triceo.robozonky.remote.Statistics;
import com.github.triceo.robozonky.remote.Wallet;
import com.github.triceo.robozonky.remote.ZonkyApi;
import com.github.triceo.robozonky.remote.ZonkyApiToken;
import com.github.triceo.robozonky.strategy.InvestmentStrategy;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.Mockito;

public class OperationsTest {

    private static Map<Rating, BigDecimal> prepareShareMap(final BigDecimal ratingA, final BigDecimal ratingB,
                                                           final BigDecimal ratingC) {
        final Map<Rating, BigDecimal> map = new EnumMap<>(Rating.class);
        map.put(Rating.A, ratingA);
        map.put(Rating.B, ratingB);
        map.put(Rating.C, ratingC);
        return Collections.unmodifiableMap(map);
    }

    private static void assertOrder(final List<Rating> result, final Rating... ratingsOrderedDown) {
        Assertions.assertThat(result).hasSize(ratingsOrderedDown.length);
        if (ratingsOrderedDown.length < 2) {
            return;
        } else if (ratingsOrderedDown.length > 3) {
            throw new IllegalStateException("This should never happen in the test.");
        }
        final Rating first = result.get(0);
        final Rating last = result.get(result.size() - 1);
        Assertions.assertThat(first).isGreaterThan(last);
        Assertions.assertThat(first).isEqualTo(ratingsOrderedDown[0]);
        Assertions.assertThat(last).isEqualTo(ratingsOrderedDown[ratingsOrderedDown.length - 1]);
    }

    @Test
    public void properRankingOfRatings() {
        final BigDecimal targetShareA = BigDecimal.valueOf(0.001);
        final BigDecimal targetShareB = targetShareA.multiply(BigDecimal.TEN);
        final BigDecimal targetShareC = targetShareB.multiply(BigDecimal.TEN);

        // prepare the mocks of strategy and context
        final InvestmentStrategy strategy = Mockito.mock(InvestmentStrategy.class);
        Mockito.when(strategy.getTargetShare(Rating.A)).thenReturn(targetShareA);
        Mockito.when(strategy.getTargetShare(Rating.B)).thenReturn(targetShareB);
        Mockito.when(strategy.getTargetShare(Rating.C)).thenReturn(targetShareC);
        final OperationsContext ctx = Mockito.mock(OperationsContext.class);
        Mockito.when(ctx.getStrategy()).thenReturn(strategy);

        // all ratings have zero share; C > B > A
        Map<Rating, BigDecimal> tmp = OperationsTest.prepareShareMap(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        OperationsTest.assertOrder(Operations.rankRatingsByDemand(ctx, tmp), Rating.C, Rating.B, Rating.A);

        // A only; B, C overinvested
        tmp = OperationsTest.prepareShareMap(BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.TEN);
        OperationsTest.assertOrder(Operations.rankRatingsByDemand(ctx, tmp), Rating.A);

        // B > C > A
        tmp = OperationsTest.prepareShareMap(BigDecimal.valueOf(0.00095), BigDecimal.ZERO, BigDecimal.valueOf(0.099));
        OperationsTest.assertOrder(Operations.rankRatingsByDemand(ctx, tmp), Rating.B, Rating.C, Rating.A);
    }

    private static List<Investment> getMockInvestmentWithBalance(final int loanAmount) {
        final Investment i = Mockito.mock(Investment.class);
        Mockito.when(i.getAmount()).thenReturn(loanAmount);
        return Collections.singletonList(i);
    }

    @Test
    public void properBalanceRetrievalInDryRun() {
        // prepare context
        final BigDecimal dryRunBalance = BigDecimal.valueOf(12345);
        final OperationsContext ctx = Mockito.mock(OperationsContext.class);
        Mockito.when(ctx.isDryRun()).thenReturn(true);
        Mockito.when(ctx.getDryRunInitialBalance()).thenReturn(dryRunBalance);
        // test operation
        Assertions.assertThat(Operations.getAvailableBalance(ctx, Collections.emptyList())).isEqualTo(dryRunBalance);
        final int amount = 1;
        final BigDecimal newBalance = dryRunBalance.subtract(BigDecimal.valueOf(amount));
        Assertions.assertThat(Operations.getAvailableBalance(ctx, OperationsTest.getMockInvestmentWithBalance(amount)))
                .isEqualTo(newBalance);
    }

    @Test
    public void properBalanceRetrievalInNormalMode() {
        // prepare context
        final BigDecimal remoteBalance = BigDecimal.valueOf(12345);
        final Wallet wallet = new Wallet(-1, -1, BigDecimal.valueOf(100000), remoteBalance);
        final ZonkyApi api = Mockito.mock(ZonkyApi.class);
        Mockito.when(api.getWallet()).thenReturn(wallet);
        final OperationsContext ctx = Mockito.mock(OperationsContext.class);
        Mockito.when(ctx.getApi()).thenReturn(api);
        // test operation
        Assertions.assertThat(Operations.getAvailableBalance(ctx, Collections.emptyList())).isEqualTo(remoteBalance);
    }

    private static void assertProperRatingShare(final Map<Rating, BigDecimal> result, final Rating r, final int amount,
                                                final int total) {
        final BigDecimal expectedShare
                = BigDecimal.valueOf(amount).divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_EVEN);
        Assertions.assertThat(result.get(r)).isEqualTo(expectedShare);
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
        Map<Rating, BigDecimal> result = Operations.calculateSharesPerRating(stats, Collections.emptyList());
        OperationsTest.assertProperRatingShare(result, Rating.AA, amountAA, totalPie);
        OperationsTest.assertProperRatingShare(result, Rating.B, amountB, totalPie);
        OperationsTest.assertProperRatingShare(result, Rating.D, amountD, totalPie);
        OperationsTest.assertProperRatingShare(result, Rating.AAAAA, 0, totalPie); // test other ratings included
        OperationsTest.assertProperRatingShare(result, Rating.AAAA, 0, totalPie);
        OperationsTest.assertProperRatingShare(result, Rating.AAA, 0, totalPie);
        OperationsTest.assertProperRatingShare(result, Rating.A, 0, totalPie);
        OperationsTest.assertProperRatingShare(result, Rating.C, 0, totalPie);
        // check operation with offline investments
        final int increment = 200, newTotalPie = totalPie + increment;
        final List<Investment> investments = OperationsTest.getMockInvestmentWithBalance(increment);
        final Investment i = investments.get(0);
        Mockito.when(i.getRating()).thenReturn(Rating.D);
        result = Operations.calculateSharesPerRating(stats, investments);
        OperationsTest.assertProperRatingShare(result, Rating.AA, amountAA, newTotalPie);
        OperationsTest.assertProperRatingShare(result, Rating.B, amountB, newTotalPie);
        OperationsTest.assertProperRatingShare(result, Rating.D, amountD + increment, newTotalPie);
        OperationsTest.assertProperRatingShare(result, Rating.AAAAA, 0, newTotalPie); // test other ratings included
        OperationsTest.assertProperRatingShare(result, Rating.AAAA, 0, newTotalPie);
        OperationsTest.assertProperRatingShare(result, Rating.AAA, 0, newTotalPie);
        OperationsTest.assertProperRatingShare(result, Rating.A, 0, newTotalPie);
        OperationsTest.assertProperRatingShare(result, Rating.C, 0, newTotalPie);
    }

    @Test
    public void properLogin() {
        final Authentication tmp = Mockito.mock(Authentication.class);
        Mockito.when(tmp.getApi()).thenReturn(Mockito.mock(ZonkyApi.class));
        Mockito.when(tmp.getApiToken()).thenReturn(Mockito.mock(ZonkyApiToken.class));
        final Authenticator auth = Mockito.mock(Authenticator.class);
        Mockito.when(auth.authenticate(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(tmp);
        try {
            final Operations.LoginResult c = Operations.login(auth, true, 1000);
            Assertions.assertThat(c.getZonkyApiToken()).isSameAs(tmp.getApiToken());
            Assertions.assertThat(c.getOperationsContext()).isNotNull();
            Assertions.assertThat(c.getOperationsContext().getApi()).isSameAs(tmp.getApi());
            Operations.logout(c.getOperationsContext());
            Mockito.verify(tmp.getApi(), Mockito.times(1)).logout();
        } catch (final LoginFailedException | LogoutFailedException e) {
            Assertions.fail("Should not have happened.", e);
        }
    }

    @Test(expected = LoginFailedException.class)
    public void failedLogin() throws LoginFailedException {
        final Authenticator auth = Mockito.mock(Authenticator.class);
        Mockito.when(auth.authenticate(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenThrow(new IllegalStateException("Something bad happened."));
        Operations.login(auth, true, 1000);
    }

}
