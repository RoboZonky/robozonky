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

package com.github.robozonky.api.remote.entities;

import static org.assertj.core.api.Assertions.*;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.Ratio;
import com.github.robozonky.api.remote.enums.Rating;

class RatingTest {

    @Test
    void noRatingCodeTwice() {
        final Set<String> codes = Stream.of(Rating.values())
            .map(Rating::getCode)
            .collect(Collectors.toSet());
        assertThat(codes).hasSize(Rating.values().length);
    }

    @Test
    void maxFeeDiscount() {
        final Ratio revenue = Rating.D.getMaximalRevenueRate(Money.from(Long.MAX_VALUE));
        assertThat(revenue.doubleValue()).isEqualTo(0.1599, Offset.offset(0.0001));
    }

    @Test
    void minFeeDiscount() {
        final Ratio revenue = Rating.AAAAAA.getMinimalRevenueRate();
        assertThat(revenue.doubleValue()).isEqualTo(0.0234, Offset.offset(0.0001));
    }

}
