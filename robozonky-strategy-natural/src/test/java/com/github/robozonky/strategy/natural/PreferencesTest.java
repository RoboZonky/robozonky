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
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.test.AbstractRoboZonkyTest;

class PreferencesTest extends AbstractRoboZonkyTest {

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

    @Test
    void acceptsRatingsProperly() {
        final int targetShareA = 1;
        final int targetShareB = targetShareA * 5;
        final int targetShareC = targetShareB * 5;
        final ParsedStrategy parsed = new ParsedStrategy(new DefaultValues(DefaultPortfolio.EMPTY),
                asList(
                        new PortfolioShare(Rating.A.getInterestRate(), fromPercentage(targetShareA)),
                        new PortfolioShare(Rating.B.getInterestRate(), fromPercentage(targetShareB)),
                        new PortfolioShare(Rating.C.getInterestRate(), fromPercentage(targetShareC))),
                Collections.emptyMap(), Collections.emptyMap());
        // all ratings have zero share; C > B > A
        final PortfolioOverview portfolio = preparePortfolio(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        final Preferences preferences = Preferences.get(parsed, portfolio);
        assertSoftly(softly -> {
            softly.assertThat(preferences.isDesirable(Rating.D.getInterestRate()))
                .isFalse();
            softly.assertThat(preferences.isDesirable(Rating.C.getInterestRate()))
                .isTrue();
            softly.assertThat(preferences.isDesirable(Rating.B.getInterestRate()))
                .isTrue();
            softly.assertThat(preferences.isDesirable(Rating.A.getInterestRate()))
                .isTrue();
            softly.assertThat(preferences.isDesirable(Rating.AE.getInterestRate()))
                .isFalse();
            softly.assertThat(preferences.isDesirable(Rating.AA.getInterestRate()))
                .isFalse();
            softly.assertThat(preferences.isDesirable(Rating.AAA.getInterestRate()))
                .isFalse();
            softly.assertThat(preferences.isDesirable(Rating.AAAA.getInterestRate()))
                .isFalse();
            softly.assertThat(preferences.isDesirable(Rating.AAAAA.getInterestRate()))
                .isFalse();
            softly.assertThat(preferences.isDesirable(Rating.AAAAAA.getInterestRate()))
                .isFalse();
        });
    }

    @Test
    void acceptsRatingsProperly2() {
        final int targetShareA = 1;
        final int targetShareB = targetShareA * 5;
        final int targetShareC = targetShareB * 5;
        final ParsedStrategy parsed = new ParsedStrategy(new DefaultValues(DefaultPortfolio.EMPTY),
                asList(
                        new PortfolioShare(Rating.A.getInterestRate(), fromPercentage(targetShareA)),
                        new PortfolioShare(Rating.B.getInterestRate(), fromPercentage(targetShareB)),
                        new PortfolioShare(Rating.C.getInterestRate(), fromPercentage(targetShareC))),
                Collections.emptyMap(), Collections.emptyMap());
        // A only; B, C overinvested
        final PortfolioOverview portfolio = preparePortfolio(BigDecimal.ZERO, BigDecimal.valueOf(10),
                BigDecimal.valueOf(30));
        final Preferences preferences = Preferences.get(parsed, portfolio);
        assertSoftly(softly -> {
            softly.assertThat(preferences.isDesirable(Rating.D.getInterestRate()))
                .isFalse();
            softly.assertThat(preferences.isDesirable(Rating.C.getInterestRate()))
                .isFalse();
            softly.assertThat(preferences.isDesirable(Rating.B.getInterestRate()))
                .isFalse();
            softly.assertThat(preferences.isDesirable(Rating.A.getInterestRate()))
                .isTrue();
            softly.assertThat(preferences.isDesirable(Rating.AE.getInterestRate()))
                .isFalse();
            softly.assertThat(preferences.isDesirable(Rating.AA.getInterestRate()))
                .isFalse();
            softly.assertThat(preferences.isDesirable(Rating.AAA.getInterestRate()))
                .isFalse();
            softly.assertThat(preferences.isDesirable(Rating.AAAA.getInterestRate()))
                .isFalse();
            softly.assertThat(preferences.isDesirable(Rating.AAAAA.getInterestRate()))
                .isFalse();
            softly.assertThat(preferences.isDesirable(Rating.AAAAAA.getInterestRate()))
                .isFalse();
        });

    }

    @Test
    void acceptsRatingsProperly3() {
        final int targetShareA = 1;
        final int targetShareB = targetShareA * 5;
        final int targetShareC = targetShareB * 5;
        final ParsedStrategy parsed = new ParsedStrategy(new DefaultValues(DefaultPortfolio.EMPTY),
                asList(
                        new PortfolioShare(Rating.A.getInterestRate(), fromPercentage(targetShareA)),
                        new PortfolioShare(Rating.B.getInterestRate(), fromPercentage(targetShareB)),
                        new PortfolioShare(Rating.C.getInterestRate(), fromPercentage(targetShareC))),
                Collections.emptyMap(), Collections.emptyMap());
        // B > A > C
        final PortfolioOverview portfolio = preparePortfolio(BigDecimal.valueOf(0.99), BigDecimal.ZERO,
                BigDecimal.valueOf(24.9));
        final Preferences preferences = Preferences.get(parsed, portfolio);
        assertSoftly(softly -> {
            softly.assertThat(preferences.isDesirable(Rating.D.getInterestRate()))
                .isFalse();
            softly.assertThat(preferences.isDesirable(Rating.C.getInterestRate()))
                .isTrue();
            softly.assertThat(preferences.isDesirable(Rating.B.getInterestRate()))
                .isTrue();
            softly.assertThat(preferences.isDesirable(Rating.A.getInterestRate()))
                .isTrue();
            softly.assertThat(preferences.isDesirable(Rating.AE.getInterestRate()))
                .isFalse();
            softly.assertThat(preferences.isDesirable(Rating.AA.getInterestRate()))
                .isFalse();
            softly.assertThat(preferences.isDesirable(Rating.AAA.getInterestRate()))
                .isFalse();
            softly.assertThat(preferences.isDesirable(Rating.AAAA.getInterestRate()))
                .isFalse();
            softly.assertThat(preferences.isDesirable(Rating.AAAAA.getInterestRate()))
                .isFalse();
            softly.assertThat(preferences.isDesirable(Rating.AAAAAA.getInterestRate()))
                .isFalse();
        });

    }

}
