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

package com.github.robozonky.api.remote.enums;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.Ratio;

class RatingTest {

    @Test
    void someRatingsUnavailableBefore2019() {
        var ratingsChange = Rating.MIDNIGHT_2019_03_18.minusSeconds(1);
        assertSoftly(softly -> {
            for (var rating : new Rating[] { Rating.AAE, Rating.AE }) {
                softly.assertThat(rating.getMaximalRevenueRate(ratingsChange))
                    .as("Max revenue rate for " + rating)
                    .isEqualTo(Ratio.ZERO);
                softly.assertThat(rating.getMinimalRevenueRate(ratingsChange))
                    .as("Min revenue rate for " + rating)
                    .isEqualTo(Ratio.ZERO);
                softly.assertThat(rating.getFee(ratingsChange))
                    .as("Fee for " + rating)
                    .isEqualTo(Ratio.ZERO);
            }
        });
    }

    @Test
    void feesBefore2018() {
        var ratingsChange = Rating.MIDNIGHT_2017_09_01.minusSeconds(1);
        assertSoftly(softly -> {
            for (var rating : new Rating[] { Rating.AAAAA, Rating.AAAA, Rating.AAA, Rating.AA, Rating.A, Rating.B,
                    Rating.C, Rating.D }) {
                softly.assertThat(rating.getFee(ratingsChange))
                    .as("Fee for " + rating)
                    .isEqualTo(Ratio.fromPercentage(1));
            }
        });
    }

    @Test
    void allRatingsNowAvailable() {
        assertSoftly(softly -> {
            for (var rating : Rating.values()) {
                softly.assertThat(rating.getMaximalRevenueRate())
                    .as("Max revenue rate for " + rating)
                    .isGreaterThan(Ratio.ZERO);
                softly.assertThat(rating.getMinimalRevenueRate())
                    .as("Min revenue rate for " + rating)
                    .isGreaterThan(Ratio.ZERO);
                softly.assertThat(rating.getFee())
                    .as("Fee for " + rating)
                    .isGreaterThan(Ratio.ZERO);
                softly.assertThat(rating.getInterestRate())
                    .as("Interest rate for " + rating)
                    .isGreaterThan(Ratio.ZERO);
            }
        });
    }

    @Test
    void feesDecreasing() {
        assertSoftly(softly -> {
            for (var rating : Rating.values()) {
                var fee0 = rating.getFee(Money.from(0));
                var fee1 = rating.getFee(Money.from(150_000));
                var fee2 = rating.getFee(Money.from(200_000));
                var fee3 = rating.getFee(Money.from(500_000));
                var fee4 = rating.getFee(Money.from(1_000_000));
                softly.assertThat(fee1)
                    .as("Fee for " + rating + " at 150 000")
                    .isLessThan(fee0);
                softly.assertThat(fee2)
                    .as("Fee for " + rating + " at 200 000")
                    .isLessThan(fee1);
                softly.assertThat(fee3)
                    .as("Fee for " + rating + " at 500 000")
                    .isLessThan(fee2);
                softly.assertThat(fee4)
                    .as("Fee for " + rating + " at 1 000 000")
                    .isLessThan(fee3);
            }
        });
    }

    @Test
    void noRatingCodeTwice() {
        var codes = Stream.of(Rating.values())
            .map(Rating::getCode)
            .collect(Collectors.toSet());
        assertThat(codes).hasSize(Rating.values().length);
    }

    @Test
    void maxFeeDiscount() {
        var revenue = Rating.D.getMaximalRevenueRate(Money.from(Long.MAX_VALUE));
        assertThat(revenue.doubleValue()).isEqualTo(0.1599, Offset.offset(0.0001));
    }

    @Test
    void minFeeDiscount() {
        var revenue = Rating.AAAAAA.getMinimalRevenueRate();
        assertThat(revenue.doubleValue()).isEqualTo(0.0234, Offset.offset(0.0001));
    }

}
