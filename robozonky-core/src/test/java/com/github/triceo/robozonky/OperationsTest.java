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
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.github.triceo.robozonky.remote.Investment;
import com.github.triceo.robozonky.remote.Rating;
import com.github.triceo.robozonky.strategy.InvestmentStrategy;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.Mockito;

public class OperationsTest {

    private static Investment getMockInvestmentWithId(final int id) {
        final Investment i = Mockito.mock(Investment.class);
        Mockito.when(i.getLoanId()).thenReturn(id);
        return i;
    }

    @Test
    public void mergingTwoInvestmentCollectinsWorksProperly() {
        final Investment I1 = OperationsTest.getMockInvestmentWithId(1);
        final Investment I2 = OperationsTest.getMockInvestmentWithId(2);
        final Investment I3 = OperationsTest.getMockInvestmentWithId(3);

        // two identical investments will result in one
        final List<Investment> a = Arrays.asList(I1, I2);
        final List<Investment> b = Arrays.asList(I2, I3);
        Assertions.assertThat(Operations.mergeInvestments(a, b)).containsExactly(I1, I2, I3);

        // standard merging also works
        final List<Investment> c = Collections.singletonList(I3);
        Assertions.assertThat(Operations.mergeInvestments(a, c)).containsExactly(I1, I2, I3);

        // reverse-order merging works
        final List<Investment> d = Arrays.asList(I2, I1);
        Assertions.assertThat(Operations.mergeInvestments(a, d)).containsExactly(I1, I2);

        // two non-identical loans with same ID are merged in the order in which they came
        final Investment I3_2 = OperationsTest.getMockInvestmentWithId(3);
        final List<Investment> e = Collections.singletonList(I3_2);
        Assertions.assertThat(Operations.mergeInvestments(c, e)).containsExactly(I3);
        Assertions.assertThat(Operations.mergeInvestments(e, c)).containsExactly(I3_2);
    }

    private static Map<Rating, BigDecimal> prepareShareMap(final BigDecimal ratingA, final BigDecimal ratingB,
                                                           final BigDecimal ratingC) {
        final EnumMap<Rating, BigDecimal> map = new EnumMap<>(Rating.class);
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
    public void testProperRankingOfRatings() {
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

}
