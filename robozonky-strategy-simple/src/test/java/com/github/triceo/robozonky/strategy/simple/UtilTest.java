/*
 * Copyright 2016 Lukáš Petrovický
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

package com.github.triceo.robozonky.strategy.simple;

import java.math.BigDecimal;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;

public class UtilTest {

    @Test
    public void testRanges() {
        final SoftAssertions softly = new SoftAssertions();
        Assertions.assertThat(Util.isBetweenZeroAndOne(BigDecimal.TEN)).isFalse();
        Assertions.assertThat(Util.isBetweenZeroAndOne(BigDecimal.ONE)).isTrue();
        Assertions.assertThat(Util.isBetweenZeroAndOne(BigDecimal.valueOf(0.5))).isTrue();
        Assertions.assertThat(Util.isBetweenZeroAndOne(BigDecimal.ZERO)).isTrue();
        Assertions.assertThat(Util.isBetweenZeroAndOne(BigDecimal.valueOf(-1))).isFalse();
        softly.assertAll();
    }

}
