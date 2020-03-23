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

package com.github.robozonky.strategy.natural.conditions;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

import org.junit.jupiter.api.Test;

class DomainTest {

    @Test
    void rangeLeftOpen() {
        final Domain<Integer> d = new Domain<>(Integer.class, null, 1);
        assertSoftly(softly -> {
            softly.assertThat(d)
                .rejects(2);
            softly.assertThat(d)
                .accepts(1);
            softly.assertThat(d)
                .accepts(Integer.MIN_VALUE);
            softly.assertThat(d.getMinimum())
                .isEmpty();
            softly.assertThat(d.getMaximum())
                .contains(1);
        });
    }

    @Test
    void rangeRightOpen() {
        final Domain<Integer> d = new Domain<>(Integer.class, 1, null);
        assertSoftly(softly -> {
            softly.assertThat(d)
                .rejects(0);
            softly.assertThat(d)
                .accepts(1);
            softly.assertThat(d)
                .accepts(Integer.MAX_VALUE);
            softly.assertThat(d.getMaximum())
                .isEmpty();
            softly.assertThat(d.getMinimum())
                .contains(1);
        });
    }

    @Test
    void rangeExact() {
        final Domain<Integer> d = new Domain<>(Integer.class, 0, 2);
        assertSoftly(softly -> {
            softly.assertThat(d)
                .rejects(Integer.MIN_VALUE);
            softly.assertThat(d)
                .rejects(-1);
            softly.assertThat(d)
                .accepts(0);
            softly.assertThat(d)
                .accepts(1);
            softly.assertThat(d)
                .accepts(2);
            softly.assertThat(d)
                .rejects(3);
            softly.assertThat(d)
                .rejects(Integer.MAX_VALUE);
            softly.assertThat(d.getMinimum())
                .contains(0);
            softly.assertThat(d.getMaximum())
                .contains(2);
        });
    }

}
