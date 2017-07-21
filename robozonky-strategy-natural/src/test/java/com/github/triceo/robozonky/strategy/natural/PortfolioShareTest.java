/*
 * Copyright 2017 Lukáš Petrovický
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

package com.github.triceo.robozonky.strategy.natural;

import com.github.triceo.robozonky.api.remote.enums.Rating;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;

public class PortfolioShareTest {

    @Test
    public void leftBoundWrong() {
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThatThrownBy(() -> new PortfolioShare(Rating.A, -1, 0))
                    .isInstanceOf(IllegalArgumentException.class);
            softly.assertThatThrownBy(() -> new PortfolioShare(Rating.A, 0, -1))
                    .isInstanceOf(IllegalArgumentException.class);
        });
    }

    @Test
    public void rightBoundWrong() {
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThatThrownBy(() -> new PortfolioShare(Rating.B, 101, 0))
                    .isInstanceOf(IllegalArgumentException.class);
            softly.assertThatThrownBy(() -> new PortfolioShare(Rating.B, 0, 101))
                    .isInstanceOf(IllegalArgumentException.class);
        });
    }

    @Test
    public void correct() {
        final PortfolioShare p = new PortfolioShare(Rating.C, 0, 100);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(p.getMininumShareInPercent()).isEqualTo(0);
            softly.assertThat(p.getMaximumShareInPercent()).isEqualTo(100);
            softly.assertThat(p.getRating()).isEqualTo(Rating.C);
        });
    }

    @Test
    public void reversed() {
        final PortfolioShare p = new PortfolioShare(Rating.D, 51, 50);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(p.getMininumShareInPercent()).isEqualTo(50);
            softly.assertThat(p.getMaximumShareInPercent()).isEqualTo(51);
            softly.assertThat(p.getRating()).isEqualTo(Rating.D);
        });
    }
}
