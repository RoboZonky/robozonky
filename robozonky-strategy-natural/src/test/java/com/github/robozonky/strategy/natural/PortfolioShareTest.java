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

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import org.junit.jupiter.api.Test;

import com.github.robozonky.api.Ratio;
import com.github.robozonky.api.remote.enums.Rating;

class PortfolioShareTest {

    @Test
    void rightBoundWrong() {
        assertThatThrownBy(() -> new PortfolioShare(Rating.B.getInterestRate(), Ratio.fromPercentage(101)))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PortfolioShare(Rating.B.getInterestRate(), Ratio.fromPercentage(-1)))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void correct1() {
        final PortfolioShare p = new PortfolioShare(Rating.C.getInterestRate(), Ratio.ONE);
        assertSoftly(softly -> {
            softly.assertThat(p.getPermitted())
                .isEqualTo(Ratio.ONE);
            softly.assertThat(p.getInterestRate())
                .isEqualTo(Rating.C.getInterestRate());
        });
    }

    @Test
    void correct2() {
        final PortfolioShare p = new PortfolioShare(Rating.C.getInterestRate(), Ratio.ZERO);
        assertSoftly(softly -> {
            softly.assertThat(p.getPermitted())
                .isEqualTo(Ratio.ZERO);
            softly.assertThat(p.getInterestRate())
                .isEqualTo(Rating.C.getInterestRate());
        });
    }

}
