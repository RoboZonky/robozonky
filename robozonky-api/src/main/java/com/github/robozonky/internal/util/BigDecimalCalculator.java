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
import java.math.RoundingMode;

public final class BigDecimalCalculator {

    static final int DEFAULT_SCALE = 8;
    private static final BigDecimal MINIMAL_INCREMENT = BigDecimal.ONE.divide(BigDecimal.TEN.pow(DEFAULT_SCALE));

    private BigDecimalCalculator() {
        // no instances
    }

    public static BigDecimal toScale(final BigDecimal number, final int scale) {
        return number.setScale(scale, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal toScale(final BigDecimal number) {
        return toScale(number, DEFAULT_SCALE);
    }

    public static BigDecimal divide(final Number numerator, final Number denominator) {
        return divide(toBigDecimal(numerator), denominator);
    }

    public static BigDecimal divide(final BigDecimal numerator, final Number denominator) {
        return divide(numerator, toBigDecimal(denominator));
    }

    public static BigDecimal divide(final Number numerator, final BigDecimal denominator) {
        return divide(toBigDecimal(numerator), denominator);
    }

    public static BigDecimal divide(final BigDecimal numerator, final BigDecimal denominator) {
        return finish(numerator.divide(denominator, DEFAULT_SCALE, RoundingMode.HALF_EVEN));
    }

    public static BigDecimal plus(final Number addend1, final Number addend2) {
        return plus(toBigDecimal(addend1), addend2);
    }

    public static BigDecimal plus(final BigDecimal addend1, final Number addend2) {
        return plus(addend1, toBigDecimal(addend2));
    }

    public static BigDecimal plus(final Number addend1, final BigDecimal addend2) {
        return plus(toBigDecimal(addend1), addend2);
    }

    public static BigDecimal plus(final BigDecimal addend1, final BigDecimal addend2) {
        return finish(addend1.add(addend2));
    }

    public static BigDecimal minus(final Number minuend, final Number subtrahend) {
        return minus(toBigDecimal(minuend), subtrahend);
    }

    public static BigDecimal minus(final BigDecimal minuend, final Number subtrahend) {
        return minus(minuend, toBigDecimal(subtrahend));
    }

    public static BigDecimal minus(final Number minuend, final BigDecimal subtrahend) {
        return minus(toBigDecimal(minuend), subtrahend);
    }

    private static BigDecimal toBigDecimal(final Number number) {
        return new BigDecimal(number.toString());
    }

    public static BigDecimal minus(final BigDecimal minuend, final BigDecimal subtrahend) {
        return finish(minuend.subtract(subtrahend));
    }

    public static BigDecimal times(final Number multiplicand, final Number multiplier) {
        return times(toBigDecimal(multiplicand), multiplier);
    }

    public static BigDecimal times(final BigDecimal multiplicand, final Number multiplier) {
        return times(multiplicand, toBigDecimal(multiplier));
    }

    public static BigDecimal times(final Number multiplicand, final BigDecimal multiplier) {
        return times(toBigDecimal(multiplicand), multiplier);
    }

    public static BigDecimal times(final BigDecimal multiplicand, final BigDecimal multiplier) {
        return finish(multiplicand.multiply(multiplier));
    }

    private static BigDecimal finish(final BigDecimal number) {
        return toScale(number).stripTrailingZeros();
    }

    public static BigDecimal lessThan(final BigDecimal number) {
        return minus(number, MINIMAL_INCREMENT);
    }

    public static BigDecimal moreThan(final BigDecimal number) {
        return plus(number, MINIMAL_INCREMENT);
    }
}
