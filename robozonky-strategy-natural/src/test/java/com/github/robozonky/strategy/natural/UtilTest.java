/*
 * Copyright 2017 The RoboZonky Project
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

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.PortfolioOverview;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public class UtilTest {

    private static PortfolioOverview prepareShareMap(final BigDecimal ratingA, final BigDecimal ratingB,
                                                     final BigDecimal ratingC) {
        final Map<Rating, BigDecimal> map = new EnumMap<>(Rating.class);
        Arrays.stream(Rating.values()).forEach(r -> map.put(r, BigDecimal.ZERO));
        map.put(Rating.A, ratingA);
        map.put(Rating.B, ratingB);
        map.put(Rating.C, ratingC);
        final PortfolioOverview portfolio = Mockito.mock(PortfolioOverview.class);
        Mockito.when(portfolio.getSharesOnInvestment()).thenReturn(map);
        map.forEach((key, value) ->
                            Mockito.when(portfolio.getShareOnInvestment(ArgumentMatchers.eq(key))).thenReturn(value));
        return portfolio;
    }

    private static void assertOrder(final Stream<Rating> result, final Rating... ratingsOrderedDown) {
        assertOrder(result.collect(Collectors.toList()), ratingsOrderedDown);
    }

    private static void assertOrder(final List<Rating> result, final Rating... ratingsOrderedDown) {
        final Rating first = result.get(0);
        final Rating last = result.get(ratingsOrderedDown.length - 1);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(first).isGreaterThan(last);
            softly.assertThat(first).isEqualTo(ratingsOrderedDown[0]);
            softly.assertThat(last).isEqualTo(ratingsOrderedDown[ratingsOrderedDown.length - 1]);
        });
    }

    private static void assertOrder(final Stream<Rating> result, final Rating r) {
        assertOrder(result.collect(Collectors.toList()), r);
    }

    private static void assertOrder(final List<Rating> result, final Rating r) {
        Assertions.assertThat(result.get(0)).isEqualTo(r);
    }

    @Test
    public void properRankingOfRatings() {
        final int targetShareA = 1;
        final int targetShareB = targetShareA * 5;
        final int targetShareC = targetShareB * 5;

        final ParsedStrategy parsedStrategy = new ParsedStrategy(new DefaultValues(DefaultPortfolio.EMPTY),
                                                                 Arrays.asList(
                                                                         new PortfolioShare(Rating.A, targetShareA,
                                                                                            targetShareA),
                                                                         new PortfolioShare(Rating.B, targetShareB,
                                                                                            targetShareB),
                                                                         new PortfolioShare(Rating.C, targetShareC,
                                                                                            targetShareC)),
                                                                 Collections.emptyMap(), Collections.emptyList(),
                                                                 Collections.emptyList(), Collections.emptyList());
        // all ratings have zero share; C > B > A
        PortfolioOverview portfolio = prepareShareMap(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        assertOrder(Util.rankRatingsByDemand(parsedStrategy, portfolio.getSharesOnInvestment()), Rating.C, Rating.B,
                    Rating.A);

        // A only; B, C overinvested
        portfolio = prepareShareMap(BigDecimal.ZERO, BigDecimal.valueOf(10), BigDecimal.valueOf(30));
        assertOrder(Util.rankRatingsByDemand(parsedStrategy, portfolio.getSharesOnInvestment()), Rating.A);

        // B > C > A
        portfolio = prepareShareMap(BigDecimal.valueOf(0.0099), BigDecimal.ZERO, BigDecimal.valueOf(0.249));
        assertOrder(Util.rankRatingsByDemand(parsedStrategy, portfolio.getSharesOnInvestment()), Rating.B, Rating.C,
                    Rating.A);
    }
}
