/*
 * Copyright 2018 The RoboZonky Project
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

class RangeCondition<T> implements Predicate<T> {

    private final Function<T, Number> targetAccessor;
    private final BigDecimal minInclusive, maxInclusive;

    private static BigDecimal toBigDecimal(final Number num) {
        return new BigDecimal(num.toString());
    }

    public RangeCondition(final Function<T, Number> targetAccessor, final Number minValueInclusive,
                          final Number maxValueInclusive) {
        this.targetAccessor = targetAccessor;
        final BigDecimal min = toBigDecimal(minValueInclusive);
        final BigDecimal max = toBigDecimal(maxValueInclusive);
        this.minInclusive = min.min(max);
        this.maxInclusive = min.max(max);
    }

    public BigDecimal getMinInclusive() {
        return minInclusive;
    }

    public BigDecimal getMaxInclusive() {
        return maxInclusive;
    }

    @Override
    public boolean test(final T item) {
        final BigDecimal target = toBigDecimal(targetAccessor.apply(item));
        return target.compareTo(minInclusive) >= 0 && target.compareTo(maxInclusive) <= 0;
    }
}
