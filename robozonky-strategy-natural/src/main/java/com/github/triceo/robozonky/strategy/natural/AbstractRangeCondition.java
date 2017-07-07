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

package com.github.triceo.robozonky.strategy.natural;

import java.math.BigDecimal;
import java.util.function.Function;

import com.github.triceo.robozonky.api.remote.entities.Loan;

abstract class AbstractRangeCondition extends MarketplaceFilterCondition {

    private static BigDecimal toBigDecimal(final Number num) {
        return BigDecimal.valueOf(num.doubleValue());
    }

    private final Function<Loan, Number> targetAccessor;
    private final BigDecimal minInclusive, maxInclusive;

    protected AbstractRangeCondition(final Function<Loan, Number> targetAccessor, final Number minValueInclusive,
                                     final Number maxValueInclusive) {
        this.targetAccessor = targetAccessor;
        final BigDecimal min = AbstractRangeCondition.toBigDecimal(minValueInclusive);
        final BigDecimal max = AbstractRangeCondition.toBigDecimal(maxValueInclusive);
        this.minInclusive = min.min(max);
        this.maxInclusive = min.max(max);
        if (this.maxInclusive.compareTo(this.minInclusive) < 0) {
            throw new IllegalArgumentException("Minimum must be smaller than maximum.");
        }
    }

    public boolean test(final Loan loan) {
        final BigDecimal target = AbstractRangeCondition.toBigDecimal(targetAccessor.apply(loan));
        return target.compareTo(minInclusive) >= 0 && target.compareTo(maxInclusive) <= 0;
    }

}
