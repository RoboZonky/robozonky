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

import java.math.BigDecimal;
import java.util.Objects;

import com.github.robozonky.internal.util.BigDecimalCalculator;

public final class Ratio extends Number implements Comparable<Ratio> {

    public static final Ratio ZERO = Ratio.fromRaw(0);
    public static final Ratio ONE = Ratio.fromRaw(1);
    private final BigDecimal raw;
    private final BigDecimal percentage;

    private Ratio(final BigDecimal raw) {
        this.raw = raw.stripTrailingZeros();
        this.percentage = BigDecimalCalculator.times(raw, BigDecimal.TEN.pow(2));
    }

    public static Ratio fromRaw(final Number rate) {
        return new Ratio(new BigDecimal(rate.toString()));
    }

    public static Ratio fromRaw(final String rate) {
        return new Ratio(new BigDecimal(rate));
    }

    public static Ratio fromPercentage(final String rate) {
        final BigDecimal original = new BigDecimal(rate);
        final BigDecimal raw = original.divide(BigDecimal.TEN.pow(2));
        return new Ratio(raw);
    }

    public static Ratio fromPercentage(final Number rate) {
        return fromPercentage(rate.toString());
    }

    public BigDecimal bigDecimalValue() {
        return raw;
    }

    public BigDecimal asPercentage() {
        return percentage;
    }

    public Ratio min(final Ratio other) {
        return this.compareTo(other) > 0 ? other : this;
    }

    public Ratio max(final Ratio other) {
        return this.compareTo(other) < 0 ? other : this;
    }

    @Override
    public String toString() {
        return raw.toPlainString();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !Objects.equals(getClass(), o.getClass())) {
            return false;
        }
        final Ratio ratio = (Ratio) o;
        return Objects.equals(raw, ratio.raw);
    }

    @Override
    public int hashCode() {
        return Objects.hash(raw);
    }

    @Override
    public int compareTo(final Ratio o) {
        return this.raw.compareTo(o.raw);
    }

    @Override
    public int intValue() {
        return raw.intValue();
    }

    @Override
    public long longValue() {
        return raw.longValue();
    }

    @Override
    public float floatValue() {
        return raw.floatValue();
    }

    @Override
    public double doubleValue() {
        return raw.doubleValue();
    }
}
