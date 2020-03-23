/*
 * Copyright 2020 The RoboZonky Project
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

import static com.github.robozonky.api.Ratio.fromPercentage;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.test.AbstractRoboZonkyTest;

class UtilTest extends AbstractRoboZonkyTest {

    private static PortfolioOverview preparePortfolio(final Number ratingA, final Number ratingB,
            final Number ratingC) {
        final PortfolioOverview portfolioOverview = mockPortfolioOverview();
        doReturn(fromPercentage(ratingA)).when(portfolioOverview)
            .getShareOnInvestment(eq(Rating.A));
        doReturn(fromPercentage(ratingB)).when(portfolioOverview)
            .getShareOnInvestment(eq(Rating.B));
        doReturn(fromPercentage(ratingC)).when(portfolioOverview)
            .getShareOnInvestment(eq(Rating.C));
        return portfolioOverview;
    }

    private static void assertOrder(final Set<Rating> result, final Rating... ratingsOrderedDown) {
        assertOrder(new ArrayList<>(result), ratingsOrderedDown);
    }

    private static void assertOrder(final List<Rating> result, final Rating... ratingsOrderedDown) {
        final Rating first = result.get(0);
        final Rating last = result.get(ratingsOrderedDown.length - 1);
        assertSoftly(softly -> {
            softly.assertThat(first)
                .isEqualTo(ratingsOrderedDown[0]);
            softly.assertThat(last)
                .isEqualTo(ratingsOrderedDown[ratingsOrderedDown.length - 1]);
        });
    }

    private static <T> Set<T> inOrder(T... items) {
        Set<T> result = new LinkedHashSet<>(items.length);
        result.addAll(asList(items));
        return result;
    }

    @Test
    void comparator() {
        Comparator<Rating> comparator = Util.getRatingByDemandComparator(inOrder(Rating.A, Rating.D, Rating.AAAAA));
        assertSoftly(softly -> softly.assertThat(Rating.D)
            .usingComparator(comparator)
            .isGreaterThan(Rating.A)
            .isLessThan(Rating.AAAAA)
            .isLessThan(Rating.AAAAAA) // Not included == greatest.
            .isLessThan(Rating.C));
    }

    @Test
    void properRankingOfRatings() {
        final int targetShareA = 1;
        final int targetShareB = targetShareA * 5;
        final int targetShareC = targetShareB * 5;
        final ParsedStrategy parsed = new ParsedStrategy(new DefaultValues(DefaultPortfolio.EMPTY),
                asList(
                        new PortfolioShare(Rating.A, fromPercentage(targetShareA)),
                        new PortfolioShare(Rating.B, fromPercentage(targetShareB)),
                        new PortfolioShare(Rating.C, fromPercentage(targetShareC))),
                Collections.emptyMap(), Collections.emptyMap());
        // all ratings have zero share; C > B > A
        final Set<Rating> ratings = EnumSet.of(Rating.A, Rating.B, Rating.C);
        PortfolioOverview portfolio = preparePortfolio(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        assertOrder(Util.rankRatingsByDemand(parsed, portfolio), Rating.C, Rating.B, Rating.A);

        // A only; B, C overinvested
        portfolio = preparePortfolio(BigDecimal.ZERO, BigDecimal.valueOf(10), BigDecimal.valueOf(30));
        assertOrder(Util.rankRatingsByDemand(parsed, portfolio), Rating.A);

        // B > A > C
        portfolio = preparePortfolio(BigDecimal.valueOf(0.99), BigDecimal.ZERO, BigDecimal.valueOf(24.9));
        assertOrder(Util.rankRatingsByDemand(parsed, portfolio), Rating.B, Rating.A, Rating.C);
    }

    @Test
    void acceptable() {
        final ParsedStrategy s = mock(ParsedStrategy.class);
        when(s.getMaximumInvestmentSize()).thenReturn(Money.from(1_000));
        final PortfolioOverview p = mockPortfolioOverview();
        when(p.getInvested()).thenReturn(Money.from(999));
        assertThat(Util.isAcceptable(s, p)).isTrue();
    }

    @Test
    void unacceptableDueToCeiling() {
        final ParsedStrategy s = mock(ParsedStrategy.class);
        when(s.getMaximumInvestmentSize()).thenReturn(Money.from(10_000));
        final PortfolioOverview p = mockPortfolioOverview();
        when(p.getInvested()).thenReturn(Money.from(10_000));
        assertThat(Util.isAcceptable(s, p)).isFalse();
    }

}
