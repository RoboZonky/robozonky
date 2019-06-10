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

package com.github.robozonky.strategy.natural.conditions;

import java.math.BigDecimal;
import java.util.function.Function;
import java.util.function.Predicate;

import com.github.robozonky.api.Ratio;
import com.github.robozonky.strategy.natural.Wrapper;

import static com.github.robozonky.internal.util.BigDecimalCalculator.divide;

final class RangeCondition<T extends Number & Comparable<T>> implements Predicate<Wrapper<?>> {

    private final Function<Wrapper<?>, T> accessor;
    private final Predicate<T> acceptor;
    private final String toString;

    private RangeCondition(final Function<Wrapper<?>, T> accessor, final Predicate<T> acceptor,
                           final String toString) {
        this.accessor = accessor;
        this.acceptor = acceptor;
        this.toString = toString;
    }

    static <X extends Number & Comparable<X>> RangeCondition<X> lessThan(final Function<Wrapper<?>, X> value,
                                                                         final Domain<X> allowedValues,
                                                                         final X threshold) {
        if (!allowedValues.test(threshold)) {
            throw new IllegalArgumentException("Threshold " + threshold + " does not fit " + allowedValues);
        }
        return new RangeCondition<>(value, v -> v.compareTo(threshold) < 0, "Range: (-inf.; " + threshold + ")");
    }

    static <X extends Number & Comparable<X>> RangeCondition<X> moreThan(final Function<Wrapper<?>, X> value,
                                                                         final Domain<X> allowedValues,
                                                                         final X threshold) {
        if (!allowedValues.test(threshold)) {
            throw new IllegalArgumentException("Threshold " + threshold + " does not fit " + allowedValues);
        }
        return new RangeCondition<>(value, v -> v.compareTo(threshold) > 0, "Range: (" + threshold + "; +inf.)");
    }

    private static <X extends Number & Comparable<X>> Ratio getActualValue(final X part, final X sum) {
        final double p = part.doubleValue();
        final double s = sum.doubleValue();
        final BigDecimal result = divide(BigDecimal.valueOf(p), BigDecimal.valueOf(s));
        return Ratio.fromRaw(result);
    }

    private static <X extends Number & Comparable<X>> Ratio getValueAccessor(final Wrapper<?> w,
                                                                             final Function<Wrapper<?>, X> part,
                                                                             final Function<Wrapper<?>, X> sum) {
        System.out.println(part.apply(w) + " " + sum.apply(w));
        System.out.println(getActualValue(part.apply(w), sum.apply(w)));
        return getActualValue(part.apply(w), sum.apply(w));
    }

    static <X extends Number & Comparable<X>> RangeCondition<X> exact(final Function<Wrapper<?>, X> value,
                                                                      final Domain<X> allowedValues, final X minimum,
                                                                      final X maximum) {
        if (!allowedValues.test(minimum)) {
            throw new IllegalArgumentException("Minimum " + minimum + " does not fit " + allowedValues);
        } else if (!allowedValues.test(maximum)) {
            throw new IllegalArgumentException("Maximum " + maximum + " does not fit " + allowedValues);
        } else if (minimum.compareTo(maximum) > 0) {
            throw new IllegalArgumentException("Minimum " + minimum + " is over maximum " + maximum);
        }
        return new RangeCondition<>(value, v -> v.compareTo(minimum) >= 0 && v.compareTo(maximum) <= 0,
                                    "Range: <" + minimum + "; " + maximum + ">");
    }

    static <X extends Number & Comparable<X>> RangeCondition<Ratio> relativeLessThan(final Function<Wrapper<?>, X> part,
                                                                                     final Function<Wrapper<?>, X> sum,
                                                                                     final Ratio threshold) {
        final Domain<Ratio> allowedValues = AbstractRelativeRangeCondition.RELATIVE_DOMAIN;
        if (!allowedValues.test(threshold)) {
            throw new IllegalArgumentException("Threshold " + threshold + " does not fit " + allowedValues);
        }
        final Function<Wrapper<?>, Ratio> value = w -> getValueAccessor(w, part, sum);
        return new RangeCondition<>(value, v -> v.compareTo(threshold) < 0,
                                    "Relative range: (-inf.; " + threshold + ")");
    }

    static <X extends Number & Comparable<X>> RangeCondition<Ratio> relativeMoreThan(final Function<Wrapper<?>, X> part,
                                                                                     final Function<Wrapper<?>, X> sum,
                                                                                     final Ratio threshold) {
        final Domain<Ratio> allowedValues = AbstractRelativeRangeCondition.RELATIVE_DOMAIN;
        if (!allowedValues.test(threshold)) {
            throw new IllegalArgumentException("Threshold " + threshold + " does not fit " + allowedValues);
        }
        final Function<Wrapper<?>, Ratio> value = w -> getValueAccessor(w, part, sum);
        return new RangeCondition<>(value, v -> v.compareTo(threshold) > 0,
                                    "Relative range: (" + threshold + "; +inf.)");
    }

    static <X extends Number & Comparable<X>> RangeCondition<Ratio> relativeExact(final Function<Wrapper<?>, X> part,
                                                                                  final Function<Wrapper<?>, X> sum,
                                                                                  final Ratio minimum,
                                                                                  final Ratio maximum) {
        final Domain<Ratio> allowedValues = AbstractRelativeRangeCondition.RELATIVE_DOMAIN;
        if (!allowedValues.test(minimum)) {
            throw new IllegalArgumentException("Minimum " + minimum + " does not fit " + allowedValues);
        } else if (!allowedValues.test(maximum)) {
            throw new IllegalArgumentException("Maximum " + maximum + " does not fit " + allowedValues);
        } else if (minimum.compareTo(maximum) > 0) {
            throw new IllegalArgumentException("Minimum " + minimum + " is over maximum " + maximum);
        }
        final Function<Wrapper<?>, Ratio> value = w -> getValueAccessor(w, part, sum);
        return new RangeCondition<>(value, v -> v.compareTo(minimum) >= 0 && v.compareTo(maximum) <= 0,
                                    "Relative range: <" + minimum + "; " + maximum + ">");
    }

    @Override
    public boolean test(final Wrapper<?> wrapper) {
        return acceptor.test(accessor.apply(wrapper));
    }

    @Override
    public String toString() {
        return toString;
    }
}
