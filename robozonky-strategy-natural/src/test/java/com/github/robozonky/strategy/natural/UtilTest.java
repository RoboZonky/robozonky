/*
 * Copyright 2018 The RoboZonky Project
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
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.test.AbstractRoboZonkyTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class UtilTest extends AbstractRoboZonkyTest {

    private static PortfolioOverview preparePortfolio(final BigDecimal ratingA, final BigDecimal ratingB,
                                                      final BigDecimal ratingC) {
        final PortfolioOverview portfolioOverview = mockPortfolioOverview(10_000);
        when(portfolioOverview.getShareOnInvestment(eq(Rating.A))).thenReturn(ratingA);
        when(portfolioOverview.getShareOnInvestment(eq(Rating.B))).thenReturn(ratingB);
        when(portfolioOverview.getShareOnInvestment(eq(Rating.C))).thenReturn(ratingC);
        return portfolioOverview;
    }

    private static void assertOrder(final Stream<Rating> result, final Rating... ratingsOrderedDown) {
        assertOrder(result.collect(Collectors.toList()), ratingsOrderedDown);
    }

    private static void assertOrder(final List<Rating> result, final Rating... ratingsOrderedDown) {
        final Rating first = result.get(0);
        final Rating last = result.get(ratingsOrderedDown.length - 1);
        assertSoftly(softly -> {
            softly.assertThat(first).isGreaterThan(last);
            softly.assertThat(first).isEqualTo(ratingsOrderedDown[0]);
            softly.assertThat(last).isEqualTo(ratingsOrderedDown[ratingsOrderedDown.length - 1]);
        });
    }

    private static void assertOrder(final Stream<Rating> result, final Rating r) {
        assertOrder(result.collect(Collectors.toList()), r);
    }

    private static void assertOrder(final List<Rating> result, final Rating r) {
        assertThat(result.get(0)).isEqualTo(r);
    }

    @Test
    void properRankingOfRatings() {
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
                                                                 Collections.emptyMap());
        // all ratings have zero share; C > B > A
        final Set<Rating> ratings = EnumSet.of(Rating.A, Rating.B, Rating.C);
        PortfolioOverview portfolio = preparePortfolio(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        assertOrder(Util.rankRatingsByDemand(parsedStrategy, ratings, portfolio), Rating.C, Rating.B, Rating.A);

        // A only; B, C overinvested
        portfolio = preparePortfolio(BigDecimal.ZERO, BigDecimal.valueOf(10), BigDecimal.valueOf(30));
        assertOrder(Util.rankRatingsByDemand(parsedStrategy, ratings, portfolio), Rating.A);

        // B > C > A
        portfolio = preparePortfolio(BigDecimal.valueOf(0.0099), BigDecimal.ZERO, BigDecimal.valueOf(0.249));
        assertOrder(Util.rankRatingsByDemand(parsedStrategy, ratings, portfolio), Rating.B, Rating.C, Rating.A);
    }
}
