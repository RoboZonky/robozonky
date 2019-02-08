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

package com.github.robozonky.internal.util;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class BigDecimalCalculatorTest {

    @Test
    void multiplication() {
        final BigDecimal result = BigDecimalCalculator.times(2, 3);
        assertThat(result).isEqualTo(new BigDecimal("6"));
    }

    @Test
    void division() {
        final BigDecimal result = BigDecimalCalculator.divide(3, 2);
        assertThat(result).isEqualTo(new BigDecimal("1.5"));
    }

    @Test
    void addition() {
        final BigDecimal result = BigDecimalCalculator.plus(0, 1.0);
        assertThat(result).isEqualTo(BigDecimal.ONE);
    }

    @Test
    void subtractionWithBigDecimalAsSecondArg() {
        final BigDecimal result = BigDecimalCalculator.minus(0.0, BigDecimal.ONE);
        assertThat(result).isEqualTo(BigDecimal.ONE.negate());
    }

    @Test
    void multiplicationWithBigDecimalAsSecondArg() {
        final BigDecimal result = BigDecimalCalculator.times(2, BigDecimal.valueOf(3));
        assertThat(result).isEqualTo(new BigDecimal("6"));
    }

    @Test
    void divisionWithBigDecimalAsSecondArg() {
        final BigDecimal result = BigDecimalCalculator.divide(3, BigDecimal.valueOf(2));
        assertThat(result).isEqualTo(new BigDecimal("1.5"));
    }

    @Test
    void additionWithBigDecimalAsSecondArg() {
        final BigDecimal result = BigDecimalCalculator.plus(0, BigDecimal.ONE);
        assertThat(result).isEqualTo(BigDecimal.ONE);
    }

    @Test
    void subtraction() {
        final BigDecimal result = BigDecimalCalculator.minus(0.0, 1);
        assertThat(result).isEqualTo(BigDecimal.ONE.negate());
    }

    @Test
    void scaling() {
        final BigDecimal result = BigDecimalCalculator.toScale(BigDecimal.ZERO);
        assertThat(result.scale()).isEqualTo(8);
    }

    @Test
    void moreThan() {
        final BigDecimal result = BigDecimalCalculator.moreThan(BigDecimal.ONE);
        assertThat(result).isEqualTo(new BigDecimal("1.00000001"));
    }

    @Test
    void lessThan() {
        final BigDecimal result = BigDecimalCalculator.lessThan(BigDecimal.ONE);
        assertThat(result).isEqualTo(new BigDecimal("0.99999999"));
    }
}
