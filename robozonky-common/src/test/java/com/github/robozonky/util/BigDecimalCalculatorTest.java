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

package com.github.robozonky.util;

import java.math.BigDecimal;

import org.assertj.core.api.Assertions;
import org.junit.Test;

public class BigDecimalCalculatorTest {

    @Test
    public void multiplication() {
        Assertions.assertThat(BigDecimalCalculator.times(2, 3)).isEqualTo(new BigDecimal("6"));
    }

    @Test
    public void division() {
        Assertions.assertThat(BigDecimalCalculator.divide(3, 2)).isEqualTo(new BigDecimal("1.5"));
    }

    @Test
    public void addition() {
        Assertions.assertThat(BigDecimalCalculator.plus(0, 1.0)).isEqualTo(BigDecimal.ONE);
    }

    @Test
    public void subtraction() {
        Assertions.assertThat(BigDecimalCalculator.minus(0.0, 1)).isEqualTo(BigDecimal.ONE.negate());
    }

    @Test
    public void scaling() {
        final BigDecimal result = BigDecimalCalculator.toScale(BigDecimal.ZERO);
        Assertions.assertThat(result.scale()).isEqualTo(BigDecimalCalculator.DEFAULT_SCALE);
    }
}
