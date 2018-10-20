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
import java.util.Optional;
import java.util.function.Function;

import com.github.robozonky.strategy.natural.Wrapper;

abstract class AbstractRelativeRangeCondition extends MarketplaceFilterConditionImpl
        implements MarketplaceFilterCondition {

    private final Function<Wrapper<?>, Number> sumAccessor, partAccessor;
    private final Number minInclusive, maxInclusive;

    protected AbstractRelativeRangeCondition(final Function<Wrapper<?>, Number> sumAccessor,
                                             final Function<Wrapper<?>, Number> partAccessor,
                                             final Number minValueInclusive, final Number maxValueInclusive) {
        assertIsInRange(minValueInclusive);
        assertIsInRange(maxValueInclusive);
        this.sumAccessor = sumAccessor;
        this.partAccessor = partAccessor;
        this.minInclusive = minValueInclusive;
        this.maxInclusive = maxValueInclusive;
    }

    private static BigDecimal toBigDecimal(final Number num) {
        return new BigDecimal(num.toString());
    }

    private static BigDecimal getActualValue(final Number sum, final Number percentage) {
        final BigDecimal s = toBigDecimal(sum);
        final BigDecimal p = toBigDecimal(percentage);
        return s.multiply(p).scaleByPowerOfTen(-2);
    }

    private static void assertIsInRange(final Number percentage) {
        final double num = toBigDecimal(percentage).doubleValue();
        if (num < 0 || num > 100) {
            throw new IllegalArgumentException("Relative value must be in range of <0; 100> %, but was " + percentage);
        }
    }

    @Override
    public Optional<String> getDescription() {
        return Optional.of("Relative range: <" + minInclusive + "; " + maxInclusive + "> %.");
    }

    @Override
    public boolean test(final Wrapper<?> item) {
        final BigDecimal realMinInclusive = getActualValue(sumAccessor.apply(item), minInclusive);
        final BigDecimal realMaxInclusive = getActualValue(sumAccessor.apply(item), maxInclusive);
        return new RangeCondition<>(partAccessor, realMinInclusive, realMaxInclusive).test(item);
    }
}
