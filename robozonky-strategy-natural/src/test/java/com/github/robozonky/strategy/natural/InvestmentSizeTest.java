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

import com.github.robozonky.api.remote.enums.Rating;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;

public class InvestmentSizeTest {

    private static final int MIN = 400, MAX = 2 * InvestmentSizeTest.MIN;

    @Test
    public void regular() {
        final InvestmentSize s = new InvestmentSize(Rating.A, InvestmentSizeTest.MIN, InvestmentSizeTest.MAX);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(s.getMinimumInvestmentInCzk()).isEqualTo(InvestmentSizeTest.MIN);
            softly.assertThat(s.getMaximumInvestmentInCzk()).isEqualTo(InvestmentSizeTest.MAX);
        });
    }

    @Test
    public void switched() {
        final InvestmentSize s = new InvestmentSize(Rating.A, InvestmentSizeTest.MAX, InvestmentSizeTest.MIN);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(s.getMinimumInvestmentInCzk()).isEqualTo(InvestmentSizeTest.MIN);
            softly.assertThat(s.getMaximumInvestmentInCzk()).isEqualTo(InvestmentSizeTest.MAX);
        });
    }

    @Test
    public void omitted() {
        final InvestmentSize s = new InvestmentSize(Rating.A, InvestmentSizeTest.MAX);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(s.getMinimumInvestmentInCzk()).isEqualTo(0);
            softly.assertThat(s.getMaximumInvestmentInCzk()).isEqualTo(InvestmentSizeTest.MAX);
        });
    }
}
