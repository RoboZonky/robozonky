/*
 * Copyright 2019 The RoboZonky Project
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

import com.github.robozonky.api.Ratio;
import com.github.robozonky.api.remote.enums.Rating;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

class PortfolioShareTest {

    @Test
    void leftBoundWrong() {
        assertSoftly(softly -> {
            softly.assertThatThrownBy(() -> new PortfolioShare(Rating.A, Ratio.fromPercentage(-1), Ratio.ZERO))
                    .isInstanceOf(IllegalArgumentException.class);
            softly.assertThatThrownBy(() -> new PortfolioShare(Rating.A, Ratio.ZERO, Ratio.fromPercentage(-1)))
                    .isInstanceOf(IllegalArgumentException.class);
        });
    }

    @Test
    void rightBoundWrong() {
        assertSoftly(softly -> {
            softly.assertThatThrownBy(() -> new PortfolioShare(Rating.B, Ratio.fromPercentage(101), Ratio.ZERO))
                    .isInstanceOf(IllegalArgumentException.class);
            softly.assertThatThrownBy(() -> new PortfolioShare(Rating.B, Ratio.ZERO, Ratio.fromPercentage(101)))
                    .isInstanceOf(IllegalArgumentException.class);
        });
    }

    @Test
    void correct() {
        final PortfolioShare p = new PortfolioShare(Rating.C, Ratio.ZERO, Ratio.ONE);
        assertSoftly(softly -> {
            softly.assertThat(p.getPermitted()).isEqualTo(Ratio.ONE);
            softly.assertThat(p.getRating()).isEqualTo(Rating.C);
        });
    }

    @Test
    void reversed() {
        final PortfolioShare p = new PortfolioShare(Rating.D, Ratio.fromPercentage(51), Ratio.fromPercentage(50));
        assertSoftly(softly -> {
            softly.assertThat(p.getPermitted()).isEqualTo(Ratio.fromPercentage(51));
            softly.assertThat(p.getRating()).isEqualTo(Rating.D);
        });
    }
}
