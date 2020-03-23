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

package com.github.robozonky.internal.util.functional;

import static org.assertj.core.api.Assertions.*;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

class Tuple3Test {

    @Test
    void getters() {
        Tuple3 tuple = Tuple.of(1, 2, 3);
        assertThat(tuple._1()).isEqualTo(1);
        assertThat(tuple._2()).isEqualTo(2);
        assertThat(tuple._3()).isEqualTo(3);
    }

    @Test
    void equals() {
        Tuple3 tuple = Tuple.of(1, 1, 1);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(tuple)
                .isEqualTo(tuple);
            softly.assertThat(tuple)
                .isNotEqualTo(null);
            softly.assertThat(tuple)
                .isNotEqualTo("");
        });
        Tuple3 tuple2 = Tuple.of(1, 1, 1);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(tuple)
                .isNotSameAs(tuple2);
            softly.assertThat(tuple)
                .isEqualTo(tuple2);
        });
        Tuple3 tuple3 = Tuple.of(1, 2, 3);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(tuple3)
                .isNotEqualTo(tuple);
            softly.assertThat(tuple)
                .isNotEqualTo(tuple3);
        });
    }

}
