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

package com.github.robozonky.api;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

class RatioTest {

    @Test
    void numericallyCorrect() {
        final Ratio ratio = Ratio.fromRaw("2.64"); // not properly representable as a double
        assertSoftly(softly -> {
            softly.assertThat(ratio.asPercentage().intValue()).isEqualTo(264);
            softly.assertThat(ratio.doubleValue()).isEqualTo(2.64, Offset.offset(0.000_000_000_1));
            softly.assertThat(ratio.floatValue()).isEqualTo(2.64f, Offset.offset(0.000_000_000_1f));
            softly.assertThat(ratio.intValue()).isEqualTo(2);
            softly.assertThat(ratio.longValue()).isEqualTo(2);
        });
    }

    @Test
    void minMax() {
        final Ratio ratio = Ratio.fromRaw("2.64");
        final Ratio ratio2 = Ratio.fromRaw("2.65");
        assertSoftly(softly -> {
            softly.assertThat(ratio.min(ratio2)).isSameAs(ratio);
            softly.assertThat(ratio2.min(ratio)).isSameAs(ratio);
            softly.assertThat(ratio.max(ratio2)).isSameAs(ratio2);
            softly.assertThat(ratio2.max(ratio)).isSameAs(ratio2);
        });
        final Ratio ratio3 = Ratio.fromRaw("2.64"); // compare two of the same
        assertSoftly(softly -> {
            softly.assertThat(ratio.min(ratio3)).isSameAs(ratio);
            softly.assertThat(ratio.max(ratio3)).isSameAs(ratio);
            softly.assertThat(ratio3.min(ratio)).isSameAs(ratio3);
            softly.assertThat(ratio3.max(ratio)).isSameAs(ratio3);
        });
    }

    @Test
    void equals() {
        final Ratio ratio = Ratio.fromRaw("2.64");
        final Ratio ratio2 = Ratio.fromRaw("2.65");
        final Ratio ratio3 = Ratio.fromRaw("2.64"); // compare two of the same
        assertSoftly(softly -> {
            softly.assertThat(ratio).isNotEqualTo(null);
            softly.assertThat(ratio).isNotEqualTo(UUID.randomUUID().toString());
            softly.assertThat(ratio).isEqualTo(ratio);
            softly.assertThat(ratio).isNotEqualTo(ratio2);
            softly.assertThat(ratio).isEqualTo(ratio3);
        });
    }

}
