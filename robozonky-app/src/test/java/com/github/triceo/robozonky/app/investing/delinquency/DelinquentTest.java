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

package com.github.triceo.robozonky.app.investing.delinquency;

import java.time.OffsetDateTime;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;

public class DelinquentTest {

    @Test
    public void equals() {
        final int id = 1;
        final OffsetDateTime now = OffsetDateTime.now();
        final Delinquent d1 = new Delinquent(id, now);
        Assertions.assertThat(d1).isEqualTo(d1);
        final Delinquent d2 = new Delinquent(id, now);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(d1).isEqualTo(d2);
            softly.assertThat(d2).isEqualTo(d1);
        });
        final Delinquent d3 = new Delinquent(id + 1, now);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(d1).isNotEqualTo(d3);
            softly.assertThat(d3).isNotEqualTo(d1);
        });
        final Delinquent d4 = new Delinquent(id, now.minusDays(1));
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(d1).isNotEqualTo(d4);
            softly.assertThat(d4).isNotEqualTo(d1);
        });
    }
}
